package br.com.giro.mercado.infra.seguranca;

/** Quem fala com o mercado além do cliente anônimo da vitrine: outro serviço (token) ou o operador (sessão). */
public enum Papel {
    SERVICO,
    OPERADOR;

    public String autoridade() {
        return "ROLE_" + name();
    }
}
