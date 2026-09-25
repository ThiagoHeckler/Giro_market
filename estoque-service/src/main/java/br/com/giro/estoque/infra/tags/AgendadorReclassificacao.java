package br.com.giro.estoque.infra.tags;

import br.com.giro.estoque.application.ReclassificacaoTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBooleanProperty(name = "tags.reclassificacao-habilitada", matchIfMissing = true)
public class AgendadorReclassificacao {

    private static final Logger log = LoggerFactory.getLogger(AgendadorReclassificacao.class);

    private final ReclassificacaoTags reclassificacao;

    public AgendadorReclassificacao(ReclassificacaoTags reclassificacao) {
        this.reclassificacao = reclassificacao;
    }

    @Scheduled(fixedDelayString = "${tags.intervalo-reclassificacao}",
            initialDelayString = "${tags.intervalo-reclassificacao}")
    void reclassificar() {
        try {
            reclassificacao.reclassificarPendentes();
        } catch (RuntimeException e) {
            log.error("Rodada de reclassificação de tags falhou", e);
        }
    }
}
