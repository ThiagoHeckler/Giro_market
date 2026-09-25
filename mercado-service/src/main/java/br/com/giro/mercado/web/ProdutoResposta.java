package br.com.giro.mercado.web;

import br.com.giro.mercado.domain.ProdutoVitrine;
import br.com.giro.mercado.domain.StatusVitrine;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param disponivel unidades na prateleira (limita a quantidade no carrinho)
 * @param categoria  {@code null} enquanto não classificado
 */
public record ProdutoResposta(String sku, String nome, BigDecimal preco, StatusVitrine status, int disponivel,
                              String categoria, List<String> tags) {

    static ProdutoResposta de(ProdutoVitrine p) {
        return new ProdutoResposta(p.getSku(), p.getNome(), p.getPreco(), p.getStatus(), p.getEstoquePrateleira(),
                p.getCategoria().orElse(null), p.getTags().orElse(List.of()));
    }
}
