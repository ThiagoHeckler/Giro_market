package br.com.giro.estoque.infra.seguranca;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * Dois jeitos de entrar: o mercado com token de serviço (stateless, sem CSRF: não usa cookie) e o
 * operador do painel com sessão em cookie HttpOnly + token CSRF (padrão SPA: cookie legível pelo
 * front, devolvido no header {@code X-XSRF-TOKEN}). Toda rota não listada é negada.
 */
@Configuration
public class SegurancaConfig {

    /** Cookie próprio: em localhost os cookies não separam por porta, e o mercado tem o dele. */
    static final String COOKIE_CSRF = "XSRF-ESTOQUE";

    private final JsonMapper json;

    public SegurancaConfig(JsonMapper json) {
        this.json = json;
    }

    @Bean
    SecurityFilterChain filtros(HttpSecurity http, SegurancaProperties seguranca) {
        var tokensCsrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        tokensCsrf.setCookieName(COOKIE_CSRF);
        tokensCsrf.setCookieCustomizer(cookie -> cookie.sameSite("Strict"));
        var sessao = PathPatternRequestMatcher.withDefaults();

        return http
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/sessao").permitAll()
                        .requestMatchers(HttpMethod.POST, "/eventos").hasRole(Papel.SERVICO.name())
                        // Consulta de produto: o mercado no cadastro e o painel na entrada de lote.
                        .requestMatchers(HttpMethod.GET, "/produtos/*")
                        .hasAnyRole(Papel.SERVICO.name(), Papel.OPERADOR.name())
                        .requestMatchers("/lotes", "/painel/**").hasRole(Papel.OPERADOR.name())
                        .anyRequest().denyAll())
                .addFilterBefore(new TokenServicoFilter(seguranca.tokenServico()), UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf.spa().csrfTokenRepository(tokensCsrf).ignoringRequestMatchers("/eventos"))
                .formLogin(login -> login
                        // loginPage próprio desliga a página HTML gerada pelo Spring; o front tem a sua.
                        .loginPage("/sessao")
                        .loginProcessingUrl("/sessao")
                        .usernameParameter("usuario")
                        .passwordParameter("senha")
                        .successHandler((_, resposta, _) -> resposta.setStatus(HttpStatus.NO_CONTENT.value()))
                        .failureHandler((_, resposta, _) ->
                                problema(resposta, HttpStatus.UNAUTHORIZED, "usuário ou senha inválidos")))
                .logout(logout -> logout
                        .logoutRequestMatcher(sessao.matcher(HttpMethod.DELETE, "/sessao"))
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .exceptionHandling(erros -> erros
                        // 401 sem WWW-Authenticate: o navegador não abre o diálogo de login nativo.
                        .authenticationEntryPoint((_, resposta, _) ->
                                problema(resposta, HttpStatus.UNAUTHORIZED, "autenticação necessária"))
                        .accessDeniedHandler((_, resposta, _) ->
                                problema(resposta, HttpStatus.FORBIDDEN, "acesso negado")))
                .requestCache(cache -> cache.disable())
                .build();
    }

    @Bean
    PasswordEncoder codificadorDeSenha() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService operadores(SegurancaProperties seguranca, PasswordEncoder codificador) {
        var operador = seguranca.operador();
        return new InMemoryUserDetailsManager(User.withUsername(operador.usuario())
                .password(codificador.encode(operador.senha()))
                .roles(Papel.OPERADOR.name())
                .build());
    }

    private void problema(HttpServletResponse resposta, HttpStatus status, String detalhe) throws IOException {
        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        json.writeValue(resposta.getOutputStream(), ProblemDetail.forStatusAndDetail(status, detalhe));
    }
}
