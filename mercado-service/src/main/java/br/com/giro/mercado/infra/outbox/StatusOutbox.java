package br.com.giro.mercado.infra.outbox;

public enum StatusOutbox {
    PENDING,
    SENT,
    /** O destino rejeitou o evento (4xx definitivo): fica para análise, sem reenvio. */
    FAILED
}
