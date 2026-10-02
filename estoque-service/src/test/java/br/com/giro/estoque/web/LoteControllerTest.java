package br.com.giro.estoque.web;

import br.com.giro.estoque.IntegracaoTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

class LoteControllerTest extends IntegracaoTest {

    private static final String LOTE = """
            {"sku":"7894900011517","descricao":"COCA COLA 2L PET","ncm":"22021000",
             "codigoLote":"L2026-09-A","quantidade":50,"validade":"2027-03-31"}""";

    @Autowired
    MockMvcTester mvc;

    private MockMvcTester.MockMvcRequestBuilder post(String corpo) {
        return mvc.post().uri("/lotes").contentType(MediaType.APPLICATION_JSON).content(corpo)
                .with(comoOperador()).with(comCsrf());
    }

    @Test
    void registraLoteERespondeOSaldo() {
        assertThat(post(LOTE)).hasStatus(HttpStatus.CREATED)
                .bodyJson().extractingPath("$.saldoDisponivel").isEqualTo(50);
    }

    @Test
    void quantidadeNaoPositivaResponde400() {
        assertThat(post(LOTE.replace("\"quantidade\":50", "\"quantidade\":0"))).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void skuNovoSemDescricaoResponde400() {
        var semDescricao = """
                {"sku":"7894900011517","codigoLote":"L1","quantidade":50}""";

        assertThat(post(semDescricao)).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyText().contains("exige descricao e ncm");
    }

    @Test
    void mesmoCodigoDeLoteParaOSkuResponde409() {
        assertThat(post(LOTE)).hasStatus(HttpStatus.CREATED);

        assertThat(post(LOTE)).hasStatus(HttpStatus.CONFLICT);
    }
}
