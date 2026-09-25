package br.com.giro.mercado.infra.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Evento a publicar, gravado na mesma transação da mudança de estado (Transactional Outbox).
 * O id é o próprio eventId do contrato, então o consumidor deduplica pelo mesmo valor.
 */
@Entity
@Table(name = "outbox_event")
public class OutboxEvent implements Persistable<UUID> {

    private static final int TAMANHO_MAXIMO_ERRO = 500;

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String tipo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusOutbox status;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "proxima_tentativa_em", nullable = false)
    private Instant proximaTentativaEm;

    @Column(name = "ultimo_erro", length = TAMANHO_MAXIMO_ERRO)
    private String ultimoErro;

    @Column(name = "enviado_em")
    private Instant enviadoEm;

    /** Id é atribuído pela aplicação; sem isto o Spring Data faria merge (SELECT + INSERT). */
    @Transient
    private boolean novo = true;

    protected OutboxEvent() {
        // exigido pelo JPA
    }

    public OutboxEvent(UUID id, String tipo, String payloadJson, Instant criadoEm) {
        this.id = Objects.requireNonNull(id, "id");
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.payload = Objects.requireNonNull(payloadJson, "payload");
        this.criadoEm = Objects.requireNonNull(criadoEm, "criadoEm");
        this.proximaTentativaEm = criadoEm;
        this.status = StatusOutbox.PENDING;
    }

    public void marcarEnviado(Instant quando) {
        this.status = StatusOutbox.SENT;
        this.enviadoEm = quando;
        this.tentativas++;
        this.ultimoErro = null;
    }

    /** Entrega falhou: continua PENDING e só volta ao polling depois de {@code quando}. */
    public void agendarNovaTentativa(String erro, Instant quando) {
        this.tentativas++;
        this.proximaTentativaEm = quando;
        this.ultimoErro = erro == null || erro.length() <= TAMANHO_MAXIMO_ERRO
                ? erro
                : erro.substring(0, TAMANHO_MAXIMO_ERRO);
    }

    @PostLoad
    @PostPersist
    void marcarPersistido() {
        this.novo = false;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return novo;
    }

    public String getTipo() {
        return tipo;
    }

    public String getPayload() {
        return payload;
    }

    public StatusOutbox getStatus() {
        return status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public int getTentativas() {
        return tentativas;
    }

    public Instant getProximaTentativaEm() {
        return proximaTentativaEm;
    }

    public Optional<String> getUltimoErro() {
        return Optional.ofNullable(ultimoErro);
    }

    public Optional<Instant> getEnviadoEm() {
        return Optional.ofNullable(enviadoEm);
    }
}
