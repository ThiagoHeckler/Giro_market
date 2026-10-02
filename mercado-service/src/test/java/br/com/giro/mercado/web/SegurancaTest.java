package br.com.giro.mercado.web;

import br.com.giro.mercado.IntegracaoTest;
import br.com.giro.mercado.domain.ProdutoVitrine;
import br.com.giro.mercado.infra.persistencia.ProdutoVitrineRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SegurancaTest extends IntegracaoTest {

    private static final String COCA = "7894900011517";
    private static final String NOVO_PRODUTO = """
            {"sku":"7894900011517","nome":"Coca-Cola 2L PET","preco":8.99,"estoqueMinimo":6,"estoqueIdeal":24}""";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    ProdutoVitrineRepository produtos;

    private MockMvcTester.MockMvcRequestBuilder cadastro() {
        return mvc.post().uri("/produtos").contentType(MediaType.APPLICATION_JSON).content(NOVO_PRODUTO);
    }

    private MockMvcTester.MockMvcRequestBuilder login(String senha) {
        return mvc.post().uri("/sessao").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("usuario", OPERADOR).param("senha", senha);
    }

    @Test
    void vitrineECheckoutSaoAbertosSemCsrf() {
        produtos.save(new ProdutoVitrine(COCA, "Coca-Cola 2L PET", new BigDecimal("8.99"), 10, 5, 20));

        assertThat(mvc.get().uri("/produtos")).hasStatusOk();
        assertThat(mvc.get().uri("/produtos/" + COCA)).hasStatusOk();
        assertThat(mvc.post().uri("/pedidos").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"itens":[{"sku":"7894900011517","qtd":1}]}"""))
                .hasStatus(HttpStatus.CREATED);
    }

    @Test
    void eventoSemTokenResponde401() {
        assertThat(mvc.post().uri("/eventos").contentType(MediaType.APPLICATION_JSON)
                .header("Evento-Tipo", "ReposicaoEnviada").content("{}"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void cadastroSemLoginResponde401SemCadastrar() {
        assertThat(cadastro().with(comCsrf())).hasStatus(HttpStatus.UNAUTHORIZED)
                .doesNotContainHeader("WWW-Authenticate");

        assertThat(produtos.count()).isZero();
    }

    @Test
    void tokenDeServicoNaoCadastraProduto() {
        assertThat(cadastro().with(comoServico())).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void rotaNaoListadaEhNegada() {
        assertThat(mvc.get().uri("/actuator/env").with(comoOperador())).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void loginComOCookieCsrfLiberaOCadastro() {
        var anonimo = mvc.get().uri("/sessao").exchange();
        assertThat(anonimo).hasStatus(HttpStatus.UNAUTHORIZED);
        var cookieCsrf = anonimo.getMvcResult().getResponse().getCookie("XSRF-MERCADO");
        assertThat(cookieCsrf).isNotNull();

        var resultado = login(SENHA_OPERADOR)
                .cookie(new Cookie("XSRF-MERCADO", cookieCsrf.getValue()))
                .header("X-XSRF-TOKEN", cookieCsrf.getValue())
                .exchange();
        assertThat(resultado).hasStatus(HttpStatus.NO_CONTENT);
        var sessao = (MockHttpSession) resultado.getMvcResult().getRequest().getSession(false);

        assertThat(cadastro().session(sessao).with(comCsrf())).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void cadastroLogadoSemCsrfResponde403() {
        var resultado = login(SENHA_OPERADOR).with(comCsrf()).exchange();
        var sessao = (MockHttpSession) resultado.getMvcResult().getRequest().getSession(false);

        assertThat(cadastro().session(sessao)).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void senhaErradaResponde401() {
        assertThat(login("senha-errada").with(comCsrf())).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
