package br.com.giro.estoque.application.contrato;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ContratosEventoTest {

    private static jakarta.validation.ValidatorFactory fabrica;
    private static Validator validator;

    @BeforeAll
    static void criaValidator() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validator = fabrica.getValidator();
    }

    @AfterAll
    static void fechaValidator() {
        fabrica.close();
    }

    @Test
    void reposicaoSolicitadaValidaEhAceita() {
        var evento = new ReposicaoSolicitada(UUID.randomUUID(), "7894900011517", 12, Instant.now());

        assertThat(validator.validate(evento)).isEmpty();
    }

    @Test
    void reposicaoSolicitadaRejeitaSkuForaDoPadraoGtinEQuantidadeNaoPositiva() {
        var evento = new ReposicaoSolicitada(UUID.randomUUID(), "COCA-2L", 0, Instant.now());

        assertThat(validator.validate(evento))
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("sku", "qtdFaltante");
    }

    @Test
    void reposicaoEnviadaExigeCorrelacaoELote() {
        var evento = new ReposicaoEnviada(UUID.randomUUID(), null, "7894900011517", 12, " ", Instant.now());

        assertThat(validator.validate(evento))
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("correlationId", "lote");
    }

    @Test
    void reposicaoNegadaExigeMotivo() {
        var evento = new ReposicaoNegada(UUID.randomUUID(), UUID.randomUUID(), "7894900011517", null, Instant.now());

        assertThat(validator.validate(evento))
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .containsExactly("motivo");
    }

    @Test
    void produtoClassificadoExigeTagsECategoria() {
        var evento = new ProdutoClassificado(UUID.randomUUID(), "7894900011517", List.of(), " ", Instant.now());

        assertThat(validator.validate(evento))
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("tags", "categoria");
    }

    @Test
    void produtoClassificadoLimitaTamanhoDeCadaTag() {
        var tagLonga = "x".repeat(31);
        var evento = new ProdutoClassificado(UUID.randomUUID(), "7894900011517", List.of("bebida", tagLonga),
                "bebidas", Instant.now());

        assertThat(validator.validate(evento)).hasSize(1);
    }
}
