package br.com.giro.mercado.infra.outbox;

import br.com.giro.mercado.application.contrato.EventoReposicao;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Porta de saída de eventos. Só grava na tabela outbox_event; quem publica é o job de polling.
 * {@link Propagation#MANDATORY} faz falhar qualquer tentativa de registrar evento fora de uma
 * transação de negócio — a regra do Transactional Outbox vira garantia em tempo de execução.
 */
@Component
public class Outbox {

    private final OutboxEventRepository eventos;
    private final JsonMapper json;

    public Outbox(OutboxEventRepository eventos, JsonMapper json) {
        this.eventos = eventos;
        this.json = json;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(EventoReposicao evento) {
        eventos.save(new OutboxEvent(
                evento.eventId(),
                evento.getClass().getSimpleName(),
                json.writeValueAsString(evento),
                evento.ocorridoEm()));
    }
}
