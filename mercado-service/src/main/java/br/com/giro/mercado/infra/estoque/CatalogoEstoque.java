package br.com.giro.mercado.infra.estoque;

import br.com.giro.mercado.domain.SkuDesconhecidoNoEstoqueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;

/** Consulta síncrona ao estoque, usada só no cadastro — nunca no caminho da venda. */
@Component
public class CatalogoEstoque {

    private static final Logger log = LoggerFactory.getLogger(CatalogoEstoque.class);

    /** {@code tags} e {@code categoria} nulos enquanto o estoque não classificou o produto. */
    public record ProdutoNoEstoque(String sku, String descricao, List<String> tags, String categoria) {

        public boolean classificado() {
            return tags != null && !tags.isEmpty() && categoria != null && !categoria.isBlank();
        }
    }

    private final RestClient cliente;

    public CatalogoEstoque(EstoqueProperties propriedades) {
        // HTTP/1.1 explícito: o padrão do JDK tenta upgrade h2c.
        var http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(propriedades.timeout())
                .build();
        var fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(propriedades.timeout());
        this.cliente = RestClient.builder()
                .baseUrl(propriedades.url().toString())
                .requestFactory(fabrica)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + propriedades.token())
                .build();
    }

    /**
     * @return o produto no estoque, ou vazio se o estoque não respondeu (o cadastro segue sem classificação)
     * @throws SkuDesconhecidoNoEstoqueException se o estoque respondeu que o SKU não existe
     */
    public Optional<ProdutoNoEstoque> consultar(String sku) {
        try {
            return Optional.ofNullable(cliente.get().uri("/produtos/{sku}", sku).retrieve().body(ProdutoNoEstoque.class));
        } catch (HttpClientErrorException.NotFound e) {
            throw new SkuDesconhecidoNoEstoqueException(sku);
        } catch (RestClientException e) {
            log.warn("Estoque indisponível na consulta sku={} erro={} — cadastro segue sem classificação",
                    sku, e.getMessage());
            return Optional.empty();
        }
    }
}
