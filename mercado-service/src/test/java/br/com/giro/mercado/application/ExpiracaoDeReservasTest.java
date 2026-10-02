package br.com.giro.mercado.application;

import br.com.giro.mercado.domain.StatusPedido;
import br.com.giro.mercado.domain.StatusReserva;
import br.com.giro.mercado.domain.StatusVitrine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class ExpiracaoDeReservasTest extends ReservaTestes {

    @Autowired
    TransactionTemplate transacao;

    @Test
    void reservaVencidaDevolveAsUnidadesEExpiraOPedido() {
        cadastra(COCA, 20, 5, 20);
        cadastra(LEITE, 30, 5, 30);
        var pedidoId = checkout.fechar(List.of(new ItemCheckout(COCA, 2), new ItemCheckout(LEITE, 3))).pedidoId();
        vencerReserva(pedidoId);

        assertThat(expiracao.expirarVencidas()).isEqualTo(1);

        assertThat(pedido(pedidoId).getStatus()).isEqualTo(StatusPedido.EXPIRADO);
        assertThat(statusDasReservas(pedidoId)).containsOnly(StatusReserva.EXPIRADA);
        assertThat(produto(COCA).getEstoquePrateleira()).isEqualTo(20);
        assertThat(produto(LEITE).getEstoquePrateleira()).isEqualTo(30);
    }

    @Test
    void produtoEsgotadoPelaReservaVoltaParaAVitrine() {
        cadastra(COCA, 3, 5, 20);
        var pedidoId = comprar(COCA, 3);
        assertThat(produto(COCA).getStatus()).isEqualTo(StatusVitrine.ESGOTADO);
        vencerReserva(pedidoId);

        expiracao.expirarVencidas();

        assertThat(produto(COCA).getStatus()).isEqualTo(StatusVitrine.DISPONIVEL);
        assertThat(produto(COCA).getEstoquePrateleira()).isEqualTo(3);
    }

    @Test
    void reservaNoPrazoNaoEhTocada() {
        cadastra(COCA, 20, 5, 20);
        var pedidoId = comprar(COCA, 2);

        assertThat(expiracao.expirarVencidas()).isZero();

        assertThat(pedido(pedidoId).getStatus()).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);
        assertThat(statusDasReservas(pedidoId)).containsOnly(StatusReserva.ATIVA);
        assertThat(produto(COCA).getEstoquePrateleira()).isEqualTo(18);
    }

    @Test
    void pedidoPagoNuncaExpira() {
        cadastra(COCA, 20, 5, 20);
        var pedidoId = comprar(COCA, 2);
        pagamento.pagar(pedidoId);
        vencerReserva(pedidoId);

        assertThat(expiracao.expirarVencidas()).isZero();

        assertThat(pedido(pedidoId).getStatus()).isEqualTo(StatusPedido.PAGO);
        assertThat(produto(COCA).getEstoquePrateleira()).isEqualTo(18);
    }

    @Test
    void devolucaoPassaDoIdealQuandoAReposicaoJaChegou() {
        cadastra(COCA, 10, 5, 20);
        var pedidoId = comprar(COCA, 6);                       // prateleira 4: pede 16
        assertThat(reposicoesSolicitadas(COCA)).containsExactly(16);
        var coca = produto(COCA);
        coca.creditar(16);                                     // o lote chegou: prateleira 20
        produtos.save(coca);
        vencerReserva(pedidoId);

        expiracao.expirarVencidas();

        // Decisão documentada: o excesso é no máximo a reserva, e não dispara nova reposição.
        assertThat(produto(COCA).getEstoquePrateleira()).isEqualTo(26);
        assertThat(reposicoesSolicitadas(COCA)).containsExactly(16);
    }

    @Test
    void duasVarredurasEmParaleloDevolvemUmaVezSo() throws Exception {
        cadastra(COCA, 20, 5, 20);
        var pedidoId = comprar(COCA, 2);
        vencerReserva(pedidoId);
        var largada = new CountDownLatch(1);

        int expirados = 0;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Integer>> rodadas = List.of(
                    executor.submit(() -> { largada.await(); return expiracao.expirarVencidas(); }),
                    executor.submit(() -> { largada.await(); return expiracao.expirarVencidas(); }));
            largada.countDown();
            for (var rodada : rodadas) {
                expirados += rodada.get();
            }
        }

        assertThat(expirados).isEqualTo(1);
        assertThat(produto(COCA).getEstoquePrateleira()).isEqualTo(20);
    }

    @Test
    void pedidoTravadoPorOutraTransacaoFicaParaAProximaRodada() throws Exception {
        cadastra(COCA, 20, 5, 20);
        var pedidoId = comprar(COCA, 2);
        vencerReserva(pedidoId);
        var travou = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var outra = executor.submit(() -> transacao.executeWithoutResult(_ -> {
                pedidos.buscarParaAtualizar(pedidoId);         // um pagamento em curso, por exemplo
                travou.countDown();
                aguardar(liberar);
            }));
            travou.await();

            assertThat(expiracao.expirarVencidas()).isZero();  // SKIP LOCKED: não espera nem trava a rodada
            assertThat(produto(COCA).getEstoquePrateleira()).isEqualTo(18);

            liberar.countDown();
            outra.get();
        }

        assertThat(expiracao.expirarVencidas()).isEqualTo(1);
        assertThat(produto(COCA).getEstoquePrateleira()).isEqualTo(20);
    }

    private static void aguardar(CountDownLatch sinal) {
        try {
            sinal.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
