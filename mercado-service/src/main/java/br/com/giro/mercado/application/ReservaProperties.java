package br.com.giro.mercado.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

/** @param ttl quanto tempo a reserva do checkout segura as unidades até o pagamento */
@ConfigurationProperties("mercado.reserva")
public record ReservaProperties(Duration ttl) {

    public ReservaProperties {
        Objects.requireNonNull(ttl, "mercado.reserva.ttl é obrigatório");
    }
}
