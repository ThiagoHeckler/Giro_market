package br.com.giro.estoque.infra.tags;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * @param workerUrl                  URL base do tag-worker
 * @param timeout                    timeout de conexão e leitura; estourou, segue sem tags
 * @param reclassificacaoHabilitada  liga o job que reclassifica produtos que ficaram sem tags
 * @param intervaloReclassificacao   pausa entre rodadas do job
 * @param loteReclassificacao        produtos por rodada
 */
@ConfigurationProperties("tags")
public record TagsProperties(
        URI workerUrl,
        Duration timeout,
        boolean reclassificacaoHabilitada,
        Duration intervaloReclassificacao,
        int loteReclassificacao) {

    public TagsProperties {
        Objects.requireNonNull(workerUrl, "tags.worker-url é obrigatório");
        Objects.requireNonNull(timeout, "tags.timeout é obrigatório");
        Objects.requireNonNull(intervaloReclassificacao, "tags.intervalo-reclassificacao é obrigatório");
        if (loteReclassificacao <= 0) {
            throw new IllegalArgumentException("tags.lote-reclassificacao deve ser positivo: " + loteReclassificacao);
        }
    }
}
