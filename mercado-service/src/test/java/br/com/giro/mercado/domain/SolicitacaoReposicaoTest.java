package br.com.giro.mercado.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SolicitacaoReposicaoTest {

    private static SolicitacaoReposicao nova() {
        return new SolicitacaoReposicao(UUID.randomUUID(), "7894900011517", 16, Instant.now());
    }

    @Test
    void negadaPorFaltaDeSaldoSegueEmAbertoAteChegarLote() {
        var s = nova();
        s.aguardarLote();
        assertThat(s.getStatus().emAberto()).isTrue();

        s.atender();
        assertThat(s.getStatus()).isEqualTo(StatusSolicitacao.ATENDIDA);
        assertThat(s.getStatus().emAberto()).isFalse();
    }

    @Test
    void solicitacaoEncerradaNaoMudaDeEstado() {
        var s = nova();
        s.cancelar();

        assertThatThrownBy(s::atender).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(s::aguardarLote).isInstanceOf(IllegalStateException.class);
    }
}
