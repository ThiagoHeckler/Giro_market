package br.com.giro.mercado.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Pedido de reposição feito ao estoque; responde {@code temSolicitacaoPendente(sku)}. */
@Entity
@Table(name = "solicitacao_reposicao")
public class SolicitacaoReposicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** eventId da {@code ReposicaoSolicitada}; volta como correlationId nas respostas. */
    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(nullable = false, length = 14)
    private String sku;

    @Column(name = "qtd_solicitada", nullable = false)
    private int qtdSolicitada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSolicitacao status;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected SolicitacaoReposicao() {
        // exigido pelo JPA
    }

    public SolicitacaoReposicao(UUID eventId, String sku, int qtdSolicitada, Instant criadoEm) {
        if (qtdSolicitada <= 0) {
            throw new IllegalArgumentException("qtdSolicitada deve ser positiva: " + qtdSolicitada);
        }
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.sku = Sku.validar(sku);
        this.qtdSolicitada = qtdSolicitada;
        this.criadoEm = Objects.requireNonNull(criadoEm, "criadoEm");
        this.status = StatusSolicitacao.PENDENTE;
    }

    /** Lote recebido. Vale também para a demanda reprimida atendida depois da negativa. */
    public void atender() {
        exigirEmAberto();
        status = StatusSolicitacao.ATENDIDA;
    }

    /** Negada por falta de saldo: segue em aberto até o estoque receber lote. */
    public void aguardarLote() {
        exigirStatus(StatusSolicitacao.PENDENTE);
        status = StatusSolicitacao.AGUARDANDO_LOTE;
    }

    public void cancelar() {
        exigirStatus(StatusSolicitacao.PENDENTE);
        status = StatusSolicitacao.CANCELADA;
    }

    private void exigirEmAberto() {
        if (!status.emAberto()) {
            throw new IllegalStateException("Solicitação %s já encerrada (%s)".formatted(eventId, status));
        }
    }

    private void exigirStatus(StatusSolicitacao esperado) {
        if (status != esperado) {
            throw new IllegalStateException(
                    "Solicitação %s em %s; esperado %s".formatted(eventId, status, esperado));
        }
    }

    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getSku() {
        return sku;
    }

    public int getQtdSolicitada() {
        return qtdSolicitada;
    }

    public StatusSolicitacao getStatus() {
        return status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
