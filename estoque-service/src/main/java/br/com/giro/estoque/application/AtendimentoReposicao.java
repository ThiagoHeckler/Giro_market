package br.com.giro.estoque.application;

import br.com.giro.estoque.application.contrato.EventoReposicao;
import br.com.giro.estoque.application.contrato.MotivoNegacao;
import br.com.giro.estoque.application.contrato.ReposicaoEnviada;
import br.com.giro.estoque.application.contrato.ReposicaoNegada;
import br.com.giro.estoque.application.contrato.ReposicaoSolicitada;
import br.com.giro.estoque.domain.DemandaReprimida;
import br.com.giro.estoque.infra.inbox.Inbox;
import br.com.giro.estoque.infra.outbox.Outbox;
import br.com.giro.estoque.infra.persistencia.DemandaReprimidaRepository;
import br.com.giro.estoque.infra.persistencia.LoteRepository;
import br.com.giro.estoque.infra.persistencia.ProdutoEstoqueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Atende pedidos de reposição do mercado. Tudo determinístico: expede do lote FEFO até a quantidade
 * pedida; sem saldo, nega e registra a demanda reprimida. Inbox, baixa de saldo e resposta na outbox
 * commitam juntos.
 */
@Service
public class AtendimentoReposicao {

    private static final Logger log = LoggerFactory.getLogger(AtendimentoReposicao.class);

    private final Inbox inbox;
    private final Outbox outbox;
    private final ProdutoEstoqueRepository produtos;
    private final LoteRepository lotes;
    private final DemandaReprimidaRepository demandas;
    private final Clock relogio;

    public AtendimentoReposicao(Inbox inbox, Outbox outbox, ProdutoEstoqueRepository produtos,
                                LoteRepository lotes, DemandaReprimidaRepository demandas, Clock relogio) {
        this.inbox = inbox;
        this.outbox = outbox;
        this.produtos = produtos;
        this.lotes = lotes;
        this.demandas = demandas;
        this.relogio = relogio;
    }

    @Transactional
    public void processar(EventoReposicao evento) {
        if (!inbox.registrarSeNovo(evento.eventId())) {
            log.info("Evento duplicado ignorado eventId={} tipo={}", evento.eventId(), evento.getClass().getSimpleName());
            return;
        }
        switch (evento) {
            case ReposicaoSolicitada solicitada -> atender(solicitada);
            case ReposicaoEnviada enviada ->
                    throw new IllegalArgumentException("estoque não consome ReposicaoEnviada: " + enviada.eventId());
            case ReposicaoNegada negada ->
                    throw new IllegalArgumentException("estoque não consome ReposicaoNegada: " + negada.eventId());
        }
    }

    private void atender(ReposicaoSolicitada pedido) {
        var agora = relogio.instant();
        var produto = produtos.buscarParaAtualizar(pedido.sku()).orElse(null);
        if (produto == null) {
            outbox.registrar(new ReposicaoNegada(UUID.randomUUID(), pedido.eventId(), pedido.sku(),
                    MotivoNegacao.SKU_DESCONHECIDO, agora));
            log.warn("Reposição negada: SKU desconhecido sku={} solicitacao={}", pedido.sku(), pedido.eventId());
            return;
        }

        var lote = lotes.proximoParaExpedir(pedido.sku(), LocalDate.now(relogio)).orElse(null);
        if (lote == null) {
            demandas.save(new DemandaReprimida(pedido.sku(), pedido.qtdFaltante(), pedido.eventId(), agora));
            outbox.registrar(new ReposicaoNegada(UUID.randomUUID(), pedido.eventId(), pedido.sku(),
                    MotivoNegacao.SEM_SALDO, agora));
            log.warn("Reposição negada: sem saldo sku={} qtd={} solicitacao={} — demanda reprimida registrada",
                    pedido.sku(), pedido.qtdFaltante(), pedido.eventId());
            return;
        }

        // Um envio sai de um lote só (rastreabilidade); se faltar, o mercado pede o resto no próximo gatilho.
        int qtd = Math.min(pedido.qtdFaltante(), lote.getQuantidadeDisponivel());
        produto.expedir(lote, qtd);
        outbox.registrar(new ReposicaoEnviada(UUID.randomUUID(), pedido.eventId(), pedido.sku(), qtd,
                lote.getCodigoLote(), agora));
        log.info("Reposição enviada sku={} qtd={}/{} lote={} saldoRestante={} solicitacao={}",
                pedido.sku(), qtd, pedido.qtdFaltante(), lote.getCodigoLote(), produto.getSaldoDisponivel(),
                pedido.eventId());
    }
}
