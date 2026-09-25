package br.com.giro.estoque.infra.tags;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;

/**
 * Cliente do tag-worker. Nunca lança: worker fora do ar, lento ou com resposta estranha vira
 * {@link Optional#empty()} e o produto segue sem tags, para ser reclassificado depois.
 */
@Component
public class ClassificadorTags {

    private static final Logger log = LoggerFactory.getLogger(ClassificadorTags.class);

    public record Classificacao(List<String> tags, String categoria) {
    }

    private record Pedido(String sku, String descricao) {
    }

    private final RestClient cliente;

    public ClassificadorTags(TagsProperties propriedades) {
        // HTTP/1.1 explícito: o padrão do JDK tenta upgrade h2c, e o uvicorn descarta o corpo nesse caso.
        var http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(propriedades.timeout())
                .build();
        var fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(propriedades.timeout());
        this.cliente = RestClient.builder()
                .baseUrl(propriedades.workerUrl().toString())
                .requestFactory(fabrica)
                .build();
    }

    public Optional<Classificacao> classificar(String sku, String descricao) {
        try {
            var resposta = cliente.post()
                    .uri("/classificar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new Pedido(sku, descricao))
                    .retrieve()
                    .body(Classificacao.class);
            if (resposta == null || resposta.tags() == null || resposta.tags().isEmpty()
                    || resposta.categoria() == null || resposta.categoria().isBlank()) {
                log.warn("tag-worker respondeu sem classificação utilizável sku={}", sku);
                return Optional.empty();
            }
            return Optional.of(resposta);
        } catch (RestClientException e) {
            log.warn("tag-worker indisponível sku={} erro={} — produto segue sem tags", sku, e.getMessage());
            return Optional.empty();
        }
    }
}
