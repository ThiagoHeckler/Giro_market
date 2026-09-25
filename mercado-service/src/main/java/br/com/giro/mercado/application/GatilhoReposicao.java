package br.com.giro.mercado.application;

import br.com.giro.mercado.application.contrato.ReposicaoSolicitada;
import br.com.giro.mercado.domain.ProdutoVitrine;
import br.com.giro.mercado.domain.SolicitacaoReposicao;
import br.com.giro.mercado.domain.StatusSolicitacao;
import br.com.giro.mercado.infra.outbox.Outbox;
import br.com.giro.mercado.infra.persistencia.SolicitacaoReposicaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Ponto de reposição (min/max). Roda na mesma transação que alterou a prateleira, com a linha
 * do produto já travada — por isso a checagem de solicitação pendente não tem corrida.
 */
@Component
public class GatilhoReposicao {

    private static final Logger log = LoggerFactory.getLogger(GatilhoReposicao.class);

    private final SolicitacaoReposicaoRepository solicitacoes;
    private final Outbox outbox;
    private final Clock relogio;

    public GatilhoReposicao(SolicitacaoReposicaoRepository solicitacoes, Outbox outbox, Clock relogio) {
        this.solicitacoes = solicitacoes;
        this.outbox = outbox;
        this.relogio = relogio;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void avaliar(ProdutoVitrine produto) {
        boolean temPendente = solicitacoes.existsBySkuAndStatusIn(produto.getSku(), StatusSolicitacao.EM_ABERTO);

        produto.quantidadeARepor(temPendente).ifPresent(qtd -> {
            var evento = new ReposicaoSolicitada(UUID.randomUUID(), produto.getSku(), qtd, relogio.instant());
            solicitacoes.save(new SolicitacaoReposicao(evento.eventId(), evento.sku(), qtd, evento.ocorridoEm()));
            outbox.registrar(evento);
            log.info("Reposição solicitada sku={} qtd={} prateleira={} minimo={} eventId={}",
                    produto.getSku(), qtd, produto.getEstoquePrateleira(), produto.getEstoqueMinimo(), evento.eventId());
        });
    }
}
