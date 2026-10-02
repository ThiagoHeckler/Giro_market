package br.com.giro.mercado.infra.reserva;

import br.com.giro.mercado.application.ExpiracaoDeReservas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Varre as reservas vencidas em intervalo fixo. Desligável para os testes chamarem a expiração direto. */
@Component
@ConditionalOnBooleanProperty(name = "mercado.reserva.expiracao-habilitada", matchIfMissing = true)
public class AgendadorExpiracaoReservas {

    private static final Logger log = LoggerFactory.getLogger(AgendadorExpiracaoReservas.class);

    private final ExpiracaoDeReservas expiracao;

    public AgendadorExpiracaoReservas(ExpiracaoDeReservas expiracao) {
        this.expiracao = expiracao;
    }

    @Scheduled(fixedDelayString = "${mercado.reserva.intervalo-expiracao}")
    void expirar() {
        try {
            expiracao.expirarVencidas();
        } catch (RuntimeException e) {
            // Falha de banco, por exemplo. A próxima rodada tenta de novo; o agendador não pode morrer.
            log.error("Rodada de expiração de reservas falhou", e);
        }
    }
}
