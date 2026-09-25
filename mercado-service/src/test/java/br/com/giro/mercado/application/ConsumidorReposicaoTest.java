package br.com.giro.mercado.application;

import br.com.giro.mercado.IntegracaoTest;
import br.com.giro.mercado.application.contrato.MotivoNegacao;
import br.com.giro.mercado.application.contrato.ReposicaoEnviada;
import br.com.giro.mercado.application.contrato.ReposicaoNegada;
import br.com.giro.mercado.domain.EstoqueInsuficienteException;
import br.com.giro.mercado.domain.ProdutoVitrine;
import br.com.giro.mercado.domain.StatusSolicitacao;
import br.com.giro.mercado.domain.StatusVitrine;
import br.com.giro.mercado.infra.persistencia.ProdutoVitrineRepository;
import br.com.giro.mercado.infra.persistencia.SolicitacaoReposicaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsumidorReposicaoTest extends IntegracaoTest {

    private static final String COCA = "7894900011517";

    @Autowired
    ConsumidorReposicao consumidor;

    @Autowired
    CheckoutService checkout;

    @Autowired
    ProdutoVitrineRepository produtos;

    @Autowired
    SolicitacaoReposicaoRepository solicitacoes;

    /** Cadastra e vende até disparar o gatilho; devolve o eventId da solicitação criada. */
    private UUID vendeAteSolicitarReposicao(int prateleira, int minimo, int ideal, int venda) {
        produtos.save(new ProdutoVitrine(COCA, "COCA COLA 2L PET", new BigDecimal("9.99"), prateleira, minimo, ideal));
        checkout.fechar(List.of(new ItemCheckout(COCA, venda)));
        return solicitacoes.findAll().getFirst().getEventId();
    }

    private ReposicaoEnviada enviada(UUID correlationId, int qtd) {
        return new ReposicaoEnviada(UUID.randomUUID(), correlationId, COCA, qtd, "L2026-09", Instant.now());
    }

    private ReposicaoNegada negada(UUID correlationId, MotivoNegacao motivo) {
        return new ReposicaoNegada(UUID.randomUUID(), correlationId, COCA, motivo, Instant.now());
    }

    private ProdutoVitrine produto() {
        return produtos.findById(COCA).orElseThrow();
    }

    private StatusSolicitacao statusDa(UUID eventId) {
        return solicitacoes.findByEventId(eventId).orElseThrow().getStatus();
    }

    @Test
    void reposicaoEnviadaCreditaPrateleiraEFechaSolicitacao() {
        var solicitacao = vendeAteSolicitarReposicao(10, 5, 20, 6);   // prateleira 4, pede 16

        consumidor.processar(enviada(solicitacao, 16));

        assertThat(produto().getEstoquePrateleira()).isEqualTo(20);
        assertThat(produto().getStatus()).isEqualTo(StatusVitrine.DISPONIVEL);
        assertThat(statusDa(solicitacao)).isEqualTo(StatusSolicitacao.ATENDIDA);
        assertThat(reposicoesSolicitadas(COCA)).hasSize(1);
    }

    @Test
    void eventoDuplicadoCreditaUmaVezSo() {
        var solicitacao = vendeAteSolicitarReposicao(10, 5, 20, 6);
        var evento = enviada(solicitacao, 16);

        consumidor.processar(evento);
        consumidor.processar(evento);

        assertThat(produto().getEstoquePrateleira()).isEqualTo(20);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM inbox_event", Integer.class)).isOne();
    }

    @Test
    void estoqueZeradoEsgotaEAguardaLoteSemPedirDeNovo() {
        var solicitacao = vendeAteSolicitarReposicao(3, 5, 20, 3);   // prateleira 0

        consumidor.processar(negada(solicitacao, MotivoNegacao.SEM_SALDO));

        assertThat(produto().getStatus()).isEqualTo(StatusVitrine.ESGOTADO);
        assertThat(statusDa(solicitacao)).isEqualTo(StatusSolicitacao.AGUARDANDO_LOTE);
        assertThatThrownBy(() -> checkout.fechar(List.of(new ItemCheckout(COCA, 1))))
                .isInstanceOf(EstoqueInsuficienteException.class);
        assertThat(reposicoesSolicitadas(COCA)).hasSize(1);
    }

    @Test
    void loteNovoAtendeDemandaReprimidaEReabreOProduto() {
        var solicitacao = vendeAteSolicitarReposicao(3, 5, 20, 3);
        consumidor.processar(negada(solicitacao, MotivoNegacao.SEM_SALDO));

        // Entrada de lote no estoque: ele responde à solicitação original, agora com saldo.
        consumidor.processar(enviada(solicitacao, 20));

        assertThat(produto().getStatus()).isEqualTo(StatusVitrine.DISPONIVEL);
        assertThat(produto().getEstoquePrateleira()).isEqualTo(20);
        assertThat(statusDa(solicitacao)).isEqualTo(StatusSolicitacao.ATENDIDA);
    }

    @Test
    void negativaComUnidadesNaPrateleiraMantemAVendaSemRepetirPedido() {
        var solicitacao = vendeAteSolicitarReposicao(10, 5, 20, 7);   // prateleira 3

        consumidor.processar(negada(solicitacao, MotivoNegacao.SEM_SALDO));
        checkout.fechar(List.of(new ItemCheckout(COCA, 1)));

        assertThat(produto().getStatus()).isEqualTo(StatusVitrine.DISPONIVEL);
        assertThat(produto().getEstoquePrateleira()).isEqualTo(2);
        assertThat(reposicoesSolicitadas(COCA)).hasSize(1);
    }

    @Test
    void skuDesconhecidoNoEstoqueCancelaASolicitacao() {
        var solicitacao = vendeAteSolicitarReposicao(10, 5, 20, 6);

        consumidor.processar(negada(solicitacao, MotivoNegacao.SKU_DESCONHECIDO));

        assertThat(statusDa(solicitacao)).isEqualTo(StatusSolicitacao.CANCELADA);
    }

    @Test
    void loteMenorQueOPedidoDisparaNovaReposicao() {
        var solicitacao = vendeAteSolicitarReposicao(10, 5, 20, 8);   // prateleira 2, pede 18

        consumidor.processar(enviada(solicitacao, 2));   // chegam só 2: prateleira 4, ainda < 5

        assertThat(statusDa(solicitacao)).isEqualTo(StatusSolicitacao.ATENDIDA);
        assertThat(reposicoesSolicitadas(COCA)).containsExactly(18, 16);
    }
}
