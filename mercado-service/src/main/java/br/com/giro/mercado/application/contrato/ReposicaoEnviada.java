package br.com.giro.mercado.application.contrato;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.UUID;

/**
 * Estoque → mercado: lote transferido para a prateleira.
 * {@code correlationId} é o eventId da {@link ReposicaoSolicitada} atendida.
 */
public record ReposicaoEnviada(
        @NotNull UUID eventId,
        @NotNull UUID correlationId,
        @NotNull @Pattern(regexp = "^([0-9]{8}|[0-9]{12,14})$") String sku,
        @Positive int qtd,
        @NotBlank String lote,
        @NotNull Instant ocorridoEm) implements EventoIntegracao {
}
