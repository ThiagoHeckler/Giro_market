package br.com.giro.mercado.application;

import br.com.giro.mercado.application.contrato.ProdutoClassificado;
import br.com.giro.mercado.infra.inbox.Inbox;
import br.com.giro.mercado.infra.persistencia.ProdutoVitrineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aplica na vitrine a classificação feita no estoque. Tags só posicionam o produto: nada aqui
 * toca prateleira, status ou reposição.
 */
@Service
public class ConsumidorClassificacao {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorClassificacao.class);

    private final Inbox inbox;
    private final ProdutoVitrineRepository produtos;

    public ConsumidorClassificacao(Inbox inbox, ProdutoVitrineRepository produtos) {
        this.inbox = inbox;
        this.produtos = produtos;
    }

    @Transactional
    public void processar(ProdutoClassificado evento) {
        if (!inbox.registrarSeNovo(evento.eventId())) {
            log.info("Evento duplicado ignorado eventId={} tipo=ProdutoClassificado", evento.eventId());
            return;
        }
        var produto = produtos.buscarParaAtualizar(evento.sku()).orElse(null);
        if (produto == null) {
            log.warn("ProdutoClassificado para SKU fora da vitrine sku={} eventId={}", evento.sku(), evento.eventId());
            return;
        }
        if (produto.classificar(evento.tags(), evento.categoria(), evento.ocorridoEm())) {
            log.info("Produto classificado na vitrine sku={} categoria={} tags={}",
                    evento.sku(), evento.categoria(), evento.tags());
        } else {
            log.info("Classificação mais antiga que a atual ignorada sku={} eventId={}", evento.sku(), evento.eventId());
        }
    }
}
