package br.com.giro.estoque.web;

import br.com.giro.estoque.IntegracaoTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

class SegurancaTest extends IntegracaoTest {

    private static final String SOLICITACAO = """
            {"eventId":"0b6c3f1e-8a4f-4c55-9d0a-1f2e3d4c5b6a","sku":"7894900011517",
             "qtdFaltante":16,"ocorridoEm":"2026-09-25T12:00:00Z"}""";

    @Autowired
    MockMvcTester mvc;

    private MockMvcTester.MockMvcRequestBuilder evento() {
        return mvc.post().uri("/eventos").contentType(MediaType.APPLICATION_JSON)
                .header("Evento-Tipo", "ReposicaoSolicitada").content(SOLICITACAO);
    }

    private MockMvcTester.MockMvcRequestBuilder login(String senha) {
        return mvc.post().uri("/sessao").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("usuario", OPERADOR).param("senha", senha);
    }

    /** Faz o login como o front faz e devolve a sessão aberta. */
    private MockHttpSession logar() {
        var resultado = login(SENHA_OPERADOR).with(comCsrf()).exchange();
        assertThat(resultado).hasStatus(HttpStatus.NO_CONTENT);
        return (MockHttpSession) resultado.getMvcResult().getRequest().getSession(false);
    }

    @Test
    void eventoSemTokenResponde401SemProcessar() {
        assertThat(evento()).hasStatus(HttpStatus.UNAUTHORIZED);

        assertThat(eventosNaOutbox()).isEmpty();
    }

    @Test
    void eventoComTokenErradoResponde401() {
        assertThat(evento().header("Authorization", "Bearer " + TOKEN_SERVICO.replace('0', '1')))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void operadorNaoEntraNaRotaDeServico() {
        assertThat(evento().with(comoOperador()).with(comCsrf())).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void tokenDeServicoNaoAbreOPainel() {
        assertThat(mvc.get().uri("/painel/resumo").with(comoServico())).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void consultaDeProdutoAceitaOOperador() {
        assertThat(mvc.get().uri("/produtos/7894900011517")).hasStatus(HttpStatus.UNAUTHORIZED);

        assertThat(mvc.get().uri("/produtos/7894900011517").with(comoOperador())).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void painelSemLoginResponde401ComProblemDetail() {
        assertThat(mvc.get().uri("/painel/resumo")).hasStatus(HttpStatus.UNAUTHORIZED)
                .hasHeader("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .doesNotContainHeader("WWW-Authenticate");
    }

    @Test
    void rotaNaoListadaEhNegada() {
        assertThat(mvc.get().uri("/actuator/env").with(comoOperador())).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void saudeFicaAberta() {
        assertThat(mvc.get().uri("/actuator/health")).hasStatusOk();
    }

    @Test
    void loginComOCookieCsrfAbreSessaoDoPainel() {
        // O front primeiro pergunta quem está logado: a resposta traz o cookie do token CSRF.
        var anonimo = mvc.get().uri("/sessao").exchange();
        assertThat(anonimo).hasStatus(HttpStatus.UNAUTHORIZED);
        var cookieCsrf = anonimo.getMvcResult().getResponse().getCookie("XSRF-ESTOQUE");
        assertThat(cookieCsrf).isNotNull();

        var resultado = login(SENHA_OPERADOR)
                .cookie(new Cookie("XSRF-ESTOQUE", cookieCsrf.getValue()))
                .header("X-XSRF-TOKEN", cookieCsrf.getValue())
                .exchange();
        assertThat(resultado).hasStatus(HttpStatus.NO_CONTENT);
        var sessao = (MockHttpSession) resultado.getMvcResult().getRequest().getSession(false);

        assertThat(mvc.get().uri("/sessao").session(sessao)).hasStatusOk()
                .bodyJson().isStrictlyEqualTo("""
                        {"usuario":"operador"}""");
        assertThat(mvc.get().uri("/painel/resumo").session(sessao)).hasStatusOk();
    }

    @Test
    void senhaErradaResponde401() {
        assertThat(login("senha-errada").with(comCsrf())).hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyText().contains("usuário ou senha inválidos");
    }

    @Test
    void loginSemTokenCsrfResponde403() {
        assertThat(login(SENHA_OPERADOR)).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void escritaSemTokenCsrfResponde403MesmoLogado() {
        var sessao = logar();

        assertThat(mvc.post().uri("/lotes").session(sessao).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void logoutEncerraASessao() {
        var sessao = logar();

        assertThat(mvc.delete().uri("/sessao").session(sessao).with(comCsrf())).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(mvc.get().uri("/painel/resumo").session(sessao)).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
