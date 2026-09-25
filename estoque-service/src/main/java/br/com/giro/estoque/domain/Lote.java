package br.com.giro.estoque.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/** Lote recebido no almoxarifado. Referencia o produto pelo SKU, não por associação JPA. */
@Entity
@Table(name = "lote")
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 14)
    private String sku;

    @Column(name = "codigo_lote", nullable = false, length = 50)
    private String codigoLote;

    @Column(nullable = false)
    private int quantidade;

    private LocalDate validade;

    @Column(name = "recebido_em", nullable = false)
    private Instant recebidoEm;

    protected Lote() {
        // exigido pelo JPA
    }

    public Lote(String sku, String codigoLote, int quantidade, LocalDate validade, Instant recebidoEm) {
        if (codigoLote == null || codigoLote.isBlank()) {
            throw new IllegalArgumentException("codigoLote é obrigatório");
        }
        if (quantidade <= 0) {
            throw new IllegalArgumentException("quantidade do lote deve ser positiva: " + quantidade);
        }
        this.sku = Sku.validar(sku);
        this.codigoLote = codigoLote;
        this.quantidade = quantidade;
        this.validade = validade;
        this.recebidoEm = Objects.requireNonNull(recebidoEm, "recebidoEm");
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getCodigoLote() {
        return codigoLote;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public Optional<LocalDate> getValidade() {
        return Optional.ofNullable(validade);
    }

    public Instant getRecebidoEm() {
        return recebidoEm;
    }
}
