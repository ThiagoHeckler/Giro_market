package br.com.giro.estoque.infra.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * @param destino            URL base do serviço que recebe os eventos (POST {destino}/eventos)
 * @param token              token de serviço que o destino exige ({@code Authorization: Bearer})
 * @param publicadorHabilitado liga o polling agendado; os testes o desligam e chamam o publicador direto
 * @param intervalo          pausa entre uma rodada de polling e a próxima
 * @param tamanhoLote        eventos travados por rodada
 * @param timeout            timeout de conexão e de leitura do envio
 * @param backoffMaximo      teto do backoff exponencial entre tentativas
 */
@ConfigurationProperties("outbox")
public record OutboxProperties(
        URI destino,
        String token,
        boolean publicadorHabilitado,
        Duration intervalo,
        int tamanhoLote,
        Duration timeout,
        Duration backoffMaximo) {

    public OutboxProperties {
        Objects.requireNonNull(destino, "outbox.destino é obrigatório");
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("outbox.token é obrigatório: defina MERCADO_TOKEN_SERVICO");
        }
        Objects.requireNonNull(intervalo, "outbox.intervalo é obrigatório");
        Objects.requireNonNull(timeout, "outbox.timeout é obrigatório");
        Objects.requireNonNull(backoffMaximo, "outbox.backoff-maximo é obrigatório");
        if (tamanhoLote <= 0) {
            throw new IllegalArgumentException("outbox.tamanho-lote deve ser positivo: " + tamanhoLote);
        }
    }
}
