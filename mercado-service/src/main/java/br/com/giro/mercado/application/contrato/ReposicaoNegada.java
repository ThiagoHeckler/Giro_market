package br.com.giro.mercado.application.contrato;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.UUID;

/**
 * Estoque → mercado: reposição recusada. O mercado marca o produto como ESGOTADO.
 * {@code correlationId} é o eventId da {@link ReposicaoSolicitada} recusada.
 */
public record ReposicaoNegada(
        @NotNull UUID eventId,
        @NotNull UUID correlationId,
        @NotNull @Pattern(regexp = "^([0-9]{8}|[0-9]{12,14})$") String sku,
        @NotNull MotivoNegacao motivo,
        @NotNull Instant ocorridoEm) implements EventoReposicao {
}
