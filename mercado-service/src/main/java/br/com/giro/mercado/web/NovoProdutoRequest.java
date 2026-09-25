package br.com.giro.mercado.web;

import br.com.giro.mercado.application.NovoProduto;
import br.com.giro.mercado.domain.Sku;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record NovoProdutoRequest(
        @NotNull @Pattern(regexp = Sku.REGEX) String sku,
        @NotBlank @Size(max = 255) String nome,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 8, fraction = 2) BigDecimal preco,
        @Positive int estoqueMinimo,
        @Positive int estoqueIdeal) {

    NovoProduto paraComando() {
        return new NovoProduto(sku, nome.strip(), preco, estoqueMinimo, estoqueIdeal);
    }
}
