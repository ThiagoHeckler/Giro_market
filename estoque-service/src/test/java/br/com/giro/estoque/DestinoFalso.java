package br.com.giro.estoque;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Servidor HTTP que faz o papel do outro serviço: registra o que recebe e responde o status configurado. */
public final class DestinoFalso {

    public record Recebido(String tipo, String corpo) {
    }

    private static final List<Recebido> RECEBIDOS = new CopyOnWriteArrayList<>();
    private static volatile int status = 204;
    private static final HttpServer SERVIDOR = iniciar();

    private DestinoFalso() {
    }

    private static HttpServer iniciar() {
        try {
            var servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            servidor.createContext("/eventos", troca -> {
                try (troca) {
                    var corpo = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    RECEBIDOS.add(new Recebido(troca.getRequestHeaders().getFirst("Evento-Tipo"), corpo));
                    troca.sendResponseHeaders(status, -1);
                }
            });
            servidor.start();
            return servidor;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String url() {
        return "http://127.0.0.1:" + SERVIDOR.getAddress().getPort();
    }

    public static List<Recebido> recebidos() {
        return List.copyOf(RECEBIDOS);
    }

    public static void responderCom(int novoStatus) {
        status = novoStatus;
    }

    public static void reiniciar() {
        RECEBIDOS.clear();
        status = 204;
    }
}
