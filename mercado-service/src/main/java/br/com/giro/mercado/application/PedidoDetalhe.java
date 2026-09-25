package br.com.giro.mercado.application;

import br.com.giro.mercado.domain.StatusPedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PedidoDetalhe(UUID id, StatusPedido status, BigDecimal total, Instant criadoEm,
                            Instant reservaExpiraEm, List<Item> itens) {

    public record Item(String sku, String nome, int qtd, BigDecimal precoUnitario) {
    }
}
