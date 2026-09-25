package br.com.giro.mercado.application;

import br.com.giro.mercado.application.contrato.EventoIntegracao;
import br.com.giro.mercado.application.contrato.MotivoNegacao;
import br.com.giro.mercado.application.contrato.ProdutoClassificado;
import br.com.giro.mercado.application.contrato.ReposicaoEnviada;
import br.com.giro.mercado.application.contrato.ReposicaoNegada;
import br.com.giro.mercado.application.contrato.ReposicaoSolicitada;
import br.com.giro.mercado.domain.ProdutoVitrine;
import br.com.giro.mercado.domain.SolicitacaoReposicao;
import br.com.giro.mercado.domain.StatusSolicitacao;
import br.com.giro.mercado.infra.inbox.Inbox;
import br.com.giro.mercado.infra.persistencia.ProdutoVitrineRepository;
import br.com.giro.mercado.infra.persistencia.SolicitacaoReposicaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Consome as respostas do estoque. Idempotente: a inbox descarta duplicatas, e o registro
 * na inbox commita junto com o efeito — ou nenhum dos dois acontece.
 */
@Service
public class ConsumidorReposicao {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorReposicao.class);

    private final Inbox inbox;
    private final ProdutoVitrineRepository produtos;
    private final SolicitacaoReposicaoRepository solicitacoes;
    private final GatilhoReposicao gatilho;

    public ConsumidorReposicao(Inbox inbox, ProdutoVitrineRepository produtos,
                               SolicitacaoReposicaoRepository solicitacoes, GatilhoReposicao gatilho) {
        this.inbox = inbox;
        this.produtos = produtos;
        this.solicitacoes = solicitacoes;
        this.gatilho = gatilho;
    }

    @Transactional
    public void processar(EventoIntegracao evento) {
        if (!inbox.registrarSeNovo(evento.eventId())) {
            log.info("Evento duplicado ignorado eventId={} tipo={}", evento.eventId(), evento.getClass().getSimpleName());
            return;
        }
        switch (evento) {
            case ReposicaoEnviada enviada -> aplicar(enviada);
            case ReposicaoNegada negada -> aplicar(negada);
            case ReposicaoSolicitada solicitada ->
                    throw new IllegalArgumentException("mercado não consome ReposicaoSolicitada: " + solicitada.eventId());
            case ProdutoClassificado classificado ->
                    throw new IllegalArgumentException("ProdutoClassificado é do ConsumidorClassificacao: "
                            + classificado.eventId());
        }
    }

    private void aplicar(ReposicaoEnviada evento) {
        var produto = produtos.buscarParaAtualizar(evento.sku()).orElse(null);
        if (produto == null) {
            log.warn("ReposicaoEnviada para SKU fora da vitrine sku={} eventId={}", evento.sku(), evento.eventId());
            return;
        }
        // A mercadoria chegou: credita mesmo que a solicitação não esteja mais em aberto.
        produto.creditar(evento.qtd());
        solicitacao(evento.correlationId()).ifPresent(s -> {
            if (s.getStatus().emAberto()) {
                s.atender();
            } else {
                log.warn("ReposicaoEnviada para solicitação já encerrada correlationId={} status={}",
                        s.getEventId(), s.getStatus());
            }
        });
        log.info("Reposição recebida sku={} qtd={} lote={} prateleira={}",
                evento.sku(), evento.qtd(), evento.lote(), produto.getEstoquePrateleira());

        // Se houve vendas enquanto o lote vinha, a prateleira pode seguir abaixo do mínimo.
        gatilho.avaliar(produto);
    }

    private void aplicar(ReposicaoNegada evento) {
        var solicitacao = solicitacao(evento.correlationId());
        solicitacao.ifPresent(s -> {
            if (s.getStatus() != StatusSolicitacao.PENDENTE) {
                log.warn("ReposicaoNegada para solicitação fora de PENDENTE correlationId={} status={}",
                        s.getEventId(), s.getStatus());
                return;
            }
            switch (evento.motivo()) {
                // O estoque guardou a demanda reprimida e repõe sozinho na entrada de lote.
                case SEM_SALDO -> s.aguardarLote();
                case SKU_DESCONHECIDO -> s.cancelar();
            }
        });
        produtos.buscarParaAtualizar(evento.sku()).ifPresent(ProdutoVitrine::registrarReposicaoNegada);
        log.warn("Reposição negada sku={} motivo={} correlationId={}", evento.sku(), evento.motivo(), evento.correlationId());
        if (evento.motivo() == MotivoNegacao.SKU_DESCONHECIDO) {
            log.error("SKU da vitrine desconhecido pelo estoque — cadastro divergente sku={}", evento.sku());
        }
    }

    private Optional<SolicitacaoReposicao> solicitacao(UUID correlationId) {
        var solicitacao = solicitacoes.findByEventId(correlationId);
        if (solicitacao.isEmpty()) {
            log.warn("Resposta do estoque sem solicitação correspondente correlationId={}", correlationId);
        }
        return solicitacao;
    }
}
