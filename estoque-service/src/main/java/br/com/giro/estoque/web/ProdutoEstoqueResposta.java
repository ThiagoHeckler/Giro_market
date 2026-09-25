package br.com.giro.estoque.web;

import br.com.giro.estoque.domain.ProdutoEstoque;

import java.util.List;

/** {@code tags} e {@code categoria} nulos enquanto o produto não foi classificado. */
public record ProdutoEstoqueResposta(String sku, String descricao, String ncm, List<String> tags,
                                     String categoria, int saldoDisponivel) {

    static ProdutoEstoqueResposta de(ProdutoEstoque produto) {
        return new ProdutoEstoqueResposta(produto.getSku(), produto.getDescricao(), produto.getNcm(),
                produto.getTags().orElse(null), produto.getCategoria().orElse(null), produto.getSaldoDisponivel());
    }
}
