package br.com.giro.mercado.web;

import br.com.giro.mercado.application.ConsumidorReposicao;
import br.com.giro.mercado.application.contrato.EventoReposicao;
import br.com.giro.mercado.application.contrato.ReposicaoEnviada;
import br.com.giro.mercado.application.contrato.ReposicaoNegada;
import br.com.giro.mercado.infra.outbox.TransporteHttp;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Entrada de eventos vindos do estoque. Responde 204 quando o evento foi processado ou já tinha
 * sido (duplicata); 400 quando o evento é inválido. Qualquer outra falha vira 5xx e o estoque reenvia.
 */
@RestController
@RequestMapping("/eventos")
public class EventoController {

    /** Tipos que o mercado consome. */
    private static final Map<String, Class<? extends EventoReposicao>> TIPOS_ACEITOS = Map.of(
            "ReposicaoEnviada", ReposicaoEnviada.class,
            "ReposicaoNegada", ReposicaoNegada.class);

    private final ConsumidorReposicao consumidor;
    private final JsonMapper json;
    private final Validator validator;

    public EventoController(ConsumidorReposicao consumidor, JsonMapper json, Validator validator) {
        this.consumidor = consumidor;
        this.json = json;
        this.validator = validator;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receber(@RequestHeader(TransporteHttp.HEADER_TIPO) String tipo,
                                        @RequestBody String corpo) {
        consumidor.processar(decodificar(tipo, corpo));
        return ResponseEntity.noContent().build();
    }

    private EventoReposicao decodificar(String tipo, String corpo) {
        var classe = TIPOS_ACEITOS.get(tipo);
        if (classe == null) {
            throw new EventoInvalidoException("tipo de evento não aceito: " + tipo);
        }
        EventoReposicao evento;
        try {
            evento = json.readValue(corpo, classe);
        } catch (JacksonException e) {
            throw new EventoInvalidoException("payload ilegível para " + tipo + ": " + e.getOriginalMessage(), e);
        }
        var violacoes = validator.validate(evento);
        if (!violacoes.isEmpty()) {
            throw new EventoInvalidoException(violacoes.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; ", tipo + " inválido: ", "")));
        }
        return evento;
    }

    @ExceptionHandler(EventoInvalidoException.class)
    ProblemDetail eventoInvalido(EventoInvalidoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
