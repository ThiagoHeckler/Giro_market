package br.com.giro.mercado.application;

import br.com.giro.mercado.IntegracaoTest;
import br.com.giro.mercado.domain.EstoqueInsuficienteException;
import br.com.giro.mercado.domain.ProdutoNaoEncontradoException;
import br.com.giro.mercado.domain.ProdutoVitrine;
import br.com.giro.mercado.domain.StatusReserva;
import br.com.giro.mercado.domain.StatusSolicitacao;
import br.com.giro.mercado.domain.StatusVitrine;
import br.com.giro.mercado.infra.persistencia.ProdutoVitrineRepository;
import br.com.giro.mercado.infra.persistencia.ReservaRepository;
import br.com.giro.mercado.infra.persistencia.SolicitacaoReposicaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutTest extends IntegracaoTest {

    private static final String COCA = "7894900011517";
    private static final String LEITE = "7891000100103";

    @Autowired
    CheckoutService checkout;

    @Autowired
    ProdutoVitrineRepository produtos;

    @Autowired
    ReservaRepository reservas;

    @Autowired
    SolicitacaoReposicaoRepository solicitacoes;

    private void cadastra(String sku, String preco, int prateleira, int minimo, int ideal) {
        produtos.save(new ProdutoVitrine(sku, "Produto " + sku, new BigDecimal(preco), prateleira, minimo, ideal));
    }

    private ProdutoVitrine produto(String sku) {
        return produtos.findById(sku).orElseThrow();
    }

    @Test
    void checkoutReservaDebitaECalculaTotal() {
        cadastra(COCA, "9.99", 20, 5, 20);
        cadastra(LEITE, "4.50", 30, 5, 30);

        var fechado = checkout.fechar(List.of(new ItemCheckout(COCA, 2), new ItemCheckout(LEITE, 3)));

        assertThat(fechado.total()).isEqualByComparingTo("33.48");
        assertThat(produto(COCA).getEstoquePrateleira()).isEqualTo(18);
        assertThat(produto(LEITE).getEstoquePrateleira()).isEqualTo(27);
        assertThat(reservas.findByPedidoId(fechado.pedidoId()))
                .extracting(r -> r.getSku() + ":" + r.getQtd() + ":" + r.getStatus())
                .containsExactlyInAnyOrder(COCA + ":2:" + StatusReserva.ATIVA, LEITE + ":3:" + StatusReserva.ATIVA);
        assertThat(reposicoesSolicitadas(COCA)).isEmpty();
    }

    @Test
    void cairAbaixoDoMinimoGravaUmaReposicaoAteOIdealNaOutbox() {
        cadastra(COCA, "9.99", 10, 5, 20);

        checkout.fechar(List.of(new ItemCheckout(COCA, 6)));   // prateleira 4 < mínimo 5

        assertThat(reposicoesSolicitadas(COCA)).containsExactly(16);   // 20 - 4
        assertThat(solicitacoes.existsBySkuAndStatusIn(COCA, List.of(StatusSolicitacao.PENDENTE))).isTrue();
    }

    @Test
    void vendasSeguidasComSolicitacaoPendenteNaoDisparamOutroPedido() {
        cadastra(COCA, "9.99", 10, 5, 20);

        checkout.fechar(List.of(new ItemCheckout(COCA, 6)));
        checkout.fechar(List.of(new ItemCheckout(COCA, 1)));
        checkout.fechar(List.of(new ItemCheckout(COCA, 1)));

        assertThat(reposicoesSolicitadas(COCA)).containsExactly(16);
    }

    @Test
    void venderOUltimoItemEsgotaOProduto() {
        cadastra(COCA, "9.99", 3, 5, 20);

        checkout.fechar(List.of(new ItemCheckout(COCA, 3)));

        assertThat(produto(COCA).getStatus()).isEqualTo(StatusVitrine.ESGOTADO);
        assertThat(produto(COCA).getEstoquePrateleira()).isZero();
    }

    @Test
    void faltaDeEstoqueEmUmItemDesfazOPedidoInteiro() {
        cadastra(COCA, "9.99", 20, 5, 20);
        cadastra(LEITE, "4.50", 1, 1, 10);

        assertThatThrownBy(() -> checkout.fechar(List.of(new ItemCheckout(COCA, 16), new ItemCheckout(LEITE, 2))))
                .isInstanceOf(EstoqueInsuficienteException.class);

        // Nada do checkout sobrevive: nem débito, nem pedido, nem reserva, nem evento.
        assertThat(produto(COCA).getEstoquePrateleira()).isEqualTo(20);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pedido", Integer.class)).isZero();
        assertThat(reservas.count()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_event", Integer.class)).isZero();
    }

    @Test
    void skuForaDaVitrineFalhaOCheckout() {
        assertThatThrownBy(() -> checkout.fechar(List.of(new ItemCheckout(COCA, 1))))
                .isInstanceOf(ProdutoNaoEncontradoException.class);
    }

    @Test
    void checkoutsConcorrentesNaoVendemAlemDaPrateleiraNemDuplicamReposicao() throws Exception {
        cadastra(COCA, "9.99", 5, 2, 10);
        int compradores = 10;
        var largada = new CountDownLatch(1);
        var vendidos = new AtomicInteger();
        var recusados = new AtomicInteger();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> tarefas = new ArrayList<>();
            for (int i = 0; i < compradores; i++) {
                tarefas.add(executor.submit(() -> {
                    largada.await();
                    try {
                        checkout.fechar(List.of(new ItemCheckout(COCA, 1)));
                        vendidos.incrementAndGet();
                    } catch (EstoqueInsuficienteException e) {
                        recusados.incrementAndGet();
                    }
                    return null;
                }));
            }
            largada.countDown();
            for (var tarefa : tarefas) {
                tarefa.get();
            }
        }

        assertThat(vendidos).hasValue(5);
        assertThat(recusados).hasValue(5);
        assertThat(produto(COCA).getEstoquePrateleira()).isZero();
        assertThat(produto(COCA).getStatus()).isEqualTo(StatusVitrine.ESGOTADO);
        assertThat(reservas.count()).isEqualTo(5);
        // Disparou uma única vez, quando a prateleira foi a 1 (< 2): pede 10 - 1.
        assertThat(reposicoesSolicitadas(COCA)).containsExactly(9);
    }
}
