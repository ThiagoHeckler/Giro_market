package br.com.giro.mercado.application;

import br.com.giro.mercado.domain.PagamentoRecusadoException;
import br.com.giro.mercado.domain.PedidoNaoEncontradoException;
import br.com.giro.mercado.domain.StatusPedido;
import br.com.giro.mercado.domain.StatusReserva;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagamentoDePedidoTest extends ReservaTestes {

    @Autowired
    TransactionTemplate transacao;

    @Test
    void pagamentoNoPrazoConfirmaPedidoEReservas() {
        cadastra(COCA, 20, 5, 20);
        var pedidoId = comprar(COCA, 2);

        pagamento.pagar(pedidoId);

        assertThat(pedido(pedidoId).getStatus()).isEqualTo(StatusPedido.PAGO);
        assertThat(statusDasReservas(pedidoId)).containsOnly(StatusReserva.CONFIRMADA);
        assertThat(produto(COCA).getEstoquePrateleira()).isEqualTo(18);   // o débito do checkout vira venda
    }

    @Test
    void pagamentoRepetidoNaoMudaNada() {
        cadastra(COCA, 20, 5, 20);
        var pedidoId = comprar(COCA, 2);
        pagamento.pagar(pedidoId);

        pagamento.pagar(pedidoId);

        assertThat(pedido(pedidoId).getStatus()).isEqualTo(StatusPedido.PAGO);
        assertThat(statusDasReservas(pedidoId)).containsOnly(StatusReserva.CONFIRMADA);
    }

    @Test
    void pagamentoDepoisDoPrazoEhRecusadoMesmoAntesDaVarredura() {
        cadastra(COCA, 20, 5, 20);
        var pedidoId = comprar(COCA, 2);
        vencerReserva(pedidoId);

        assertThatThrownBy(() -> pagamento.pagar(pedidoId)).isInstanceOf(PagamentoRecusadoException.class)
                .hasMessageContaining("prazo da reserva");

        assertThat(pedido(pedidoId).getStatus()).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);
        assertThat(expiracao.expirarVencidas()).isEqualTo(1);
        assertThat(produto(COCA).getEstoquePrateleira()).isEqualTo(20);
    }

    @Test
    void pedidoExpiradoNaoAceitaPagamento() {
        cadastra(COCA, 20, 5, 20);
        var pedidoId = comprar(COCA, 2);
        vencerReserva(pedidoId);
        expiracao.expirarVencidas();

        assertThatThrownBy(() -> pagamento.pagar(pedidoId)).isInstanceOf(PagamentoRecusadoException.class);

        assertThat(pedido(pedidoId).getStatus()).isEqualTo(StatusPedido.EXPIRADO);
    }

    @Test
    void pagamentoDuranteAExpiracaoEsperaATravaEEhRecusado() throws Exception {
        cadastra(COCA, 20, 5, 20);
        var pedidoId = comprar(COCA, 2);
        vencerReserva(pedidoId);
        var expirou = new CountDownLatch(1);
        var segurando = Duration.ofMillis(300);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // A varredura expira o pedido e segura as travas um pouco antes de commitar.
            var varredura = executor.submit(() -> transacao.executeWithoutResult(_ -> {
                assertThat(expiracao.expirarVencidas()).isEqualTo(1);
                expirou.countDown();
                dormir(segurando);
            }));
            expirou.await();

            long inicio = System.nanoTime();
            assertThatThrownBy(() -> pagamento.pagar(pedidoId)).isInstanceOf(PagamentoRecusadoException.class);
            var esperou = Duration.ofNanos(System.nanoTime() - inicio);

            varredura.get();
            // Esperou o commit da expiração (FOR UPDATE) e só então viu o pedido expirado.
            assertThat(esperou).isGreaterThanOrEqualTo(segurando.dividedBy(2));
        }

        assertThat(pedido(pedidoId).getStatus()).isEqualTo(StatusPedido.EXPIRADO);
        assertThat(produto(COCA).getEstoquePrateleira()).isEqualTo(20);
    }

    @Test
    void pedidoInexistente() {
        assertThatThrownBy(() -> pagamento.pagar(UUID.randomUUID())).isInstanceOf(PedidoNaoEncontradoException.class);
    }

    private static void dormir(Duration tempo) {
        try {
            Thread.sleep(tempo);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
