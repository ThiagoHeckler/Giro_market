package br.com.giro.estoque.application;

import br.com.giro.estoque.application.contrato.ProdutoClassificado;
import br.com.giro.estoque.domain.ProdutoEstoque;
import br.com.giro.estoque.infra.outbox.Outbox;
import br.com.giro.estoque.infra.tags.ClassificadorTags.Classificacao;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Aplica a classificação do tag-worker ao produto e avisa o mercado ({@link ProdutoClassificado}),
 * na mesma transação — tags gravadas no estoque sempre chegam à vitrine.
 */
@Component
public class ClassificacaoDeProduto {

    private final Outbox outbox;
    private final Clock relogio;

    public ClassificacaoDeProduto(Outbox outbox, Clock relogio) {
        this.outbox = outbox;
        this.relogio = relogio;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void aplicar(ProdutoEstoque produto, Classificacao classificacao) {
        produto.classificar(classificacao.tags(), classificacao.categoria());
        outbox.registrar(new ProdutoClassificado(UUID.randomUUID(), produto.getSku(),
                List.copyOf(classificacao.tags()), classificacao.categoria(), relogio.instant()));
    }
}
