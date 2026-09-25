package br.com.giro.mercado.domain;

/** O SKU é a identidade canônica entre os sistemas: a vitrine não expõe o que o estoque não conhece. */
public class SkuDesconhecidoNoEstoqueException extends RuntimeException {

    public SkuDesconhecidoNoEstoqueException(String sku) {
        super("SKU desconhecido no estoque: " + sku);
    }
}
