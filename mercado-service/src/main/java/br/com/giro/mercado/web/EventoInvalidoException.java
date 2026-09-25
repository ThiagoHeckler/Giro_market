package br.com.giro.mercado.web;

/** Evento recebido que não pode ser processado como está — reenviar não resolve. */
public class EventoInvalidoException extends RuntimeException {

    public EventoInvalidoException(String mensagem) {
        super(mensagem);
    }

    public EventoInvalidoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
