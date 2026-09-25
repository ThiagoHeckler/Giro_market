package br.com.giro.mercado.application.contrato;

import java.time.Instant;
import java.util.UUID;

/** Eventos trocados entre mercado e estoque. Selada: o compilador conhece todos os casos. */
public sealed interface EventoIntegracao permits ReposicaoSolicitada, ReposicaoEnviada, ReposicaoNegada,
        ProdutoClassificado {

    UUID eventId();

    Instant ocorridoEm();
}
