package br.com.giro.estoque;

import br.com.giro.estoque.application.contrato.EventoReposicao;
import br.com.giro.estoque.application.contrato.ReposicaoEnviada;
import br.com.giro.estoque.application.contrato.ReposicaoNegada;
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
import java.util.Map;

/**
 * Base dos testes de integração: um contexto e um container para todos, banco limpo a cada teste.
 * O agendador da outbox fica desligado; os testes chamam o publicador quando querem.
 */
@SpringBootTest(properties = "outbox.publicador-habilitado=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegracaoTest {

    private static final Map<String, Class<? extends EventoReposicao>> TIPOS = Map.of(
            "ReposicaoEnviada", ReposicaoEnviada.class,
            "ReposicaoNegada", ReposicaoNegada.class);

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected JsonMapper json;

    @DynamicPropertySource
    static void destinoDosEventos(DynamicPropertyRegistry registro) {
        registro.add("outbox.destino", DestinoFalso::url);
    }

    @BeforeEach
    void limpaBanco() {
        jdbc.execute("TRUNCATE produto_estoque, lote, demanda_reprimida, outbox_event, inbox_event CASCADE");
        DestinoFalso.reiniciar();
    }

    /** Respostas gravadas na outbox, na ordem, já desserializadas nos contratos. */
    protected List<EventoReposicao> respostasNaOutbox() {
        return jdbc.query("SELECT tipo, payload FROM outbox_event ORDER BY criado_em",
                (linha, _) -> json.readValue(linha.getString("payload"), TIPOS.get(linha.getString("tipo"))));
    }
}
