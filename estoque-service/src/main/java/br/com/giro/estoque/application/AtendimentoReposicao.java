package br.com.giro.estoque.application;

import br.com.giro.estoque.application.contrato.EventoIntegracao;
import br.com.giro.estoque.application.contrato.MotivoNegacao;
import br.com.giro.estoque.application.contrato.ProdutoClassificado;
import br.com.giro.estoque.application.contrato.ReposicaoEnviada;
import br.com.giro.estoque.application.contrato.ReposicaoNegada;
import br.com.giro.estoque.application.contrato.ReposicaoSolicitada;
import br.com.giro.estoque.domain.DemandaReprimida;
import br.com.giro.estoque.infra.inbox.Inbox;
import br.com.giro.estoque.infra.outbox.Outbox;
import br.com.giro.estoque.infra.persistencia.DemandaReprimidaRepository;
import br.com.giro.estoque.infra.persistencia.ProdutoEstoqueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Atende pedidos de reposição do mercado. Tudo determinístico: expede do lote FEFO até a quantidade
 * pedida; sem saldo, nega e registra a demanda reprimida — atendida depois pela {@link EntradaDeLote}. Inbox, baixa de saldo e resposta na outbox
 * commitam juntos.
 */
@Service
public class AtendimentoReposicao {

    private static final Logger log = LoggerFactory.getLogger(AtendimentoReposicao.class);

    private final Inbox inbox;
    private final Outbox outbox;
    private final ProdutoEstoqueRepository produtos;
    private final Expedicao expedicao;
    private final DemandaReprimidaRepository demandas;
    private final Clock relogio;

    public AtendimentoReposicao(Inbox inbox, Outbox outbox, ProdutoEstoqueRepository produtos,
                                Expedicao expedicao, DemandaReprimidaRepository demandas, Clock relogio) {
        this.inbox = inbox;
        this.outbox = outbox;
        this.produtos = produtos;
        this.expedicao = expedicao;
        this.demandas = demandas;
        this.relogio = relogio;
    }

    @Transactional
    public void processar(EventoIntegracao evento) {
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
            case ProdutoClassificado classificado ->
                    throw new IllegalArgumentException("estoque não consome ProdutoClassificado: " + classificado.eventId());
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

        var envio = expedicao.expedirDoProximoLote(produto, pedido.eventId(), pedido.qtdFaltante());
        if (envio.isPresent()) {
            log.info("Reposição enviada sku={} qtd={}/{} lote={} saldoRestante={} solicitacao={}",
                    pedido.sku(), envio.get().qtd(), pedido.qtdFaltante(), envio.get().lote(),
                    produto.getSaldoDisponivel(), pedido.eventId());
            return;
        }

        demandas.save(new DemandaReprimida(pedido.sku(), pedido.qtdFaltante(), pedido.eventId(), agora));
        outbox.registrar(new ReposicaoNegada(UUID.randomUUID(), pedido.eventId(), pedido.sku(),
                MotivoNegacao.SEM_SALDO, agora));
        log.warn("Reposição negada: sem saldo sku={} qtd={} solicitacao={} — demanda reprimida registrada",
                pedido.sku(), pedido.qtdFaltante(), pedido.eventId());
    }
}
