package br.com.giro.estoque.infra.inbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/** Porta de entrada idempotente: o registro na inbox e o efeito do evento commitam juntos. */
@Component
public class Inbox {

    private final InboxEventRepository eventos;
    private final Clock relogio;

    public Inbox(InboxEventRepository eventos, Clock relogio) {
        this.eventos = eventos;
        this.relogio = relogio;
    }

    /** @return {@code true} se o evento é inédito e deve ser processado. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean registrarSeNovo(UUID eventId) {
        return eventos.registrarSeNovo(eventId, relogio.instant()) == 1;
    }
}
