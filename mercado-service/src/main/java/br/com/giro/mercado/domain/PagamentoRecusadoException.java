package br.com.giro.mercado.domain;

import java.util.UUID;

/** O pedido não aceita mais pagamento: a reserva expirou (ou o pedido saiu de "aguardando pagamento"). */
public class PagamentoRecusadoException extends RuntimeException {

    public PagamentoRecusadoException(UUID pedidoId, StatusPedido status) {
        super(status == StatusPedido.EXPIRADO
                ? "O prazo da reserva do pedido %s terminou; os itens voltaram para a vitrine".formatted(pedidoId)
                : "O pedido %s não aceita pagamento: está %s".formatted(pedidoId, status));
    }

    public static PagamentoRecusadoException prazoVencido(UUID pedidoId) {
        return new PagamentoRecusadoException(pedidoId, StatusPedido.EXPIRADO);
    }
}
