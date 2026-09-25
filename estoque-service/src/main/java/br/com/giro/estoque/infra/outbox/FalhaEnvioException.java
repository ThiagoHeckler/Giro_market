package br.com.giro.estoque.infra.outbox;

/** O destino não confirmou o recebimento; o evento será reenviado. */
public class FalhaEnvioException extends RuntimeException {

    public FalhaEnvioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
