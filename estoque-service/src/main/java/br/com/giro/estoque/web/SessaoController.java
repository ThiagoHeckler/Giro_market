package br.com.giro.estoque.web;

import br.com.giro.estoque.infra.seguranca.Papel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sessão do operador. O login ({@code POST /sessao}) e o logout ({@code DELETE /sessao}) são do
 * Spring Security; aqui só a consulta de quem está logado.
 */
@RestController
@RequestMapping("/sessao")
public class SessaoController {

    public record Sessao(String usuario) {
    }

    /** Também entrega o cookie do token CSRF, que o front precisa antes do primeiro POST (o próprio login). */
    @GetMapping
    public ResponseEntity<?> atual(Authentication autenticacao, CsrfToken csrf) {
        csrf.getToken(); // o token é carregado sob demanda: lê-lo é o que grava o cookie
        var operador = autenticacao != null && autenticacao.getAuthorities().stream()
                .anyMatch(a -> Papel.OPERADOR.autoridade().equals(a.getAuthority()));
        if (!operador) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "nenhum operador logado"));
        }
        return ResponseEntity.ok(new Sessao(autenticacao.getName()));
    }
}
