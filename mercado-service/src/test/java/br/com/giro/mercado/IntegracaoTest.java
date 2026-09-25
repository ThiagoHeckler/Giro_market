package br.com.giro.mercado;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/** Base dos testes de integração: contexto e container compartilhados, banco limpo a cada teste. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
public abstract class IntegracaoTest {

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void limpaBanco() {
        jdbc.execute("""
                TRUNCATE produto_vitrine, pedido, item_pedido, reserva,
                         solicitacao_reposicao, outbox_event, inbox_event CASCADE
                """);
    }

    /** Valores de {@code qtdFaltante} das ReposicaoSolicitada gravadas na outbox para o SKU. */
    protected List<Integer> reposicoesSolicitadas(String sku) {
        return jdbc.queryForList("""
                SELECT (payload ->> 'qtdFaltante')::int FROM outbox_event
                WHERE tipo = 'ReposicaoSolicitada' AND payload ->> 'sku' = ?
                ORDER BY criado_em
                """, Integer.class, sku);
    }
}
