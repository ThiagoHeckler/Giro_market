package br.com.giro.mercado;

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
    /** Resposta de GET /produtos/{sku}; {@code null} = produto classificado genérico com o SKU pedido. */
    private static volatile Integer statusProduto;
    private static volatile String corpoProduto;
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
            servidor.createContext("/produtos/", troca -> {
                try (troca) {
                    var sku = troca.getRequestURI().getPath().substring("/produtos/".length());
                    int codigo = statusProduto == null ? 200 : statusProduto;
                    var corpo = corpoProduto != null ? corpoProduto : """
                            {"sku":"%s","descricao":"COCA COLA 2L PET","ncm":"22021000",
                             "tags":["refrigerante","coca cola"],"categoria":"bebidas","saldoDisponivel":50}"""
                            .formatted(sku);
                    var bytes = corpo.getBytes(StandardCharsets.UTF_8);
                    troca.getResponseHeaders().add("Content-Type", "application/json");
                    troca.sendResponseHeaders(codigo, bytes.length);
                    troca.getResponseBody().write(bytes);
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

    public static void responderProdutoCom(int novoStatus, String novoCorpo) {
        statusProduto = novoStatus;
        corpoProduto = novoCorpo;
    }

    public static void reiniciar() {
        RECEBIDOS.clear();
        status = 204;
        statusProduto = null;
        corpoProduto = null;
    }
}
