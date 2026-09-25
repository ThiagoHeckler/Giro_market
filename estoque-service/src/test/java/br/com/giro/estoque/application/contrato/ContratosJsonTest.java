package br.com.giro.estoque.application.contrato;

import br.com.giro.estoque.IntegracaoTest;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Formato de fio dos contratos. Os fixtures em src/test/resources/contratos são idênticos nos dois
 * serviços: se um lado mudar o JSON, o teste do lado que não mudou quebra.
 */
class ContratosJsonTest extends IntegracaoTest {

    private static final Map<String, Class<? extends EventoIntegracao>> TIPOS = Map.of(
            "ReposicaoSolicitada", ReposicaoSolicitada.class,
            "ReposicaoEnviada", ReposicaoEnviada.class,
            "ReposicaoNegada", ReposicaoNegada.class,
            "ProdutoClassificado", ProdutoClassificado.class);

    @Autowired
    Validator validator;

    private static String fixture(String tipo) throws IOException {
        try (InputStream in = ContratosJsonTest.class.getResourceAsStream("/contratos/" + tipo + ".json")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ReposicaoSolicitada", "ReposicaoEnviada", "ReposicaoNegada", "ProdutoClassificado"})
    void fixtureFazIdaEVoltaSemPerderNada(String tipo) throws IOException {
        var original = fixture(tipo);

        var evento = json.readValue(original, TIPOS.get(tipo));

        assertThat(validator.validate(evento)).isEmpty();
        assertThat(json.readTree(json.writeValueAsString(evento))).isEqualTo(json.readTree(original));
    }

    @Test
    void campoNovoDesconhecidoEhIgnorado() throws IOException {
        // Leitor tolerante: o outro serviço pode acrescentar campos sem quebrar este.
        var comCampoExtra = fixture("ReposicaoSolicitada").replace("{", "{\"prioridade\":\"ALTA\",");

        var evento = json.readValue(comCampoExtra, ReposicaoSolicitada.class);

        assertThat(evento.qtdFaltante()).isEqualTo(16);
    }
}
