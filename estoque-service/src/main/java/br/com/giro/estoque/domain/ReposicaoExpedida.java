package br.com.giro.estoque.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Envio de unidades de um lote para a prateleira do mercado. Imutável: é o rastro de recall
 * (quais envios saíram de um lote) e o histórico de reposições do painel.
 */
@Entity
@Table(name = "reposicao_expedida")
public class ReposicaoExpedida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** eventId da {@code ReposicaoEnviada} publicada para este envio. */
    @Column(name = "event_id", nullable = false, unique = true, updatable = false)
    private UUID eventId;

    /** eventId da {@code ReposicaoSolicitada} atendida. */
    @Column(name = "correlation_id", nullable = false, updatable = false)
    private UUID correlationId;

    @Column(nullable = false, length = 14, updatable = false)
    private String sku;

    @Column(name = "lote_id", nullable = false, updatable = false)
    private Long loteId;

    @Column(nullable = false, updatable = false)
    private int qtd;

    @Column(name = "expedido_em", nullable = false, updatable = false)
    private Instant expedidoEm;

    protected ReposicaoExpedida() {
        // exigido pelo JPA
    }

    public ReposicaoExpedida(UUID eventId, UUID correlationId, Lote lote, int qtd, Instant expedidoEm) {
        if (qtd <= 0) {
            throw new IllegalArgumentException("qtd expedida deve ser positiva: " + qtd);
        }
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
        this.sku = lote.getSku();
        this.loteId = Objects.requireNonNull(lote.getId(), "lote ainda não persistido");
        this.qtd = qtd;
        this.expedidoEm = Objects.requireNonNull(expedidoEm, "expedidoEm");
    }

    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public String getSku() {
        return sku;
    }

    public Long getLoteId() {
        return loteId;
    }

    public int getQtd() {
        return qtd;
    }

    public Instant getExpedidoEm() {
        return expedidoEm;
    }
}
