package br.com.giro.mercado;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Base dos testes de integração: um contexto e um container para todos, banco limpo a cada teste.
 * O agendador da outbox fica desligado; os testes chamam o publicador quando querem.
 */
@SpringBootTest(properties = {"outbox.publicador-habilitado=false", "estoque.timeout=500ms"})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegracaoTest {

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected JsonMapper json;

    @DynamicPropertySource
    static void destinoDosEventos(DynamicPropertyRegistry registro) {
        registro.add("outbox.destino", DestinoFalso::url);
        registro.add("estoque.url", DestinoFalso::url);
    }

    @BeforeEach
    void limpaBanco() {
        jdbc.execute("""
                TRUNCATE produto_vitrine, pedido, item_pedido, reserva,
                         solicitacao_reposicao, outbox_event, inbox_event CASCADE
                """);
        DestinoFalso.reiniciar();
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
