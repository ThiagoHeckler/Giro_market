package br.com.giro.estoque.infra.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Registro de evento já consumido. A PK no eventId torna o consumo idempotente. */
@Entity
@Table(name = "inbox_event")
public class InboxEvent implements Persistable<UUID> {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "processado_em", nullable = false)
    private Instant processadoEm;

    @Transient
    private boolean novo = true;

    protected InboxEvent() {
        // exigido pelo JPA
    }

    public InboxEvent(UUID eventId, Instant processadoEm) {
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.processadoEm = Objects.requireNonNull(processadoEm, "processadoEm");
    }

    @PostLoad
    @PostPersist
    void marcarPersistido() {
        this.novo = false;
    }

    @Override
    public UUID getId() {
        return eventId;
    }

    @Override
    public boolean isNew() {
        return novo;
    }

    public Instant getProcessadoEm() {
        return processadoEm;
    }
}
