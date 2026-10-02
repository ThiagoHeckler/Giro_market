package br.com.giro.mercado.infra;

import br.com.giro.mercado.IntegracaoTest;
import br.com.giro.mercado.application.contrato.ReposicaoSolicitada;
import br.com.giro.mercado.infra.outbox.Outbox;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxInboxTest extends IntegracaoTest {

    @Autowired
    Outbox outbox;

    @Autowired
    TransactionTemplate transacao;

    @Test
    void flywayAplicaTodasAsMigrationsDoZero() {
        assertThat(jdbc.queryForList(
                "SELECT script FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class))
                .containsExactly(
                        "V1__cria_produto_vitrine.sql",
                        "V2__cria_pedido.sql",
                        "V3__cria_reserva.sql",
                        "V4__cria_solicitacao_reposicao.sql",
                        "V5__cria_outbox_event.sql",
                        "V6__cria_inbox_event.sql",
                        "V7__adiciona_controle_de_envio_ao_outbox.sql",
                        "V8__adiciona_classificacao_ao_produto_vitrine.sql",
                        "V9__adiciona_status_expirado_ao_pedido.sql");
    }

    @Test
    void outboxRecusaEventoForaDeTransacaoDeNegocio() {
        var evento = new ReposicaoSolicitada(UUID.randomUUID(), "7894900011517", 16, Instant.now());

        assertThatThrownBy(() -> outbox.registrar(evento)).isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void outboxSerializaOContratoComoJson() {
        var evento = new ReposicaoSolicitada(UUID.randomUUID(), "7894900011517", 16,
                Instant.parse("2026-09-25T12:00:00Z"));

        transacao.executeWithoutResult(_ -> outbox.registrar(evento));

        var linha = jdbc.queryForMap("""
                SELECT tipo, status, payload ->> 'eventId' AS event_id, payload ->> 'ocorridoEm' AS ocorrido_em
                FROM outbox_event WHERE id = ?
                """, evento.eventId());
        assertThat(linha).containsEntry("tipo", "ReposicaoSolicitada")
                .containsEntry("status", "PENDING")
                .containsEntry("event_id", evento.eventId().toString())
                .containsEntry("ocorrido_em", "2026-09-25T12:00:00Z");
    }

    @Test
    void solicitacaoEmAbertoEhUnicaPorSkuNoBanco() {
        jdbc.update("""
                INSERT INTO produto_vitrine (sku, nome, preco, estoque_prateleira, estoque_minimo, estoque_ideal, status)
                VALUES ('7894900011517', 'COCA', 9.99, 1, 5, 20, 'DISPONIVEL')""");
        var insere = "INSERT INTO solicitacao_reposicao (event_id, sku, qtd_solicitada, status, criado_em) "
                + "VALUES (?, '7894900011517', 19, ?, now())";
        jdbc.update(insere, UUID.randomUUID(), "ATENDIDA");
        jdbc.update(insere, UUID.randomUUID(), "PENDENTE");

        assertThatThrownBy(() -> jdbc.update(insere, UUID.randomUUID(), "AGUARDANDO_LOTE"))
                .hasMessageContaining("uk_solicitacao_reposicao_ativa_por_sku");
    }
}
