package br.com.giro.estoque.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Objects;
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

    public void classificar(List<String> tags) {
        this.tags = List.copyOf(Objects.requireNonNull(tags, "tags"));
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

    public int getSaldoDisponivel() {
        return saldoDisponivel;
    }
}
