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

    @Column(name = "quantidade_disponivel", nullable = false)
    private int quantidadeDisponivel;

    private LocalDate validade;

    @Column(name = "recebido_em", nullable = false)
    private Instant recebidoEm;

    protected Lote() {
        // exigido pelo JPA
    }

    /** Criado só por {@link ProdutoEstoque#receberLote}, que mantém o saldo do produto em dia. */
    Lote(String sku, String codigoLote, int quantidade, LocalDate validade, Instant recebidoEm) {
        if (codigoLote == null || codigoLote.isBlank()) {
            throw new IllegalArgumentException("codigoLote é obrigatório");
        }
        if (quantidade <= 0) {
            throw new IllegalArgumentException("quantidade do lote deve ser positiva: " + quantidade);
        }
        this.sku = Sku.validar(sku);
        this.codigoLote = codigoLote;
        this.quantidade = quantidade;
        this.quantidadeDisponivel = quantidade;
        this.validade = validade;
        this.recebidoEm = Objects.requireNonNull(recebidoEm, "recebidoEm");
    }

    void retirar(int qtd) {
        if (qtd <= 0 || qtd > quantidadeDisponivel) {
            throw new IllegalArgumentException(
                    "retirada inválida do lote %s: %d de %d disponíveis".formatted(codigoLote, qtd, quantidadeDisponivel));
        }
        quantidadeDisponivel -= qtd;
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

    public int getQuantidadeDisponivel() {
        return quantidadeDisponivel;
    }

    public Optional<LocalDate> getValidade() {
        return Optional.ofNullable(validade);
    }

    public Instant getRecebidoEm() {
        return recebidoEm;
    }
}
