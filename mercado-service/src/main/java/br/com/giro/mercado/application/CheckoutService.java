package br.com.giro.mercado.application;

import br.com.giro.mercado.domain.Pedido;
import br.com.giro.mercado.domain.ProdutoNaoEncontradoException;
import br.com.giro.mercado.domain.ProdutoVitrine;
import br.com.giro.mercado.domain.Reserva;
import br.com.giro.mercado.infra.persistencia.PedidoRepository;
import br.com.giro.mercado.infra.persistencia.ProdutoVitrineRepository;
import br.com.giro.mercado.infra.persistencia.ReservaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Checkout: reserva e debita a prateleira na hora (não no pagamento), para não vender o
 * último item duas vezes. Pedido, reservas, débito e evento de reposição commitam juntos.
 */
@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final ProdutoVitrineRepository produtos;
    private final PedidoRepository pedidos;
    private final ReservaRepository reservas;
    private final GatilhoReposicao gatilho;
    private final ReservaProperties reservaProperties;
    private final Clock relogio;

    public CheckoutService(ProdutoVitrineRepository produtos, PedidoRepository pedidos,
                           ReservaRepository reservas, GatilhoReposicao gatilho,
                           ReservaProperties reservaProperties, Clock relogio) {
        this.produtos = produtos;
        this.pedidos = pedidos;
        this.reservas = reservas;
        this.gatilho = gatilho;
        this.reservaProperties = reservaProperties;
        this.relogio = relogio;
    }

    @Transactional
    public PedidoFechado fechar(List<ItemCheckout> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("checkout sem itens");
        }
        var agora = relogio.instant();
        var expiraEm = agora.plus(reservaProperties.ttl());
        var pedido = new Pedido(agora);
        var tocados = new ArrayList<ProdutoVitrine>();

        // Travas sempre na mesma ordem (SKU crescente) evitam deadlock entre carrinhos cruzados.
        for (Map.Entry<String, Integer> item : consolidarPorSku(itens).entrySet()) {
            var produto = produtos.buscarParaAtualizar(item.getKey())
                    .orElseThrow(() -> new ProdutoNaoEncontradoException(item.getKey()));
            produto.debitar(item.getValue());
            pedido.adicionarItem(produto.getSku(), item.getValue(), produto.getPreco());
            tocados.add(produto);
        }

        pedidos.save(pedido);
        pedido.getItens().forEach(i -> reservas.save(new Reserva(i.getSku(), i.getQtd(), pedido.getId(), expiraEm)));
        tocados.forEach(gatilho::avaliar);

        log.info("Pedido fechado pedidoId={} itens={} total={}", pedido.getId(), pedido.getItens().size(), pedido.getTotal());
        return new PedidoFechado(pedido.getId(), pedido.getTotal(), expiraEm);
    }

    private static TreeMap<String, Integer> consolidarPorSku(List<ItemCheckout> itens) {
        var porSku = new TreeMap<String, Integer>();
        for (var item : itens) {
            if (item.qtd() <= 0) {
                throw new IllegalArgumentException("quantidade deve ser positiva: " + item);
            }
            porSku.merge(item.sku(), item.qtd(), Math::addExact);
        }
        return porSku;
    }
}
