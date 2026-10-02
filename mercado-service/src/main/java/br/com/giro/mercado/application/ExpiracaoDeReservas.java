package br.com.giro.mercado.application;

import br.com.giro.mercado.domain.Reserva;
import br.com.giro.mercado.domain.StatusPedido;
import br.com.giro.mercado.infra.persistencia.PedidoRepository;
import br.com.giro.mercado.infra.persistencia.ProdutoVitrineRepository;
import br.com.giro.mercado.infra.persistencia.ReservaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;

/**
 * Reserva vencida sem pagamento: as unidades voltam para a prateleira e o pedido expira. Uma
 * transação por pedido (a falha de um não desfaz os outros), com o pedido travado — pagamento e
 * expiração do mesmo pedido nunca vencem os dois.
 */
@Service
public class ExpiracaoDeReservas {

    private static final Logger log = LoggerFactory.getLogger(ExpiracaoDeReservas.class);

    private final PedidoRepository pedidos;
    private final ReservaRepository reservas;
    private final ProdutoVitrineRepository produtos;
    private final ReservaProperties propriedades;
    private final TransactionTemplate transacao;
    private final Clock relogio;

    public ExpiracaoDeReservas(PedidoRepository pedidos, ReservaRepository reservas,
                               ProdutoVitrineRepository produtos, ReservaProperties propriedades,
                               TransactionTemplate transacao, Clock relogio) {
        this.pedidos = pedidos;
        this.reservas = reservas;
        this.produtos = produtos;
        this.propriedades = propriedades;
        this.transacao = transacao;
        this.relogio = relogio;
    }

    /** @return quantos pedidos expiraram nesta rodada */
    public int expirarVencidas() {
        var agora = relogio.instant();
        int expirados = 0;
        for (var pedidoId : reservas.pedidosComReservaVencida(agora, propriedades.loteExpiracao())) {
            if (Boolean.TRUE.equals(transacao.execute(_ -> expirar(pedidoId, agora)))) {
                expirados++;
            }
        }
        if (expirados > 0) {
            log.info("Expiração de reservas: {} pedido(s) expirado(s)", expirados);
        }
        return expirados;
    }

    private boolean expirar(UUID pedidoId, Instant agora) {
        var travado = pedidos.travarSeLivre(pedidoId);
        if (travado.isEmpty()) {
            return false;   // ocupado agora (um pagamento em curso, por exemplo): fica para a próxima rodada
        }
        var pedido = travado.get();
        // Reconfere sob a trava: o pedido pode ter sido pago entre a busca e a trava.
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            return false;
        }
        var ativas = reservas.findByPedidoId(pedidoId).stream()
                .filter(r -> r.venceuEm(agora))
                // Mesma ordem de travas do checkout (SKU crescente): sem deadlock com carrinhos em curso.
                .sorted(Comparator.comparing(Reserva::getSku))
                .toList();
        for (var reserva : ativas) {
            var produto = produtos.buscarParaAtualizar(reserva.getSku()).orElseThrow();
            produto.devolver(reserva.getQtd());
            reserva.expirar();
        }
        pedido.expirar();
        log.info("Reserva expirada pedidoId={} itens={} unidadesDevolvidas={}", pedidoId, ativas.size(),
                ativas.stream().mapToInt(Reserva::getQtd).sum());
        return true;
    }
}
