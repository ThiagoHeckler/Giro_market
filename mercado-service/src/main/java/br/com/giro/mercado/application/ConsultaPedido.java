package br.com.giro.mercado.application;

import br.com.giro.mercado.domain.ProdutoVitrine;
import br.com.giro.mercado.domain.Reserva;
import br.com.giro.mercado.infra.persistencia.PedidoRepository;
import br.com.giro.mercado.infra.persistencia.ProdutoVitrineRepository;
import br.com.giro.mercado.infra.persistencia.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ConsultaPedido {

    private final PedidoRepository pedidos;
    private final ReservaRepository reservas;
    private final ProdutoVitrineRepository produtos;

    public ConsultaPedido(PedidoRepository pedidos, ReservaRepository reservas, ProdutoVitrineRepository produtos) {
        this.pedidos = pedidos;
        this.reservas = reservas;
        this.produtos = produtos;
    }

    public Optional<PedidoDetalhe> buscar(UUID id) {
        return pedidos.findById(id).map(pedido -> {
            var skus = pedido.getItens().stream().map(i -> i.getSku()).toList();
            Map<String, String> nomes = produtos.findAllById(skus).stream()
                    .collect(Collectors.toMap(ProdutoVitrine::getSku, ProdutoVitrine::getNome));
            Instant expiraEm = reservas.findByPedidoId(id).stream()
                    .map(Reserva::getExpiraEm).min(Comparator.naturalOrder()).orElse(null);
            var itens = pedido.getItens().stream()
                    .map(i -> new PedidoDetalhe.Item(i.getSku(), nomes.getOrDefault(i.getSku(), i.getSku()),
                            i.getQtd(), i.getPrecoUnitario()))
                    .toList();
            return new PedidoDetalhe(pedido.getId(), pedido.getStatus(), pedido.getTotal(), pedido.getCriadoEm(),
                    expiraEm, itens);
        });
    }
}
