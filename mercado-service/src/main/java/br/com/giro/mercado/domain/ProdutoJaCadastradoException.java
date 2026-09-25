package br.com.giro.mercado.domain;

public class ProdutoJaCadastradoException extends RuntimeException {

    public ProdutoJaCadastradoException(String sku) {
        super("Produto já cadastrado na vitrine: " + sku);
    }
}
