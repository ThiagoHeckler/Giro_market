package br.com.giro.mercado.domain;

import java.util.Set;

public enum StatusSolicitacao {
    /** Enviada ao estoque, aguardando resposta. */
    PENDENTE,
    /** Negada por falta de saldo; o estoque atende sozinho quando entrar lote novo. */
    AGUARDANDO_LOTE,
    ATENDIDA,
    /** Negada sem perspectiva de atendimento (ex.: SKU desconhecido no estoque). */
    CANCELADA;

    /** Status que contam como "tem solicitação pendente" para o gatilho. */
    public static final Set<StatusSolicitacao> EM_ABERTO = Set.of(PENDENTE, AGUARDANDO_LOTE);

    public boolean emAberto() {
        return EM_ABERTO.contains(this);
    }
}
