package br.com.giro.mercado;

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
import java.util.Objects;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/**
 * Base dos testes de integração: um contexto e um container para todos, banco limpo a cada teste.
 * O agendador da outbox fica desligado; os testes chamam o publicador quando querem.
 */
@SpringBootTest(properties = {
        "seguranca.token-servico=" + IntegracaoTest.TOKEN_SERVICO,
        "seguranca.operador.usuario=" + IntegracaoTest.OPERADOR,
        "seguranca.operador.senha=" + IntegracaoTest.SENHA_OPERADOR,
        "outbox.token=" + DestinoFalso.TOKEN,
        "estoque.token=" + DestinoFalso.TOKEN,
        "outbox.publicador-habilitado=false",
        "estoque.timeout=500ms"})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegracaoTest {

    /** Token que o mercado exige do estoque nos testes. */
    protected static final String TOKEN_SERVICO = "token-do-mercado-nos-testes-0123456789abcdef";
    protected static final String OPERADOR = "operador";
    protected static final String SENHA_OPERADOR = "senha-de-teste";

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected JsonMapper json;

    @DynamicPropertySource
    static void destinoDosEventos(DynamicPropertyRegistry registro) {
        registro.add("outbox.destino", DestinoFalso::url);
        registro.add("estoque.url", DestinoFalso::url);
    }

    /** Chamada de outro serviço, com o token de serviço do mercado. */
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
            cookies.add(new Cookie("XSRF-MERCADO", token));
            requisicao.setCookies(cookies.toArray(Cookie[]::new));
            requisicao.addHeader("X-XSRF-TOKEN", token);
            return requisicao;
        };
    }

    /** Chamada do operador já logado (sessão). */
    protected static RequestPostProcessor comoOperador() {
        return user(OPERADOR).roles("OPERADOR");
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
