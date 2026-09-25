package br.com.giro.mercado.application;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record ItemCheckout(
        @NotNull @Pattern(regexp = "^([0-9]{8}|[0-9]{12,14})$") String sku,
        @Positive int qtd) {
}
