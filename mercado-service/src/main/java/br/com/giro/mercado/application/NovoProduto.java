package br.com.giro.mercado.application;

import java.math.BigDecimal;

public record NovoProduto(String sku, String nome, BigDecimal preco, int estoqueMinimo, int estoqueIdeal) {
}
