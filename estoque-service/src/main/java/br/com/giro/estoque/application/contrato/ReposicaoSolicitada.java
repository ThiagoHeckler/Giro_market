package br.com.giro.estoque.application.contrato;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.UUID;

/**
 * Mercado → estoque: prateleira caiu abaixo do mínimo; pede {@code estoqueIdeal - estoquePrateleira}.
 * Contrato duplicado de propósito no mercado-service — não há módulo compartilhado.
 */
public record ReposicaoSolicitada(
        @NotNull UUID eventId,
        @NotNull @Pattern(regexp = "^([0-9]{8}|[0-9]{12,14})$") String sku,
        @Positive int qtdFaltante,
        @NotNull Instant ocorridoEm) {
}
