package br.com.giro.mercado.application;

import br.com.giro.mercado.domain.PagamentoRecusadoException;
import br.com.giro.mercado.domain.PedidoNaoEncontradoException;
import br.com.giro.mercado.domain.Reserva;
import br.com.giro.mercado.domain.StatusPedido;
import br.com.giro.mercado.infra.persistencia.PedidoRepository;
import br.com.giro.mercado.infra.persistencia.ReservaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Confirmação de pagamento (simulada: não há gateway). Trava o pedido, então pagamento e expiração
 * nunca vencem os dois. O prazo é conferido pelo relógio, não pelo status: um pagamento que chega
 * depois do prazo é recusado mesmo que a varredura de expiração ainda não tenha passado.
 */
@Service
public class PagamentoDePedido {

    private static final Logger log = LoggerFactory.getLogger(PagamentoDePedido.class);

    private final PedidoRepository pedidos;
    private final ReservaRepository reservas;
    private final Clock relogio;

    public PagamentoDePedido(PedidoRepository pedidos, ReservaRepository reservas, Clock relogio) {
        this.pedidos = pedidos;
        this.reservas = reservas;
        this.relogio = relogio;
    }

    @Transactional
    public void pagar(UUID pedidoId) {
        var pedido = pedidos.buscarParaAtualizar(pedidoId)
                .orElseThrow(() -> new PedidoNaoEncontradoException(pedidoId));
        var reservasDoPedido = reservas.findByPedidoId(pedidoId);
        var agora = relogio.instant();
        if (pedido.getStatus() == StatusPedido.AGUARDANDO_PAGAMENTO
                && reservasDoPedido.stream().anyMatch(r -> r.venceuEm(agora))) {
            throw PagamentoRecusadoException.prazoVencido(pedidoId);
        }
        if (!pedido.pagar()) {
            log.info("Pagamento repetido ignorado pedidoId={}", pedidoId);
            return;
        }
        reservasDoPedido.forEach(Reserva::confirmar);
        log.info("Pedido pago pedidoId={} total={}", pedidoId, pedido.getTotal());
    }
}
