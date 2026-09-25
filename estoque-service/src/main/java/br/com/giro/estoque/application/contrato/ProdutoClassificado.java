package br.com.giro.estoque.application.contrato;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Estoque → mercado: o tag-worker classificou o produto. Só posicionamento na vitrine —
 * tags nunca são identidade nem influenciam a reposição.
 */
public record ProdutoClassificado(
        @NotNull UUID eventId,
        @NotNull @Pattern(regexp = "^([0-9]{8}|[0-9]{12,14})$") String sku,
        @NotEmpty @Size(max = 8) List<@NotBlank @Size(max = 30) String> tags,
        @NotBlank @Size(max = 30) String categoria,
        @NotNull Instant ocorridoEm) implements EventoIntegracao {
}
