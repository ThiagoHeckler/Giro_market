package br.com.giro.estoque.web;

import br.com.giro.estoque.application.EntradaLote;
import br.com.giro.estoque.domain.Sku;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** {@code descricao} e {@code ncm} são obrigatórios só para SKU ainda não cadastrado. */
public record EntradaLoteRequest(
        @NotNull @Pattern(regexp = Sku.REGEX) String sku,
        @Size(max = 255) String descricao,
        @Pattern(regexp = "^[0-9]{8}$") String ncm,
        @NotBlank @Size(max = 50) String codigoLote,
        @Positive int quantidade,
        LocalDate validade) {

    EntradaLote paraComando() {
        return new EntradaLote(sku, descricao, ncm, codigoLote, quantidade, validade);
    }
}
