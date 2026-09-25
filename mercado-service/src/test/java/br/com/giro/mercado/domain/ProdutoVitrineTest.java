package br.com.giro.mercado.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Regras puras da prateleira e do gatilho min/max — sem Spring, sem banco. */
class ProdutoVitrineTest {

    private static ProdutoVitrine produto(int prateleira, int minimo, int ideal) {
        return new ProdutoVitrine("7894900011517", "COCA COLA 2L PET", new BigDecimal("9.99"), prateleira, minimo, ideal);
    }

    @Test
    void abaixoDoMinimoSemPendentePedeADiferencaAteOIdeal() {
        var p = produto(10, 5, 20);
        p.debitar(6);

        assertThat(p.getEstoquePrateleira()).isEqualTo(4);
        assertThat(p.quantidadeARepor(false)).hasValue(16);
    }

    @Test
    void noMinimoAindaNaoRepoe() {
        var p = produto(5, 5, 20);

        assertThat(p.quantidadeARepor(false)).isEmpty();
    }

    @Test
    void comSolicitacaoPendenteNaoPedeDeNovo() {
        var p = produto(1, 5, 20);

        assertThat(p.quantidadeARepor(true)).isEmpty();
    }

    @Test
    void debitarAteZeroEsgota() {
        var p = produto(2, 1, 10);
        p.debitar(2);

        assertThat(p.getStatus()).isEqualTo(StatusVitrine.ESGOTADO);
        assertThat(p.quantidadeARepor(false)).hasValue(10);
    }

    @Test
    void naoVendeMaisDoQueAPrateleiraTem() {
        var p = produto(1, 1, 10);

        assertThatThrownBy(() -> p.debitar(2)).isInstanceOf(EstoqueInsuficienteException.class);
        assertThat(p.getEstoquePrateleira()).isEqualTo(1);
    }

    @Test
    void creditoReabreProdutoEsgotado() {
        var p = produto(0, 1, 10);
        assertThat(p.getStatus()).isEqualTo(StatusVitrine.ESGOTADO);

        p.creditar(10);

        assertThat(p.getStatus()).isEqualTo(StatusVitrine.DISPONIVEL);
        assertThat(p.getEstoquePrateleira()).isEqualTo(10);
    }

    @Test
    void negativaSoEsgotaComPrateleiraVazia() {
        var comSobra = produto(3, 5, 20);
        comSobra.registrarReposicaoNegada();
        assertThat(comSobra.getStatus()).isEqualTo(StatusVitrine.DISPONIVEL);

        var vazio = produto(1, 5, 20);
        vazio.debitar(1);
        vazio.registrarReposicaoNegada();
        assertThat(vazio.getStatus()).isEqualTo(StatusVitrine.ESGOTADO);
    }

    @Test
    void rejeitaMinMaxIncoerente() {
        assertThatThrownBy(() -> produto(10, 0, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> produto(10, 21, 20)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void classificacaoMaisAntigaNaoSobrescreveAMaisNova() {
        var p = produto(10, 5, 20);
        var agora = Instant.parse("2026-09-25T12:00:00Z");

        assertThat(p.classificar(List.of("refrigerante"), "bebidas", agora)).isTrue();
        assertThat(p.classificar(List.of("errada"), "outros", agora.minusSeconds(60))).isFalse();

        assertThat(p.getTags()).contains(List.of("refrigerante"));
        assertThat(p.getCategoria()).contains("bebidas");
    }

    @Test
    void classificacaoNaoMexeEmPrateleiraNemStatus() {
        var p = produto(3, 5, 20);

        p.classificar(List.of("refrigerante"), "bebidas", Instant.now());

        assertThat(p.getEstoquePrateleira()).isEqualTo(3);
        assertThat(p.getStatus()).isEqualTo(StatusVitrine.DISPONIVEL);
        assertThat(p.quantidadeARepor(false)).hasValue(17);
    }
}
