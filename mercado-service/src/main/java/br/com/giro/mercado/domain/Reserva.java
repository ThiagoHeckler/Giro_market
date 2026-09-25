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

/** Unidades separadas no checkout — antes do pagamento — para não vender o último item duas vezes. */
@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 14)
    private String sku;

    @Column(nullable = false)
    private int qtd;

    @Column(name = "pedido_id", nullable = false)
    private UUID pedidoId;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusReserva status;

    protected Reserva() {
        // exigido pelo JPA
    }

    public Reserva(String sku, int qtd, UUID pedidoId, Instant expiraEm) {
        if (qtd <= 0) {
            throw new IllegalArgumentException("qtd deve ser positiva: " + qtd);
        }
        this.sku = Sku.validar(sku);
        this.qtd = qtd;
        this.pedidoId = Objects.requireNonNull(pedidoId, "pedidoId");
        this.expiraEm = Objects.requireNonNull(expiraEm, "expiraEm");
        this.status = StatusReserva.ATIVA;
    }

    public UUID getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public int getQtd() {
        return qtd;
    }

    public UUID getPedidoId() {
        return pedidoId;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public StatusReserva getStatus() {
        return status;
    }
}
