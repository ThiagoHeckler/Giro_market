package br.com.giro.estoque;

import br.com.giro.estoque.application.contrato.EventoIntegracao;
import br.com.giro.estoque.application.contrato.ProdutoClassificado;
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
 * Agendadores desligados (os testes chamam os jobs quando querem); worker de tags e destino dos
 * eventos são servidores HTTP falsos.
 */
@SpringBootTest(properties = {
        "outbox.publicador-habilitado=false",
        "tags.reclassificacao-habilitada=false",
        "tags.timeout=500ms"})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegracaoTest {

    private static final Map<String, Class<? extends EventoIntegracao>> TIPOS = Map.of(
            "ReposicaoEnviada", ReposicaoEnviada.class,
            "ReposicaoNegada", ReposicaoNegada.class,
            "ProdutoClassificado", ProdutoClassificado.class);

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected JsonMapper json;

    @DynamicPropertySource
    static void destinoDosEventos(DynamicPropertyRegistry registro) {
        registro.add("outbox.destino", DestinoFalso::url);
        registro.add("tags.worker-url", WorkerFalso::url);
    }

    @BeforeEach
    void limpaBanco() {
        jdbc.execute("TRUNCATE produto_estoque, lote, demanda_reprimida, outbox_event, inbox_event CASCADE");
        DestinoFalso.reiniciar();
        WorkerFalso.reiniciar();
    }

    /** Todos os eventos gravados na outbox, na ordem, já desserializados nos contratos. */
    protected List<EventoIntegracao> eventosNaOutbox() {
        return jdbc.query("SELECT tipo, payload FROM outbox_event ORDER BY criado_em",
                (linha, _) -> json.readValue(linha.getString("payload"), TIPOS.get(linha.getString("tipo"))));
    }

    /** Só as respostas de reposição (enviada/negada), na ordem. */
    protected List<EventoIntegracao> respostasNaOutbox() {
        return eventosNaOutbox().stream().filter(e -> !(e instanceof ProdutoClassificado)).toList();
    }

    protected List<ProdutoClassificado> classificacoesNaOutbox() {
        return eventosNaOutbox().stream()
                .filter(ProdutoClassificado.class::isInstance).map(ProdutoClassificado.class::cast).toList();
    }
}
