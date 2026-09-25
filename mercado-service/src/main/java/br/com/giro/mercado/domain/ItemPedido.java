package br.com.giro.mercado.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "item_pedido")
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 14)
    private String sku;

    @Column(nullable = false)
    private int qtd;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    protected ItemPedido() {
        // exigido pelo JPA
    }

    ItemPedido(String sku, int qtd, BigDecimal precoUnitario) {
        if (qtd <= 0) {
            throw new IllegalArgumentException("qtd deve ser positiva: " + qtd);
        }
        this.sku = Sku.validar(sku);
        this.qtd = qtd;
        this.precoUnitario = precoUnitario;
    }

    BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(qtd));
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public int getQtd() {
        return qtd;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }
}
