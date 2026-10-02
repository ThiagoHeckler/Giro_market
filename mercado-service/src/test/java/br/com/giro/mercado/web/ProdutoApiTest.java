package br.com.giro.mercado.web;

import br.com.giro.mercado.DestinoFalso;
import br.com.giro.mercado.IntegracaoTest;
import br.com.giro.mercado.domain.ProdutoVitrine;
import br.com.giro.mercado.infra.persistencia.ProdutoVitrineRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProdutoApiTest extends IntegracaoTest {

    private static final String COCA = "7894900011517";
    private static final String NOVO = """
            {"sku":"7894900011517","nome":"Coca-Cola 2L PET","preco":8.99,"estoqueMinimo":5,"estoqueIdeal":20}""";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    ProdutoVitrineRepository produtos;

    private MockMvcTester.MockMvcRequestBuilder cadastrar(String corpo) {
        return mvc.post().uri("/produtos").contentType(MediaType.APPLICATION_JSON).content(corpo)
                .with(comoOperador()).with(comCsrf());
    }

    private void naVitrine(String sku, String nome, int prateleira, String categoria) {
        var p = new ProdutoVitrine(sku, nome, new BigDecimal("5.00"), prateleira, 1, 10);
        if (categoria != null) {
            p.classificar(List.of(categoria), categoria, Instant.now());
        }
        produtos.save(p);
    }

    @Test
    void cadastroHerdaClassificacaoDoEstoqueEPedeOPrimeiroLote() {
        assertThat(cadastrar(NOVO)).hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "/produtos/" + COCA)
                .bodyJson().isLenientlyEqualTo("""
                        {"sku":"7894900011517","status":"ESGOTADO","disponivel":0,
                         "categoria":"bebidas","tags":["refrigerante","coca cola"]}""");

        // Prateleira 0 < mínimo 5: o gatilho pede até o ideal, sem ninguém precisar lembrar.
        assertThat(reposicoesSolicitadas(COCA)).containsExactly(20);
    }

    @Test
    void skuQueOEstoqueNaoConheceResponde422() {
        DestinoFalso.responderProdutoCom(404, "{}");

        assertThat(cadastrar(NOVO)).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(produtos.existsById(COCA)).isFalse();
        assertThat(reposicoesSolicitadas(COCA)).isEmpty();
    }

    @Test
    void estoqueForaDoArNaoImpedeOCadastro() {
        DestinoFalso.responderProdutoCom(503, "{}");

        assertThat(cadastrar(NOVO)).hasStatus(HttpStatus.CREATED)
                .bodyJson().extractingPath("$.categoria").isNull();
    }

    @Test
    void produtoAindaNaoClassificadoNoEstoqueEntraSemTags() {
        DestinoFalso.responderProdutoCom(200, """
                {"sku":"7894900011517","descricao":"COCA COLA 2L PET","tags":null,"categoria":null}""");

        assertThat(cadastrar(NOVO)).hasStatus(HttpStatus.CREATED)
                .bodyJson().extractingPath("$.tags").asArray().isEmpty();
    }

    @Test
    void skuJaNaVitrineResponde409() {
        assertThat(cadastrar(NOVO)).hasStatus(HttpStatus.CREATED);

        assertThat(cadastrar(NOVO)).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void minimoMaiorQueIdealResponde400() {
        assertThat(cadastrar(NOVO.replace("\"estoqueMinimo\":5", "\"estoqueMinimo\":30")))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void precoInvalidoResponde400ComDetalheDoCampo() {
        assertThat(cadastrar(NOVO.replace("8.99", "0"))).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void listaDisponiveisPrimeiroEFiltraPorCategoriaEBusca() {
        naVitrine("7894900011517", "Coca-Cola 2L", 10, "bebidas");
        naVitrine("7891000100103", "Leite Integral 1L", 10, "laticinios");
        naVitrine("7896004000015", "Água Mineral 500ml", 0, "bebidas");

        assertThat(mvc.get().uri("/produtos")).hasStatusOk()
                .bodyJson().extractingPath("$[*].nome")
                .isEqualTo(List.of("Coca-Cola 2L", "Leite Integral 1L", "Água Mineral 500ml"));
        assertThat(mvc.get().uri("/produtos?categoria=bebidas")).hasStatusOk()
                .bodyJson().extractingPath("$[*].sku").isEqualTo(List.of("7894900011517", "7896004000015"));
        assertThat(mvc.get().uri("/produtos?busca=leite")).hasStatusOk()
                .bodyJson().extractingPath("$[*].sku").isEqualTo(List.of("7891000100103"));
        assertThat(mvc.get().uri("/produtos?busca=7896004000015")).hasStatusOk()
                .bodyJson().extractingPath("$[*].nome").isEqualTo(List.of("Água Mineral 500ml"));
    }

    @Test
    void detalheDeSkuInexistenteResponde404() {
        assertThat(mvc.get().uri("/produtos/7894900011517")).hasStatus(HttpStatus.NOT_FOUND);
    }
}
