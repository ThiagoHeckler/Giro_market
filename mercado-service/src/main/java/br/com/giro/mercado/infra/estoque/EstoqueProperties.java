package br.com.giro.mercado.infra.estoque;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * @param url     URL base da API de consulta do estoque
 * @param timeout timeout de conexão e leitura das consultas
 */
@ConfigurationProperties("estoque")
public record EstoqueProperties(URI url, Duration timeout) {

    public EstoqueProperties {
        Objects.requireNonNull(url, "estoque.url é obrigatório");
        Objects.requireNonNull(timeout, "estoque.timeout é obrigatório");
    }
}
