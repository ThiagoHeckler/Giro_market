package br.com.giro.mercado.web;

import br.com.giro.mercado.application.CheckoutService;
import br.com.giro.mercado.application.ConsultaPedido;
import br.com.giro.mercado.application.PagamentoDePedido;
import br.com.giro.mercado.application.PedidoDetalhe;
import br.com.giro.mercado.application.PedidoFechado;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final CheckoutService checkout;
    private final ConsultaPedido consulta;
    private final PagamentoDePedido pagamento;

    public PedidoController(CheckoutService checkout, ConsultaPedido consulta, PagamentoDePedido pagamento) {
        this.checkout = checkout;
        this.consulta = consulta;
        this.pagamento = pagamento;
    }

    /** Checkout: reserva e debita na hora, antes do pagamento. */
    @PostMapping
    public ResponseEntity<PedidoFechado> fechar(@Valid @RequestBody CheckoutRequest requisicao) {
        var fechado = checkout.fechar(requisicao.itens());
        return ResponseEntity.created(URI.create("/pedidos/" + fechado.pedidoId())).body(fechado);
    }

    /** Pagamento simulado: 200 com o pedido pago (também se já estava); 409 se a reserva expirou. */
    @PostMapping("/{id}/pagamento")
    public ResponseEntity<PedidoDetalhe> pagar(@PathVariable UUID id) {
        pagamento.pagar(id);
        return ResponseEntity.of(consulta.buscar(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoDetalhe> buscar(@PathVariable UUID id) {
        return ResponseEntity.of(consulta.buscar(id));
    }
}
