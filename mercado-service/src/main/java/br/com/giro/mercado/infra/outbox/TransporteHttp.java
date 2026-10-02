package br.com.giro.mercado.infra.outbox;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;

/** Entrega um evento da outbox no serviço de destino: {@code POST /eventos} com o tipo no header. */
@Component
public class TransporteHttp {

    public static final String HEADER_TIPO = "Evento-Tipo";

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

    /** Retorna só se o destino respondeu 2xx; qualquer outra coisa vira {@link FalhaEnvioException}. */
    public void enviar(OutboxEvent evento) {
        try {
            cliente.post()
                    .uri("/eventos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HEADER_TIPO, evento.getTipo())
                    .body(evento.getPayload())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new FalhaEnvioException("envio de %s %s falhou: %s"
                    .formatted(evento.getTipo(), evento.getId(), e.getMessage()), e);
        }
    }
}
