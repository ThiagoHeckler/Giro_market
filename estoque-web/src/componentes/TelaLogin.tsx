import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";
import { estoque, naoAutenticado } from "../api/estoque";
import { CHAVE_SESSAO } from "../consultas";
import { MarcaGiro } from "./Logo";

export function TelaLogin() {
  const [usuario, setUsuario] = useState("");
  const [senha, setSenha] = useState("");
  const consultas = useQueryClient();

  const entrar = useMutation({
    mutationFn: () => estoque.entrar(usuario.trim(), senha),
    // Reconsultar a sessão também traz o token CSRF novo (o login troca o anterior).
    onSuccess: () => consultas.invalidateQueries({ queryKey: CHAVE_SESSAO }),
  });

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    entrar.mutate();
  }

  return (
    <main className="entrada">
      <form className="cartao entrada__cartao" aria-labelledby="titulo-login" onSubmit={enviar}>
        <div className="entrada__marca">
          <MarcaGiro />
          <div className="lateral__nomes">
            <span className="lateral__nome">Girô</span>
            <span className="lateral__sistema">Estoque</span>
          </div>
        </div>
        <h1 id="titulo-login" className="cartao__titulo">Entrar no painel</h1>

        <div className="campo">
          <label htmlFor="usuario">Usuário</label>
          <input
            id="usuario"
            autoComplete="username"
            autoCapitalize="none"
            spellCheck={false}
            required
            value={usuario}
            onChange={(e) => setUsuario(e.target.value)}
          />
        </div>
        <div className="campo">
          <label htmlFor="senha">Senha</label>
          <input
            id="senha"
            type="password"
            autoComplete="current-password"
            required
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
          />
        </div>

        <button type="submit" className="botao botao--primario botao--grande" disabled={entrar.isPending}>
          {entrar.isPending ? "Entrando…" : "Entrar"}
        </button>

        {entrar.isError && (
          <p className="aviso aviso--erro" role="alert">
            {naoAutenticado(entrar.error)
              ? "Usuário ou senha inválidos."
              : "Não foi possível entrar. Verifique a conexão com o estoque-service."}
          </p>
        )}
      </form>
    </main>
  );
}
