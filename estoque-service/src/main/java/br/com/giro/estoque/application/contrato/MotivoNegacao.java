package br.com.giro.estoque.application.contrato;

/** Motivos de {@link ReposicaoNegada}. Serializado como texto no JSON do evento. */
public enum MotivoNegacao {
    SEM_SALDO,
    SKU_DESCONHECIDO
}
