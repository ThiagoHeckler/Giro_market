package br.com.giro.estoque.infra;

import br.com.giro.estoque.TestcontainersConfiguration;
import br.com.giro.estoque.domain.ProdutoEstoque;
import br.com.giro.estoque.infra.inbox.InboxEvent;
import br.com.giro.estoque.infra.inbox.InboxEventRepository;
import br.com.giro.estoque.infra.outbox.OutboxEvent;
import br.com.giro.estoque.infra.outbox.OutboxEventRepository;
import br.com.giro.estoque.infra.outbox.StatusOutbox;
import br.com.giro.estoque.infra.persistencia.ProdutoEstoqueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prova que a fundação está de pé: o Flyway cria o schema num Postgres limpo,
 * o Hibernate o valida (ddl-auto=validate) e as entidades fazem o ida-e-volta no banco.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FundacaoPersistenciaTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ProdutoEstoqueRepository produtos;

    @Autowired
    OutboxEventRepository outbox;

    @Autowired
    InboxEventRepository inbox;

    @Test
    void flywayAplicaTodasAsMigrationsDoZero() {
        List<String> aplicadas = jdbc.queryForList(
                "SELECT script FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);

        assertThat(aplicadas).containsExactly(
                "V1__cria_produto_estoque.sql",
                "V2__cria_lote.sql",
                "V3__cria_demanda_reprimida.sql",
                "V4__cria_outbox_event.sql",
                "V5__cria_inbox_event.sql",
                "V6__adiciona_saldo_por_lote.sql");
    }

    @Test
    void gravaELeProdutoEstoque() {
        var produto = new ProdutoEstoque("7894900011517", "COCA COLA 2L PET", "22021000");
        produto.classificar(List.of("bebida", "refrigerante", "2l"));

        produtos.save(produto);

        // findById fora de transação usa um novo EntityManager: a leitura vem do banco, não do cache.
        var lido = produtos.findById("7894900011517").orElseThrow();
        assertThat(lido.getDescricao()).isEqualTo("COCA COLA 2L PET");
        assertThat(lido.getNcm()).isEqualTo("22021000");
        assertThat(lido.getSaldoDisponivel()).isZero();
        assertThat(lido.getTags()).contains(List.of("bebida", "refrigerante", "2l"));
    }

    @Test
    void produtoSemTagsFicaPendenteDeClassificacao() {
        // Worker de tags fora do ar: o produto é salvo mesmo assim, com tags NULL.
        produtos.save(new ProdutoEstoque("7891000100103", "LEITE INTEGRAL 1L", "04012010"));

        assertThat(produtos.findById("7891000100103").orElseThrow().getTags()).isEmpty();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM produto_estoque WHERE sku = '7891000100103' AND tags IS NULL", Integer.class))
                .isOne();
    }

    @Test
    void outboxGravaPayloadJsonComoPendente() {
        var eventId = UUID.randomUUID();
        var agora = Instant.now().truncatedTo(ChronoUnit.MICROS);
        outbox.save(new OutboxEvent(eventId, "ReposicaoEnviada", "{\"sku\":\"7894900011517\",\"qtd\":12}", agora));

        var lido = outbox.findById(eventId).orElseThrow();
        assertThat(lido.getStatus()).isEqualTo(StatusOutbox.PENDING);
        assertThat(lido.getCriadoEm()).isEqualTo(agora);
        assertThat(jdbc.queryForObject(
                "SELECT payload ->> 'qtd' FROM outbox_event WHERE id = ?", String.class, eventId))
                .isEqualTo("12");
    }

    @Test
    void inboxRejeitaEventoDuplicado() {
        var eventId = UUID.randomUUID();
        inbox.save(new InboxEvent(eventId, Instant.now()));

        assertThatThrownBy(() -> inbox.saveAndFlush(new InboxEvent(eventId, Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
