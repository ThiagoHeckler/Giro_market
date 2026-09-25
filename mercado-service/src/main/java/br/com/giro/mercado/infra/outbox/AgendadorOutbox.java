package br.com.giro.mercado.infra.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Dispara o publicador em intervalo fixo. Desligável para os testes chamarem o publicador direto. */
@Component
@ConditionalOnBooleanProperty(name = "outbox.publicador-habilitado", matchIfMissing = true)
public class AgendadorOutbox {

    private static final Logger log = LoggerFactory.getLogger(AgendadorOutbox.class);

    private final PublicadorOutbox publicador;

    public AgendadorOutbox(PublicadorOutbox publicador) {
        this.publicador = publicador;
    }

    @Scheduled(fixedDelayString = "${outbox.intervalo}")
    void publicar() {
        try {
            publicador.publicarProntos();
        } catch (RuntimeException e) {
            // Falha de banco, por exemplo. A próxima rodada tenta de novo; o agendador não pode morrer.
            log.error("Rodada do publicador da outbox falhou", e);
        }
    }
}
