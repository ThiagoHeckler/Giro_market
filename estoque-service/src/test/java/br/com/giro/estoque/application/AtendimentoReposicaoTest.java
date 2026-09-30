package br.com.giro.estoque.application;

import br.com.giro.estoque.IntegracaoTest;
import br.com.giro.estoque.application.contrato.MotivoNegacao;
import br.com.giro.estoque.application.contrato.ReposicaoEnviada;
import br.com.giro.estoque.application.contrato.ReposicaoNegada;
import br.com.giro.estoque.application.contrato.ReposicaoSolicitada;
import br.com.giro.estoque.domain.ProdutoEstoque;
import br.com.giro.estoque.infra.persistencia.DemandaReprimidaRepository;
import br.com.giro.estoque.infra.persistencia.LoteRepository;
import br.com.giro.estoque.infra.persistencia.ProdutoEstoqueRepository;
import br.com.giro.estoque.infra.persistencia.ReposicaoExpedidaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

class AtendimentoReposicaoTest extends IntegracaoTest {

    private static final String COCA = "7894900011517";

    @Autowired
    AtendimentoReposicao atendimento;

    @Autowired
    ProdutoEstoqueRepository produtos;

    @Autowired
    LoteRepository lotes;

    @Autowired
    DemandaReprimidaRepository demandas;

    @Autowired
    ReposicaoExpedidaRepository expedidas;

    @Autowired
    TransactionTemplate transacao;

    @Autowired
    Clock relogio;

    private record NovoLote(String codigo, int qtd, LocalDate validade) {
    }

    private void cadastra(NovoLote... novos) {
        transacao.executeWithoutResult(_ -> {
            var produto = produtos.save(new ProdutoEstoque(COCA, "COCA COLA 2L PET", "22021000"));
            for (var novo : novos) {
                lotes.save(produto.receberLote(novo.codigo(), novo.qtd(), novo.validade(), Instant.now()));
            }
        });
    }

    private LocalDate hojeMais(int dias) {
        return LocalDate.now(relogio).plusDays(dias);
    }

    private ReposicaoSolicitada pedido(int qtd) {
        return new ReposicaoSolicitada(UUID.randomUUID(), COCA, qtd, Instant.now());
    }

    private int saldo() {
        return produtos.findById(COCA).orElseThrow().getSaldoDisponivel();
    }

    private int disponivelNoLote(String codigo) {
        return jdbc.queryForObject("SELECT quantidade_disponivel FROM lote WHERE codigo_lote = ?", Integer.class, codigo);
    }

    @Test
    void comSaldoExpedeOPedidoEBaixaLoteEProduto() {
        cadastra(new NovoLote("L1", 50, hojeMais(30)));
        var pedido = pedido(16);

        atendimento.processar(pedido);

        assertThat(respostasNaOutbox()).singleElement()
                .asInstanceOf(type(ReposicaoEnviada.class))
                .satisfies(e -> {
                    assertThat(e.correlationId()).isEqualTo(pedido.eventId());
                    assertThat(e.qtd()).isEqualTo(16);
                    assertThat(e.lote()).isEqualTo("L1");
                });
        assertThat(saldo()).isEqualTo(34);
        assertThat(disponivelNoLote("L1")).isEqualTo(34);
    }

    @Test
    void envioFicaRegistradoParaRecallDoLote() {
        cadastra(new NovoLote("L1", 50, hojeMais(30)));
        var pedido = pedido(16);

        atendimento.processar(pedido);

        var enviada = (ReposicaoEnviada) respostasNaOutbox().getFirst();
        var loteId = jdbc.queryForObject("SELECT id FROM lote WHERE codigo_lote = 'L1'", Long.class);
        assertThat(expedidas.findByLoteIdOrderByExpedidoEmAsc(loteId)).singleElement().satisfies(r -> {
            assertThat(r.getEventId()).isEqualTo(enviada.eventId());
            assertThat(r.getCorrelationId()).isEqualTo(pedido.eventId());
            assertThat(r.getSku()).isEqualTo(COCA);
            assertThat(r.getQtd()).isEqualTo(16);
        });
    }

    @Test
    void negacaoNaoRegistraEnvio() {
        cadastra();

        atendimento.processar(pedido(5));

        assertThat(expedidas.count()).isZero();
    }

    @Test
    void expedeFefoVencePrimeiroSaiPrimeiro() {
        cadastra(new NovoLote("VENCE-DEPOIS", 50, hojeMais(60)),
                new NovoLote("SEM-VALIDADE", 50, null),
                new NovoLote("VENCE-ANTES", 50, hojeMais(10)));

        atendimento.processar(pedido(5));

        assertThat(respostasNaOutbox()).singleElement()
                .asInstanceOf(type(ReposicaoEnviada.class))
                .extracting(ReposicaoEnviada::lote).isEqualTo("VENCE-ANTES");
    }

    @Test
    void loteVencidoNaoEhExpedido() {
        cadastra(new NovoLote("VENCIDO", 50, hojeMais(-1)));

        atendimento.processar(pedido(5));

        assertThat(respostasNaOutbox()).singleElement()
                .asInstanceOf(type(ReposicaoNegada.class))
                .extracting(ReposicaoNegada::motivo).isEqualTo(MotivoNegacao.SEM_SALDO);
        assertThat(disponivelNoLote("VENCIDO")).isEqualTo(50);
    }

    @Test
    void pedidoMaiorQueOLoteEnviaSoOQueOLoteTem() {
        cadastra(new NovoLote("L1", 6, hojeMais(10)), new NovoLote("L2", 50, hojeMais(20)));

        atendimento.processar(pedido(16));

        assertThat(respostasNaOutbox()).singleElement()
                .asInstanceOf(type(ReposicaoEnviada.class))
                .satisfies(e -> {
                    assertThat(e.lote()).isEqualTo("L1");
                    assertThat(e.qtd()).isEqualTo(6);
                });
        assertThat(saldo()).isEqualTo(50);
    }

    @Test
    void estoqueZeradoNegaERegistraDemandaReprimida() {
        cadastra();
        var pedido = pedido(16);

        atendimento.processar(pedido);

        assertThat(respostasNaOutbox()).singleElement()
                .asInstanceOf(type(ReposicaoNegada.class))
                .satisfies(n -> {
                    assertThat(n.motivo()).isEqualTo(MotivoNegacao.SEM_SALDO);
                    assertThat(n.correlationId()).isEqualTo(pedido.eventId());
                });
        assertThat(demandas.findAll()).singleElement().satisfies(d -> {
            assertThat(d.getSku()).isEqualTo(COCA);
            assertThat(d.getQtdSolicitada()).isEqualTo(16);
            assertThat(d.getSolicitacaoOriginal()).isEqualTo(pedido.eventId());
            assertThat(d.isAtendida()).isFalse();
        });
    }

    @Test
    void skuDesconhecidoNegaSemDemanda() {
        atendimento.processar(pedido(16));

        assertThat(respostasNaOutbox()).singleElement()
                .asInstanceOf(type(ReposicaoNegada.class))
                .extracting(ReposicaoNegada::motivo).isEqualTo(MotivoNegacao.SKU_DESCONHECIDO);
        assertThat(demandas.count()).isZero();
    }

    @Test
    void pedidoDuplicadoExpedeUmaVezSo() {
        cadastra(new NovoLote("L1", 50, hojeMais(30)));
        var pedido = pedido(16);

        atendimento.processar(pedido);
        atendimento.processar(pedido);

        assertThat(respostasNaOutbox()).hasSize(1);
        assertThat(saldo()).isEqualTo(34);
    }
}
