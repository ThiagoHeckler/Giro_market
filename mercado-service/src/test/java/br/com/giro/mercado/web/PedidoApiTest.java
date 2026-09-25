package br.com.giro.mercado.web;

import br.com.giro.mercado.IntegracaoTest;
import br.com.giro.mercado.domain.ProdutoVitrine;
import br.com.giro.mercado.infra.persistencia.ProdutoVitrineRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PedidoApiTest extends IntegracaoTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    ProdutoVitrineRepository produtos;

    private MockMvcTester.MockMvcRequestBuilder checkout(String corpo) {
        return mvc.post().uri("/pedidos").contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    private void naVitrine(String sku, String nome, String preco, int prateleira) {
        produtos.save(new ProdutoVitrine(sku, nome, new BigDecimal(preco), prateleira, 1, 10));
    }

    @Test
    void checkoutReservaEOPedidoPodeSerConsultado() {
        naVitrine("7894900011517", "Coca-Cola 2L", "8.99", 10);
        naVitrine("7891000100103", "Leite Integral 1L", "4.59", 10);

        var resposta = checkout("""
                {"itens":[{"sku":"7894900011517","qtd":2},{"sku":"7891000100103","qtd":3}]}""").exchange();

        assertThat(resposta).hasStatus(HttpStatus.CREATED).bodyJson().extractingPath("$.total").isEqualTo(31.75);
        var location = resposta.getMvcResult().getResponse().getHeader("Location");
        assertThat(mvc.get().uri(location)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {"status":"AGUARDANDO_PAGAMENTO","total":31.75,
                 "itens":[{"sku":"7891000100103","nome":"Leite Integral 1L","qtd":3,"precoUnitario":4.59},
                          {"sku":"7894900011517","nome":"Coca-Cola 2L","qtd":2,"precoUnitario":8.99}]}""");
        assertThat(mvc.get().uri(location)).bodyJson().extractingPath("$.reservaExpiraEm").isNotNull();
    }

    @Test
    void faltaDeEstoqueResponde409ApontandoOSku() {
        naVitrine("7894900011517", "Coca-Cola 2L", "8.99", 1);

        assertThat(checkout("""
                {"itens":[{"sku":"7894900011517","qtd":2}]}"""))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.sku").isEqualTo("7894900011517");
    }

    @Test
    void carrinhoVazioResponde400() {
        assertThat(checkout("{\"itens\":[]}")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void quantidadeZeroResponde400() {
        naVitrine("7894900011517", "Coca-Cola 2L", "8.99", 10);

        assertThat(checkout("""
                {"itens":[{"sku":"7894900011517","qtd":0}]}""")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void skuForaDaVitrineResponde422() {
        assertThat(checkout("""
                {"itens":[{"sku":"7894900011517","qtd":1}]}""")).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void pedidoInexistenteResponde404() {
        assertThat(mvc.get().uri("/pedidos/00000000-0000-0000-0000-000000000000")).hasStatus(HttpStatus.NOT_FOUND);
    }
}
