package br.com.giro.mercado.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Produto na prateleira da vitrine, com a política de reposição min/max.
 * {@code estoqueMinimo} é o gatilho; {@code estoqueIdeal} é o alvo da reposição.
 */
@Entity
@Table(name = "produto_vitrine")
public class ProdutoVitrine {

    @Id
    @Column(length = 14)
    private String sku;

    @Column(nullable = false)
    private String nome;

    /** Só posicionamento na vitrine; {@code null} enquanto não classificado. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> tags;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "estoque_prateleira", nullable = false)
    private int estoquePrateleira;

    @Column(name = "estoque_minimo", nullable = false)
    private int estoqueMinimo;

    @Column(name = "estoque_ideal", nullable = false)
    private int estoqueIdeal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusVitrine status;

    @Version
    private Long versao;

    protected ProdutoVitrine() {
        // exigido pelo JPA
    }

    public ProdutoVitrine(String sku, String nome, BigDecimal preco,
                          int estoqueInicial, int estoqueMinimo, int estoqueIdeal) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome é obrigatório");
        }
        if (preco == null || preco.signum() <= 0) {
            throw new IllegalArgumentException("preço deve ser positivo: " + preco);
        }
        if (estoqueInicial < 0) {
            throw new IllegalArgumentException("estoque inicial não pode ser negativo: " + estoqueInicial);
        }
        if (estoqueMinimo < 1 || estoqueIdeal < estoqueMinimo) {
            throw new IllegalArgumentException(
                    "exigido 1 <= estoqueMinimo <= estoqueIdeal; recebido min=%d ideal=%d"
                            .formatted(estoqueMinimo, estoqueIdeal));
        }
        this.sku = Sku.validar(sku);
        this.nome = nome;
        this.preco = preco;
        this.estoquePrateleira = estoqueInicial;
        this.estoqueMinimo = estoqueMinimo;
        this.estoqueIdeal = estoqueIdeal;
        this.status = estoqueInicial > 0 ? StatusVitrine.DISPONIVEL : StatusVitrine.ESGOTADO;
    }

    /** Tira unidades da prateleira no checkout. Zerou, esgotou. */
    public void debitar(int qtd) {
        exigirPositivo(qtd);
        if (qtd > estoquePrateleira) {
            throw new EstoqueInsuficienteException(sku, qtd, estoquePrateleira);
        }
        estoquePrateleira -= qtd;
        if (estoquePrateleira == 0) {
            status = StatusVitrine.ESGOTADO;
        }
    }

    /** Recebe unidades transferidas pelo estoque. */
    public void creditar(int qtd) {
        exigirPositivo(qtd);
        estoquePrateleira += qtd;
        status = StatusVitrine.DISPONIVEL;
    }

    /**
     * Estoque sem saldo para repor. Se a prateleira já está vazia, o produto fica ESGOTADO;
     * se ainda restam unidades, elas continuam à venda até zerar.
     */
    public void registrarReposicaoNegada() {
        if (estoquePrateleira == 0) {
            status = StatusVitrine.ESGOTADO;
        }
    }

    /**
     * Regra de gatilho do ponto de reposição — aritmética pura, sem heurística:
     * abaixo do mínimo e sem solicitação em aberto, pede a diferença até o ideal.
     */
    public OptionalInt quantidadeARepor(boolean temSolicitacaoPendente) {
        if (estoquePrateleira < estoqueMinimo && !temSolicitacaoPendente) {
            return OptionalInt.of(estoqueIdeal - estoquePrateleira);
        }
        return OptionalInt.empty();
    }

    public void classificar(List<String> tags) {
        this.tags = List.copyOf(Objects.requireNonNull(tags, "tags"));
    }

    private static void exigirPositivo(int qtd) {
        if (qtd <= 0) {
            throw new IllegalArgumentException("quantidade deve ser positiva: " + qtd);
        }
    }

    public String getSku() {
        return sku;
    }

    public String getNome() {
        return nome;
    }

    public Optional<List<String>> getTags() {
        return Optional.ofNullable(tags).map(List::copyOf);
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public int getEstoquePrateleira() {
        return estoquePrateleira;
    }

    public int getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public int getEstoqueIdeal() {
        return estoqueIdeal;
    }

    public StatusVitrine getStatus() {
        return status;
    }
}
