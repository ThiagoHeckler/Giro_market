package br.com.giro.estoque.web;

import br.com.giro.estoque.IntegracaoTest;
import br.com.giro.estoque.domain.ProdutoEstoque;
import br.com.giro.estoque.infra.persistencia.ProdutoEstoqueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProdutoEstoqueControllerTest extends IntegracaoTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    ProdutoEstoqueRepository produtos;

    @Test
    void devolveProdutoComClassificacao() {
        var produto = new ProdutoEstoque("7894900011517", "COCA COLA 2L PET", "22021000");
        produto.classificar(List.of("refrigerante", "2l"), "bebidas");
        produtos.save(produto);

        assertThat(mvc.get().uri("/produtos/7894900011517")).hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        {"sku":"7894900011517","descricao":"COCA COLA 2L PET","tags":["refrigerante","2l"],
                         "categoria":"bebidas","saldoDisponivel":0}""");
    }

    @Test
    void produtoAindaSemClassificacaoVemComTagsNulas() {
        produtos.save(new ProdutoEstoque("7894900011517", "COCA COLA 2L PET", "22021000"));

        assertThat(mvc.get().uri("/produtos/7894900011517")).hasStatusOk()
                .bodyJson().extractingPath("$.tags").isNull();
    }

    @Test
    void skuDesconhecidoResponde404() {
        assertThat(mvc.get().uri("/produtos/7894900011517")).hasStatus(HttpStatus.NOT_FOUND);
    }
}
