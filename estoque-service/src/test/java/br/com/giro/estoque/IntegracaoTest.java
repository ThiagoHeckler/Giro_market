package br.com.giro.estoque;

import br.com.giro.estoque.application.contrato.EventoIntegracao;
import br.com.giro.estoque.application.contrato.ProdutoClassificado;
import br.com.giro.estoque.application.contrato.ReposicaoEnviada;
import br.com.giro.estoque.application.contrato.ReposicaoNegada;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/**
 * Base dos testes de integração: um contexto e um container para todos, banco limpo a cada teste.
 * Agendadores desligados (os testes chamam os jobs quando querem); worker de tags e destino dos
 * eventos são servidores HTTP falsos.
 */
@SpringBootTest(properties = {
        "seguranca.token-servico=" + IntegracaoTest.TOKEN_SERVICO,
        "seguranca.operador.usuario=" + IntegracaoTest.OPERADOR,
        "seguranca.operador.senha=" + IntegracaoTest.SENHA_OPERADOR,
        "outbox.token=" + DestinoFalso.TOKEN,
        "outbox.publicador-habilitado=false",
        "tags.reclassificacao-habilitada=false",
        "tags.timeout=500ms"})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegracaoTest {

    /** Token que o estoque exige do mercado nos testes. */
    protected static final String TOKEN_SERVICO = "token-do-estoque-nos-testes-0123456789abcdef";
    protected static final String OPERADOR = "operador";
    protected static final String SENHA_OPERADOR = "senha-de-teste";

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
        jdbc.execute("TRUNCATE produto_estoque, lote, demanda_reprimida, reposicao_expedida, outbox_event, inbox_event CASCADE");
        DestinoFalso.reiniciar();
        WorkerFalso.reiniciar();
    }

    /** Chamada de outro serviço, com o token de serviço do estoque. */
    protected static RequestPostProcessor comoServico() {
        return requisicao -> {
            requisicao.addHeader("Authorization", "Bearer " + TOKEN_SERVICO);
            return requisicao;
        };
    }

    /**
     * Token CSRF como o front manda: o valor do cookie repetido no header (double-submit). Não usa o
     * {@code csrf()} do spring-security-test, que troca para sempre o repositório do filtro (compartilhado
     * entre os testes) por um de sessão, e o cookie deixaria de ser testado.
     */
    protected static RequestPostProcessor comCsrf() {
        return requisicao -> {
            var token = UUID.randomUUID().toString();
            var cookies = new ArrayList<>(Arrays.asList(Objects.requireNonNullElse(requisicao.getCookies(), new Cookie[0])));
            cookies.add(new Cookie("XSRF-ESTOQUE", token));
            requisicao.setCookies(cookies.toArray(Cookie[]::new));
            requisicao.addHeader("X-XSRF-TOKEN", token);
            return requisicao;
        };
    }

    /** Chamada do operador já logado (sessão). */
    protected static RequestPostProcessor comoOperador() {
        return user(OPERADOR).roles("OPERADOR");
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
