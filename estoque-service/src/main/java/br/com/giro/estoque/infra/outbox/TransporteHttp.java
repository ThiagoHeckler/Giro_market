package br.com.giro.estoque.infra.outbox;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.util.Set;

/** Entrega um evento da outbox no serviço de destino: {@code POST /eventos} com o tipo no header. */
@Component
public class TransporteHttp {

    public static final String HEADER_TIPO = "Evento-Tipo";

    /**
     * 4xx que valem nova tentativa: 401/403 são token errado (corrige-se o .env e o fluxo volta sozinho),
     * 408/429 são temporários por definição. Os demais 4xx são recusa do próprio evento.
     */
    private static final Set<Integer> CLIENTE_REENVIAVEL = Set.of(401, 403, 408, 429);

    private final RestClient cliente;

    public TransporteHttp(OutboxProperties propriedades) {
        // HTTP/1.1 explícito: o padrão do JDK tenta upgrade h2c, e o uvicorn descarta o corpo nesse caso.
        var http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(propriedades.timeout())
                .build();
        var fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(propriedades.timeout());
        this.cliente = RestClient.builder()
                .baseUrl(propriedades.destino().toString())
                .requestFactory(fabrica)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + propriedades.token())
                .build();
    }

    /**
     * Retorna só se o destino respondeu 2xx. Recusa definitiva vira {@link EventoRejeitadoException};
     * o resto (5xx, timeout, rede, 4xx reenviável) vira {@link FalhaEnvioException}.
     */
    public void enviar(OutboxEvent evento) {
        try {
            cliente.post()
                    .uri("/eventos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HEADER_TIPO, evento.getTipo())
                    .body(evento.getPayload())
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            if (CLIENTE_REENVIAVEL.contains(e.getStatusCode().value())) {
                throw new FalhaEnvioException(mensagemDeFalha(evento, e), e);
            }
            throw new EventoRejeitadoException(mensagemDeFalha(evento, e), e);
        } catch (RestClientException e) {
            throw new FalhaEnvioException(mensagemDeFalha(evento, e), e);
        }
    }

    private static String mensagemDeFalha(OutboxEvent evento, RestClientException e) {
        return "envio de %s %s falhou: %s".formatted(evento.getTipo(), evento.getId(), e.getMessage());
    }
}
