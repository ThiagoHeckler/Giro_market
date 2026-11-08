package br.com.giro.mercado.infra.outbox;

/**
 * O destino recusou o evento de forma definitiva (4xx que não melhora com nova tentativa, como 400 ou 422).
 * Reenviar só repetiria a recusa; o evento vai para {@link StatusOutbox#FAILED}.
 */
public class EventoRejeitadoException extends FalhaEnvioException {

    public EventoRejeitadoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
