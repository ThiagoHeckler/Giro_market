package br.com.giro.estoque.infra.seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Autentica a chamada de outro serviço: {@code Authorization: Bearer <token>} igual ao token deste
 * serviço vira o papel {@code SERVICO}, só nesta requisição (nunca cria sessão). Sem o header, ou com
 * token errado, segue sem autenticar e a autorização decide (401 nas rotas de serviço).
 */
final class TokenServicoFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TokenServicoFilter.class);
    private static final String PREFIXO = "Bearer ";

    private final byte[] token;
    private final SecurityContextHolderStrategy contextos = SecurityContextHolder.getContextHolderStrategy();

    TokenServicoFilter(String token) {
        this.token = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        var cabecalho = requisicao.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho != null && cabecalho.startsWith(PREFIXO)) {
            var recebido = cabecalho.substring(PREFIXO.length()).getBytes(StandardCharsets.UTF_8);
            // Comparação em tempo constante: não vaza por tempo de resposta quantos bytes acertou.
            if (MessageDigest.isEqual(token, recebido)) {
                var contexto = contextos.createEmptyContext();
                contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                        "servico", null, AuthorityUtils.createAuthorityList(Papel.SERVICO.autoridade())));
                contextos.setContext(contexto);
            } else {
                log.warn("Token de serviço inválido metodo={} caminho={} origem={}",
                        requisicao.getMethod(), requisicao.getRequestURI(), requisicao.getRemoteAddr());
            }
        }
        cadeia.doFilter(requisicao, resposta);
    }
}
