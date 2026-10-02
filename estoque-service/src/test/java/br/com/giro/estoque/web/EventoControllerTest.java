package br.com.giro.estoque.web;

import br.com.giro.estoque.IntegracaoTest;
import br.com.giro.estoque.application.contrato.ReposicaoNegada;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

class EventoControllerTest extends IntegracaoTest {

    private static final String SOLICITACAO = """
            {"eventId":"0b6c3f1e-8a4f-4c55-9d0a-1f2e3d4c5b6a","sku":"7894900011517",
             "qtdFaltante":16,"ocorridoEm":"2026-09-25T12:00:00Z"}""";

    @Autowired
    MockMvcTester mvc;

    private MockMvcTester.MockMvcRequestBuilder post(String tipo, String corpo) {
        return mvc.post().uri("/eventos").contentType(MediaType.APPLICATION_JSON)
                .header("Evento-Tipo", tipo).content(corpo).with(comoServico());
    }

    @Test
    void eventoValidoEhProcessado() {
        assertThat(post("ReposicaoSolicitada", SOLICITACAO)).hasStatus(HttpStatus.NO_CONTENT);

        // Sem o produto cadastrado, a resposta é uma negativa por SKU desconhecido.
        assertThat(respostasNaOutbox()).singleElement().isInstanceOf(ReposicaoNegada.class);
    }

    @Test
    void reentregaDoMesmoEventoResponde204SemEfeitoRepetido() {
        assertThat(post("ReposicaoSolicitada", SOLICITACAO)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(post("ReposicaoSolicitada", SOLICITACAO)).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(respostasNaOutbox()).hasSize(1);
    }

    @Test
    void payloadQueFereOContratoResponde400() {
        var semQuantidade = SOLICITACAO.replace("16", "0");

        assertThat(post("ReposicaoSolicitada", semQuantidade))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyText().contains("qtdFaltante");
        assertThat(respostasNaOutbox()).isEmpty();
    }

    @Test
    void jsonIlegivelResponde400() {
        assertThat(post("ReposicaoSolicitada", "{nao-e-json")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void tipoQueOEstoqueNaoConsomeResponde400() {
        assertThat(post("ReposicaoEnviada", SOLICITACAO))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyText().contains("não aceito");
    }

    @Test
    void semHeaderDeTipoResponde400() {
        assertThat(mvc.post().uri("/eventos").contentType(MediaType.APPLICATION_JSON).content(SOLICITACAO)
                .with(comoServico()))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }
}
