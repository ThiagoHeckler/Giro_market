package br.com.giro.mercado.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PedidoFechado(UUID pedidoId, BigDecimal total, Instant reservaExpiraEm) {
}
