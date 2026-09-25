package br.com.giro.mercado.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Agregado Pedido; os itens só existem dentro dele. */
@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private StatusPedido status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "pedido_id", nullable = false)
    private List<ItemPedido> itens = new ArrayList<>();

    protected Pedido() {
        // exigido pelo JPA
    }

    public Pedido(Instant criadoEm) {
        this.criadoEm = Objects.requireNonNull(criadoEm, "criadoEm");
        this.status = StatusPedido.AGUARDANDO_PAGAMENTO;
        this.total = BigDecimal.ZERO;
    }

    public void adicionarItem(String sku, int qtd, BigDecimal precoUnitario) {
        if (itens.stream().anyMatch(i -> i.getSku().equals(sku))) {
            throw new IllegalArgumentException("SKU repetido no pedido: " + sku);
        }
        var item = new ItemPedido(sku, qtd, precoUnitario);
        itens.add(item);
        total = total.add(item.subtotal());
    }

    public UUID getId() {
        return id;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public List<ItemPedido> getItens() {
        return List.copyOf(itens);
    }
}
