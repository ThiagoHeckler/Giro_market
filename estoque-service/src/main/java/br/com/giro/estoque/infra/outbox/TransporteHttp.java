package br.com.giro.estoque.infra.outbox;

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
        var http = HttpClient.newBuilder().connectTimeout(propriedades.timeout()).build();
        var fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(propriedades.timeout());
        this.cliente = RestClient.builder()
                .baseUrl(propriedades.destino().toString())
                .requestFactory(fabrica)
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
