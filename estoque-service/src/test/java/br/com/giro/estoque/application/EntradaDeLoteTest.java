package br.com.giro.estoque.application;

import br.com.giro.estoque.IntegracaoTest;
import br.com.giro.estoque.WorkerFalso;
import br.com.giro.estoque.application.contrato.ReposicaoEnviada;
import br.com.giro.estoque.application.contrato.ReposicaoNegada;
import br.com.giro.estoque.application.contrato.ReposicaoSolicitada;
import br.com.giro.estoque.domain.ProdutoEstoque;
import br.com.giro.estoque.infra.persistencia.DemandaReprimidaRepository;
import br.com.giro.estoque.infra.persistencia.ProdutoEstoqueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

class EntradaDeLoteTest extends IntegracaoTest {

    private static final String COCA = "7894900011517";

    @Autowired
    EntradaDeLote entradaDeLote;

    @Autowired
    AtendimentoReposicao atendimento;

    @Autowired
    ProdutoEstoqueRepository produtos;

    @Autowired
    DemandaReprimidaRepository demandas;

    private static EntradaLote lote(String codigo, int qtd) {
        return new EntradaLote(COCA, "COCA COLA 2L PET", "22021000", codigo, qtd, LocalDate.now().plusDays(90));
    }

    private ProdutoEstoque produto() {
        return produtos.findById(COCA).orElseThrow();
    }

    @Test
    void skuNovoEhCadastradoClassificadoECreditado() {
        var registrado = entradaDeLote.registrar(lote("L1", 50));

        assertThat(registrado.saldoDisponivel()).isEqualTo(50);
        assertThat(registrado.classificado()).isTrue();
        assertThat(produto().getTags()).contains(List.of("refrigerante", "coca cola", "2l"));
        assertThat(produto().getCategoria()).contains("bebidas");
        assertThat(classificacoesNaOutbox()).singleElement().satisfies(c -> {
            assertThat(c.sku()).isEqualTo(COCA);
            assertThat(c.tags()).containsExactly("refrigerante", "coca cola", "2l");
            assertThat(c.categoria()).isEqualTo("bebidas");
        });
    }

    @Test
    void workerForaDoArNaoImpedeAEntrada() {
        WorkerFalso.responderCom(503, "{\"detail\":\"groq inacessível\"}");

        var registrado = entradaDeLote.registrar(lote("L1", 50));

        assertThat(registrado.saldoDisponivel()).isEqualTo(50);
        assertThat(registrado.classificado()).isFalse();
        assertThat(produto().getTags()).isEmpty();
        assertThat(classificacoesNaOutbox()).isEmpty();
    }

    @Test
    void workerLentoEhAbandonadoNoTimeout() {
        WorkerFalso.atrasar(Duration.ofSeconds(3));
        var inicio = System.nanoTime();

        var registrado = entradaDeLote.registrar(lote("L1", 50));

        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofMillis(2_000));
        assertThat(registrado.classificado()).isFalse();
        assertThat(registrado.saldoDisponivel()).isEqualTo(50);
    }

    @Test
    void produtoJaClassificadoNaoChamaOWorkerDeNovo() {
        entradaDeLote.registrar(lote("L1", 50));
        entradaDeLote.registrar(lote("L2", 30));

        assertThat(WorkerFalso.chamadas()).isOne();
        assertThat(classificacoesNaOutbox()).hasSize(1);
        assertThat(produto().getSaldoDisponivel()).isEqualTo(80);
    }

    @Test
    void skuNovoSemDescricaoEhRecusadoSemChamarOWorker() {
        var semDescricao = new EntradaLote(COCA, null, null, "L1", 50, null);

        assertThatThrownBy(() -> entradaDeLote.registrar(semDescricao)).isInstanceOf(IllegalArgumentException.class);
        assertThat(WorkerFalso.chamadas()).isZero();
        assertThat(produtos.existsById(COCA)).isFalse();
    }

    @Test
    void loteNovoAtendeDemandaReprimidaComACorrelacaoOriginal() {
        produtos.save(new ProdutoEstoque(COCA, "COCA COLA 2L PET", "22021000"));
        var solicitacao = new ReposicaoSolicitada(UUID.randomUUID(), COCA, 16, Instant.now());
        atendimento.processar(solicitacao);   // sem saldo: nega e registra demanda

        var registrado = entradaDeLote.registrar(lote("L1", 50));

        assertThat(registrado.demandasAtendidas()).isOne();
        assertThat(registrado.saldoDisponivel()).isEqualTo(34);
        assertThat(respostasNaOutbox()).hasSize(2)
                .satisfies(eventos -> assertThat(eventos.getFirst()).isInstanceOf(ReposicaoNegada.class))
                .last().asInstanceOf(type(ReposicaoEnviada.class))
                .satisfies(e -> {
                    assertThat(e.correlationId()).isEqualTo(solicitacao.eventId());
                    assertThat(e.qtd()).isEqualTo(16);
                    assertThat(e.lote()).isEqualTo("L1");
                });
        assertThat(demandas.findAll()).singleElement().satisfies(d -> assertThat(d.isAtendida()).isTrue());
    }

    @Test
    void loteMenorQueADemandaEnviaOQueTemEFechaADemanda() {
        produtos.save(new ProdutoEstoque(COCA, "COCA COLA 2L PET", "22021000"));
        atendimento.processar(new ReposicaoSolicitada(UUID.randomUUID(), COCA, 16, Instant.now()));

        entradaDeLote.registrar(lote("L1", 6));

        // O mercado recebe 6, segue abaixo do mínimo e pede o resto num novo gatilho.
        assertThat(respostasNaOutbox()).last().asInstanceOf(type(ReposicaoEnviada.class))
                .extracting(ReposicaoEnviada::qtd).isEqualTo(6);
        assertThat(produto().getSaldoDisponivel()).isZero();
        assertThat(demandas.findAll()).singleElement().satisfies(d -> assertThat(d.isAtendida()).isTrue());
    }

    @Test
    void loteJaVencidoNaEntradaNaoAtendeDemanda() {
        produtos.save(new ProdutoEstoque(COCA, "COCA COLA 2L PET", "22021000"));
        atendimento.processar(new ReposicaoSolicitada(UUID.randomUUID(), COCA, 16, Instant.now()));

        var registrado = entradaDeLote.registrar(
                new EntradaLote(COCA, null, null, "VENCIDO", 50, LocalDate.now().minusDays(1)));

        assertThat(registrado.demandasAtendidas()).isZero();
        assertThat(demandas.findAll()).singleElement().satisfies(d -> assertThat(d.isAtendida()).isFalse());
    }
}
