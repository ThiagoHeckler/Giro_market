package br.com.giro.estoque.web;

import br.com.giro.estoque.IntegracaoTest;
import br.com.giro.estoque.application.AtendimentoReposicao;
import br.com.giro.estoque.application.EntradaDeLote;
import br.com.giro.estoque.application.EntradaLote;
import br.com.giro.estoque.application.contrato.ReposicaoSolicitada;
import br.com.giro.estoque.domain.ProdutoEstoque;
import br.com.giro.estoque.infra.persistencia.ProdutoEstoqueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PainelControllerTest extends IntegracaoTest {

    private static final String COCA = "7894900011517";
    private static final String LEITE = "7891000068019";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    EntradaDeLote entradaDeLote;

    @Autowired
    AtendimentoReposicao atendimento;

    @Autowired
    ProdutoEstoqueRepository produtos;

    @Autowired
    Clock relogio;

    private LocalDate hojeMais(int dias) {
        return LocalDate.now(relogio).plusDays(dias);
    }

    private void entrada(String codigo, int qtd, LocalDate validade) {
        entradaDeLote.registrar(new EntradaLote(COCA, "COCA COLA 2L PET", "22021000", codigo, qtd, validade));
    }

    private ReposicaoSolicitada pedido(String sku, int qtd) {
        var pedido = new ReposicaoSolicitada(UUID.randomUUID(), sku, qtd, Instant.now());
        atendimento.processar(pedido);
        return pedido;
    }

    @Test
    void resumoContaSoOQueEhDoEstoque() {
        entrada("VENCE-NO-LIMITE", 10, hojeMais(30));
        entrada("VENCE-DEPOIS", 10, hojeMais(31));
        entrada("VENCIDO", 10, hojeMais(-1));
        entrada("SEM-VALIDADE", 10, null);
        produtos.save(new ProdutoEstoque(LEITE, "LEITE INTEGRAL 1L", "04012010"));
        pedido(LEITE, 12);

        assertThat(mvc.get().uri("/painel/resumo").with(comoOperador())).hasStatusOk()
                .bodyJson().isStrictlyEqualTo("""
                        {"skusCadastrados":2,"skusSemSaldo":1,"demandasAbertas":1,"lotesVencendo":1}""");
    }

    @Test
    void skuSoComLoteVencidoContaComoSemSaldo() {
        entrada("VENCIDO", 60, hojeMais(-1));

        assertThat(mvc.get().uri("/painel/resumo").with(comoOperador())).hasStatusOk()
                .bodyJson().extractingPath("$.skusSemSaldo").isEqualTo(1);
    }

    @Test
    void loteEsgotadoNaoContaComoVencendo() {
        entrada("L1", 10, hojeMais(5));
        pedido(COCA, 10);

        assertThat(mvc.get().uri("/painel/resumo").with(comoOperador())).hasStatusOk()
                .bodyJson().extractingPath("$.lotesVencendo").isEqualTo(0);
    }

    @Test
    void demandasAbertasNaOrdemDeAtendimentoComADescricao() {
        produtos.save(new ProdutoEstoque(LEITE, "LEITE INTEGRAL 1L", "04012010"));
        produtos.save(new ProdutoEstoque(COCA, "COCA COLA 2L PET", "22021000"));
        pedido(LEITE, 12);
        pedido(COCA, 30);

        assertThat(mvc.get().uri("/painel/demandas").with(comoOperador())).hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        [{"sku":"7891000068019","descricao":"LEITE INTEGRAL 1L","qtdSolicitada":12},
                         {"sku":"7894900011517","descricao":"COCA COLA 2L PET","qtdSolicitada":30}]""");
    }

    @Test
    void demandaAtendidaSaiDaLista() {
        produtos.save(new ProdutoEstoque(COCA, "COCA COLA 2L PET", "22021000"));
        pedido(COCA, 30);

        entrada("L1", 50, hojeMais(60));

        assertThat(mvc.get().uri("/painel/demandas").with(comoOperador())).hasStatusOk().bodyJson().isStrictlyEqualTo("[]");
    }

    @Test
    void reposicoesMaisRecentesPrimeiroComLoteEStatusDeEntrega() {
        entrada("L1", 50, hojeMais(60));
        var primeiro = pedido(COCA, 10);
        pedido(COCA, 5);
        jdbc.update("UPDATE outbox_event SET status = 'SENT', enviado_em = now() WHERE payload ->> 'correlationId' = ?",
                primeiro.eventId().toString());

        var resposta = assertThat(mvc.get().uri("/painel/reposicoes").with(comoOperador())).hasStatusOk().bodyJson();
        resposta.isLenientlyEqualTo("""
                [{"sku":"7894900011517","descricao":"COCA COLA 2L PET","qtd":5,"codigoLote":"L1","entregueEm":null},
                 {"qtd":10,"codigoLote":"L1"}]""");
        resposta.extractingPath("$[1].entregueEm").isNotNull();
    }

    @Test
    void limiteDeReposicoesEhRespeitado() {
        entrada("L1", 50, hojeMais(60));
        pedido(COCA, 1);
        pedido(COCA, 2);
        pedido(COCA, 3);

        assertThat(mvc.get().uri("/painel/reposicoes?limite=2").with(comoOperador())).hasStatusOk()
                .bodyJson().extractingPath("$.length()").isEqualTo(2);
    }
}
