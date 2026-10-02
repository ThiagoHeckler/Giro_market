package br.com.giro.estoque.infra.seguranca;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Segredos vêm do ambiente (ou do {@code .env} da raiz em dev); sem eles o serviço não sobe.
 *
 * @param tokenServico token que o estoque exige de outro serviço em {@code Authorization: Bearer}
 * @param operador     credenciais do operador do painel
 */
@Validated
@ConfigurationProperties("seguranca")
public record SegurancaProperties(
        @NotBlank(message = "defina ESTOQUE_TOKEN_SERVICO (gere com: openssl rand -hex 32)")
        @Size(min = 32, message = "ESTOQUE_TOKEN_SERVICO precisa de ao menos 32 caracteres")
        String tokenServico,
        @NotNull @Valid Operador operador) {

    public record Operador(
            @NotBlank(message = "defina OPERADOR_USUARIO") String usuario,
            @NotBlank(message = "defina OPERADOR_SENHA")
            @Size(min = 8, message = "OPERADOR_SENHA precisa de ao menos 8 caracteres") String senha) {
    }
}
