package br.com.giro.mercado.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

/**
 * @param ttl                 quanto tempo a reserva do checkout segura as unidades até o pagamento
 * @param expiracaoHabilitada liga a varredura agendada; os testes a desligam e chamam a expiração direto
 * @param intervaloExpiracao  pausa entre uma varredura e a próxima
 * @param loteExpiracao       pedidos vencidos tratados por varredura
 */
@ConfigurationProperties("mercado.reserva")
public record ReservaProperties(
        Duration ttl,
        boolean expiracaoHabilitada,
        Duration intervaloExpiracao,
        int loteExpiracao) {

    public ReservaProperties {
        Objects.requireNonNull(ttl, "mercado.reserva.ttl é obrigatório");
        Objects.requireNonNull(intervaloExpiracao, "mercado.reserva.intervalo-expiracao é obrigatório");
        if (loteExpiracao <= 0) {
            throw new IllegalArgumentException("mercado.reserva.lote-expiracao deve ser positivo: " + loteExpiracao);
        }
    }
}
