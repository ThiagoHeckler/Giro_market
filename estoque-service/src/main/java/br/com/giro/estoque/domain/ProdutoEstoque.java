package br.com.giro.estoque.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Produto do almoxarifado, identificado pelo SKU (EAN/GTIN).
 * Tags servem só para posicionamento na vitrine — nunca são identidade.
 */
@Entity
@Table(name = "produto_estoque")
public class ProdutoEstoque {

    @Id
    @Column(length = 14)
    private String sku;

    @Column(nullable = false)
    private String descricao;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 8)
    private String ncm;

    /** {@code null} enquanto o tag-worker não classificou o produto. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> tags;

    /** Categoria sugerida pelo tag-worker; {@code null} enquanto não classificado. */
    @Column(length = 30)
    private String categoria;

    @Column(name = "saldo_disponivel", nullable = false)
    private int saldoDisponivel;

    @Version
    private Long versao;

    protected ProdutoEstoque() {
        // exigido pelo JPA
    }

    public ProdutoEstoque(String sku, String descricao, String ncm) {
        this.sku = Sku.validar(sku);
        this.descricao = exigirTexto(descricao, "descricao");
        this.ncm = validarNcm(ncm);
        this.saldoDisponivel = 0;
    }

    /** Entrada de lote: o saldo do produto acompanha a soma dos lotes. */
    public Lote receberLote(String codigoLote, int quantidade, LocalDate validade, Instant recebidoEm) {
        var lote = new Lote(sku, codigoLote, quantidade, validade, recebidoEm);
        saldoDisponivel += quantidade;
        return lote;
    }

    /** Tira unidades de um lote deste produto para transferir à prateleira. */
    public void expedir(Lote lote, int qtd) {
        if (!sku.equals(lote.getSku())) {
            throw new IllegalArgumentException("lote %s não pertence ao SKU %s".formatted(lote.getCodigoLote(), sku));
        }
        if (qtd > saldoDisponivel) {
            throw new IllegalStateException(
                    "saldo do SKU %s (%d) menor que a expedição (%d)".formatted(sku, saldoDisponivel, qtd));
        }
        lote.retirar(qtd);
        saldoDisponivel -= qtd;
    }

    /** Aplica a classificação do tag-worker. Só afeta posicionamento — nunca identidade nem reposição. */
    public void classificar(List<String> tags, String categoria) {
        if (tags == null || tags.isEmpty()) {
            throw new IllegalArgumentException("classificação sem tags");
        }
        this.tags = List.copyOf(tags);
        this.categoria = exigirTexto(categoria, "categoria");
    }

    private static String validarNcm(String ncm) {
        if (ncm == null || !ncm.matches("^[0-9]{8}$")) {
            throw new IllegalArgumentException("NCM inválido (esperado 8 dígitos): " + ncm);
        }
        return ncm;
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório");
        }
        return valor;
    }

    public String getSku() {
        return sku;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getNcm() {
        return ncm;
    }

    public Optional<List<String>> getTags() {
        return Optional.ofNullable(tags).map(List::copyOf);
    }

    public Optional<String> getCategoria() {
        return Optional.ofNullable(categoria);
    }

    public int getSaldoDisponivel() {
        return saldoDisponivel;
    }
}
