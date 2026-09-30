package br.com.giro.estoque.infra;

import br.com.giro.estoque.IntegracaoTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A V9 recupera, a partir da outbox, os envios feitos antes de existir {@code reposicao_expedida}.
 * Roda num schema à parte: migra até a V8, grava dados no formato antigo e aplica a V9.
 */
class MigracaoReposicaoExpedidaTest extends IntegracaoTest {

    private static final String SCHEMA = "migracao_v9";

    @Autowired
    DataSource dataSource;

    private Flyway flywayAte(String versao) {
        return Flyway.configure().dataSource(dataSource).schemas(SCHEMA)
                .locations("classpath:db/migration").target(versao).load();
    }

    @AfterEach
    void removeSchema() {
        jdbc.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
    }

    @Test
    void envioAntigoDaOutboxViraReposicaoExpedida() {
        flywayAte("8").migrate();
        var envio = UUID.randomUUID();
        var solicitacao = UUID.randomUUID();
        jdbc.update("INSERT INTO " + SCHEMA + ".produto_estoque (sku, descricao, ncm) VALUES ('7894900011517', 'COCA', '22021000')");
        jdbc.update("""
                INSERT INTO %s.lote (sku, codigo_lote, quantidade, quantidade_disponivel, recebido_em)
                VALUES ('7894900011517', 'L1', 50, 34, now())""".formatted(SCHEMA));
        jdbc.update("""
                INSERT INTO %s.outbox_event (id, tipo, payload, criado_em, proxima_tentativa_em)
                VALUES (?, 'ReposicaoEnviada', ?::jsonb, now(), now()),
                       (?, 'ReposicaoNegada', '{"sku":"7894900011517","motivo":"SEM_SALDO"}'::jsonb, now(), now())
                """.formatted(SCHEMA),
                envio, """
                        {"eventId":"%s","correlationId":"%s","sku":"7894900011517","qtd":16,"lote":"L1"}"""
                        .formatted(envio, solicitacao),
                UUID.randomUUID());

        flywayAte("9").migrate();

        var linha = jdbc.queryForMap("SELECT event_id, correlation_id, sku, qtd FROM " + SCHEMA + ".reposicao_expedida");
        assertThat(linha).containsEntry("event_id", envio)
                .containsEntry("correlation_id", solicitacao)
                .containsEntry("sku", "7894900011517")
                .containsEntry("qtd", 16);
    }
}
