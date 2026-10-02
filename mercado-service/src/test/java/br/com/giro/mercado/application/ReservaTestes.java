package br.com.giro.mercado.application;

import br.com.giro.mercado.IntegracaoTest;
import br.com.giro.mercado.domain.Pedido;
import br.com.giro.mercado.domain.ProdutoVitrine;
import br.com.giro.mercado.domain.StatusReserva;
import br.com.giro.mercado.infra.persistencia.PedidoRepository;
import br.com.giro.mercado.infra.persistencia.ProdutoVitrineRepository;
import br.com.giro.mercado.infra.persistencia.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Base dos testes de pagamento e expiração: cadastro, checkout e "passar do prazo" da reserva. */
abstract class ReservaTestes extends IntegracaoTest {

    static final String COCA = "7894900011517";
    static final String LEITE = "7891000100103";

    @Autowired
    CheckoutService checkout;

    @Autowired
    PagamentoDePedido pagamento;

    @Autowired
    ExpiracaoDeReservas expiracao;

    @Autowired
    ProdutoVitrineRepository produtos;

    @Autowired
    PedidoRepository pedidos;

    @Autowired
    ReservaRepository reservas;

    void cadastra(String sku, int prateleira, int minimo, int ideal) {
        produtos.save(new ProdutoVitrine(sku, "Produto " + sku, new BigDecimal("5.00"), prateleira, minimo, ideal));
    }

    UUID comprar(String sku, int qtd) {
        return checkout.fechar(List.of(new ItemCheckout(sku, qtd))).pedidoId();
    }

    /** Leva o prazo da reserva para o passado, como se o cliente tivesse sumido. */
    void vencerReserva(UUID pedidoId) {
        jdbc.update("UPDATE reserva SET expira_em = now() - interval '1 minute' WHERE pedido_id = ?", pedidoId);
    }

    ProdutoVitrine produto(String sku) {
        return produtos.findById(sku).orElseThrow();
    }

    Pedido pedido(UUID id) {
        return pedidos.findById(id).orElseThrow();
    }

    List<StatusReserva> statusDasReservas(UUID pedidoId) {
        return reservas.findByPedidoId(pedidoId).stream().map(r -> r.getStatus()).toList();
    }
}
