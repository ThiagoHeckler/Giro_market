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
 * Reposição pedida pelo mercado e negada por falta de saldo.
 * Fica em aberto até a entrada de um lote do mesmo SKU atendê-la.
 */
@Entity
@Table(name = "demanda_reprimida")
public class DemandaReprimida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 14)
    private String sku;

    @Column(name = "qtd_solicitada", nullable = false)
    private int qtdSolicitada;

    /** eventId da {@code ReposicaoSolicitada} que originou esta demanda. */
    @Column(name = "solicitacao_original", nullable = false, unique = true)
    private UUID solicitacaoOriginal;

    @Column(nullable = false)
    private boolean atendida;

    @Column(name = "registrado_em", nullable = false)
    private Instant registradoEm;

    protected DemandaReprimida() {
        // exigido pelo JPA
    }

    public DemandaReprimida(String sku, int qtdSolicitada, UUID solicitacaoOriginal, Instant registradoEm) {
        if (qtdSolicitada <= 0) {
            throw new IllegalArgumentException("qtdSolicitada deve ser positiva: " + qtdSolicitada);
        }
        this.sku = Sku.validar(sku);
        this.qtdSolicitada = qtdSolicitada;
        this.solicitacaoOriginal = Objects.requireNonNull(solicitacaoOriginal, "solicitacaoOriginal");
        this.registradoEm = Objects.requireNonNull(registradoEm, "registradoEm");
        this.atendida = false;
    }

    public void marcarAtendida() {
        this.atendida = true;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public int getQtdSolicitada() {
        return qtdSolicitada;
    }

    public UUID getSolicitacaoOriginal() {
        return solicitacaoOriginal;
    }

    public boolean isAtendida() {
        return atendida;
    }

    public Instant getRegistradoEm() {
        return registradoEm;
    }
}
