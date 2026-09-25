package br.com.giro.estoque;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Faz o papel do tag-worker: resposta, status e atraso configuráveis; conta as chamadas. */
public final class WorkerFalso {

    public static final String CLASSIFICACAO_PADRAO = """
            {"tags":["refrigerante","coca cola","2l"],"categoria":"bebidas"}""";

    private static final AtomicInteger CHAMADAS = new AtomicInteger();
    private static volatile String ultimoCorpoRecebido;
    private static volatile String ultimoContentType;
    private static volatile int status = 200;
    private static volatile String corpo = CLASSIFICACAO_PADRAO;
    private static volatile Duration atraso = Duration.ZERO;
    private static final HttpServer SERVIDOR = iniciar();

    private WorkerFalso() {
    }

    private static HttpServer iniciar() {
        try {
            var servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            servidor.createContext("/classificar", troca -> {
                try (troca) {
                    ultimoCorpoRecebido = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    ultimoContentType = troca.getRequestHeaders().getFirst("Content-Type");
                    CHAMADAS.incrementAndGet();
                    Thread.sleep(atraso);
                    var bytes = corpo.getBytes(StandardCharsets.UTF_8);
                    troca.getResponseHeaders().add("Content-Type", "application/json");
                    troca.sendResponseHeaders(status, bytes.length);
                    troca.getResponseBody().write(bytes);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (IOException e) {
                    // cliente desistiu (timeout) antes da resposta: esperado nos testes de lentidão
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

    public static int chamadas() {
        return CHAMADAS.get();
    }

    public static String ultimoCorpoRecebido() {
        return ultimoCorpoRecebido;
    }

    public static String ultimoContentType() {
        return ultimoContentType;
    }

    public static void responderCom(int novoStatus, String novoCorpo) {
        status = novoStatus;
        corpo = novoCorpo;
    }

    public static void atrasar(Duration novoAtraso) {
        atraso = novoAtraso;
    }

    public static void reiniciar() {
        CHAMADAS.set(0);
        ultimoCorpoRecebido = null;
        ultimoContentType = null;
        status = 200;
        corpo = CLASSIFICACAO_PADRAO;
        atraso = Duration.ZERO;
    }
}
