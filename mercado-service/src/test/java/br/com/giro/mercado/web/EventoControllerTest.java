package br.com.giro.mercado.web;

import br.com.giro.mercado.IntegracaoTest;
import br.com.giro.mercado.application.CheckoutService;
import br.com.giro.mercado.application.ItemCheckout;
import br.com.giro.mercado.domain.ProdutoVitrine;
import br.com.giro.mercado.domain.StatusSolicitacao;
import br.com.giro.mercado.infra.persistencia.ProdutoVitrineRepository;
import br.com.giro.mercado.infra.persistencia.SolicitacaoReposicaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventoControllerTest extends IntegracaoTest {

    private static final String COCA = "7894900011517";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    ProdutoVitrineRepository produtos;

    @Autowired
    SolicitacaoReposicaoRepository solicitacoes;

    @Autowired
    CheckoutService checkout;

    private MockMvcTester.MockMvcRequestBuilder post(String tipo, String corpo) {
        return mvc.post().uri("/eventos").contentType(MediaType.APPLICATION_JSON)
                .header("Evento-Tipo", tipo).content(corpo);
    }

    private UUID solicitacaoAberta() {
        produtos.save(new ProdutoVitrine(COCA, "COCA COLA 2L PET", new BigDecimal("9.99"), 10, 5, 20));
        checkout.fechar(List.of(new ItemCheckout(COCA, 6)));   // prateleira 4: pede 16
        return solicitacoes.findAll().getFirst().getEventId();
    }

    private static String enviada(UUID correlationId, int qtd) {
        return """
                {"eventId":"5f1d2c3b-4a59-4e68-8b7a-6c5d4e3f2a1b","correlationId":"%s",
                 "sku":"7894900011517","qtd":%d,"lote":"L1","ocorridoEm":"2026-09-25T12:00:05Z"}"""
                .formatted(correlationId, qtd);
    }

    @Test
    void reposicaoEnviadaCreditaAPrateleira() {
        var solicitacao = solicitacaoAberta();

        assertThat(post("ReposicaoEnviada", enviada(solicitacao, 16))).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(produtos.findById(COCA).orElseThrow().getEstoquePrateleira()).isEqualTo(20);
        assertThat(solicitacoes.findByEventId(solicitacao).orElseThrow().getStatus())
                .isEqualTo(StatusSolicitacao.ATENDIDA);
    }

    @Test
    void reentregaDoMesmoEventoResponde204SemCreditarDeNovo() {
        var solicitacao = solicitacaoAberta();

        assertThat(post("ReposicaoEnviada", enviada(solicitacao, 16))).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(post("ReposicaoEnviada", enviada(solicitacao, 16))).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(produtos.findById(COCA).orElseThrow().getEstoquePrateleira()).isEqualTo(20);
    }

    @Test
    void reposicaoNegadaDeixaSolicitacaoAguardandoLote() {
        var solicitacao = solicitacaoAberta();
        var negada = """
                {"eventId":"9e8d7c6b-5a49-4382-a1b0-c9d8e7f6a5b4","correlationId":"%s",
                 "sku":"7894900011517","motivo":"SEM_SALDO","ocorridoEm":"2026-09-25T12:00:05Z"}"""
                .formatted(solicitacao);

        assertThat(post("ReposicaoNegada", negada)).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(solicitacoes.findByEventId(solicitacao).orElseThrow().getStatus())
                .isEqualTo(StatusSolicitacao.AGUARDANDO_LOTE);
    }

    @Test
    void motivoForaDoContratoResponde400() {
        var negada = """
                {"eventId":"9e8d7c6b-5a49-4382-a1b0-c9d8e7f6a5b4","correlationId":"%s",
                 "sku":"7894900011517","motivo":"CAIU_DO_CAMINHAO","ocorridoEm":"2026-09-25T12:00:05Z"}"""
                .formatted(UUID.randomUUID());

        assertThat(post("ReposicaoNegada", negada)).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void quantidadeNaoPositivaResponde400() {
        assertThat(post("ReposicaoEnviada", enviada(UUID.randomUUID(), 0)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyText().contains("qtd");
    }

    @Test
    void tipoQueOMercadoNaoConsomeResponde400() {
        assertThat(post("ReposicaoSolicitada", enviada(UUID.randomUUID(), 1)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyText().contains("não aceito");
    }

    @Test
    void produtoClassificadoPosicionaOProdutoNaVitrine() {
        produtos.save(new ProdutoVitrine(COCA, "COCA COLA 2L PET", new BigDecimal("9.99"), 10, 5, 20));
        var classificado = """
                {"eventId":"3c2b1a09-8f7e-4d6c-9b5a-4e3d2c1b0a9f","sku":"7894900011517",
                 "tags":["refrigerante","coca cola"],"categoria":"bebidas","ocorridoEm":"2026-09-25T12:00:05Z"}""";

        assertThat(post("ProdutoClassificado", classificado)).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(produtos.findById(COCA).orElseThrow().getCategoria()).contains("bebidas");
    }
}
