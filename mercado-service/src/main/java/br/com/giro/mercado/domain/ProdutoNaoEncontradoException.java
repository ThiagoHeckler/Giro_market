package br.com.giro.mercado.domain;

public class ProdutoNaoEncontradoException extends RuntimeException {

    public ProdutoNaoEncontradoException(String sku) {
        super("Produto não encontrado na vitrine: " + sku);
    }
}
