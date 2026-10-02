package br.com.giro.mercado.infra.estoque;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * @param url     URL base da API de consulta do estoque
 * @param token   token de serviço que o estoque exige ({@code Authorization: Bearer})
 * @param timeout timeout de conexão e leitura das consultas
 */
@ConfigurationProperties("estoque")
public record EstoqueProperties(URI url, String token, Duration timeout) {

    public EstoqueProperties {
        Objects.requireNonNull(url, "estoque.url é obrigatório");
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("estoque.token é obrigatório: defina ESTOQUE_TOKEN_SERVICO");
        }
        Objects.requireNonNull(timeout, "estoque.timeout é obrigatório");
    }
}
