package br.com.giro.estoque.infra.seguranca;

/** Quem fala com o estoque: outro serviço (token) ou o operador do painel (sessão). */
public enum Papel {
    SERVICO,
    OPERADOR;

    public String autoridade() {
        return "ROLE_" + name();
    }
}
