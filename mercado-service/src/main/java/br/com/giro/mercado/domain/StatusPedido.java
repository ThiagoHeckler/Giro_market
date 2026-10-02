package br.com.giro.mercado.domain;

public enum StatusPedido {
    AGUARDANDO_PAGAMENTO,
    PAGO,
    CANCELADO,
    /** O prazo da reserva acabou sem pagamento; as unidades voltaram para a prateleira. */
    EXPIRADO
}
