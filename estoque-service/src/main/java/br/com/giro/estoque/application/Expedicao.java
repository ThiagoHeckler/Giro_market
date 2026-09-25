package br.com.giro.estoque.application;

import br.com.giro.estoque.application.contrato.ReposicaoEnviada;
import br.com.giro.estoque.domain.ProdutoEstoque;
import br.com.giro.estoque.infra.outbox.Outbox;
import br.com.giro.estoque.infra.persistencia.LoteRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Transfere unidades de um lote para a prateleira do mercado e registra a {@link ReposicaoEnviada}.
 * Um envio sai de um lote só (FEFO), para cada crédito na prateleira ter rastreio de lote;
 * se o lote não cobre o pedido, o mercado pede o resto no próximo gatilho.
 */
@Component
public class Expedicao {

    private final LoteRepository lotes;
    private final Outbox outbox;
    private final Clock relogio;

    public Expedicao(LoteRepository lotes, Outbox outbox, Clock relogio) {
        this.lotes = lotes;
        this.outbox = outbox;
        this.relogio = relogio;
    }

    /**
     * Exige a linha do produto travada pela transação corrente.
     *
     * @return o evento registrado, ou vazio se não há lote expedível (sem saldo ou tudo vencido)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<ReposicaoEnviada> expedirDoProximoLote(ProdutoEstoque produto, UUID correlationId, int qtdPedida) {
        return lotes.proximoParaExpedir(produto.getSku(), LocalDate.now(relogio)).map(lote -> {
            int qtd = Math.min(qtdPedida, lote.getQuantidadeDisponivel());
            produto.expedir(lote, qtd);
            var evento = new ReposicaoEnviada(UUID.randomUUID(), correlationId, produto.getSku(), qtd,
                    lote.getCodigoLote(), relogio.instant());
            outbox.registrar(evento);
            return evento;
        });
    }
}
