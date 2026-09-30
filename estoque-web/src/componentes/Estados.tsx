import type { ReactNode } from "react";
import { ErroApi } from "../api/estoque";

export function Carregando({ rotulo = "Carregando…" }: { rotulo?: string }) {
  return (
    <p className="estado" role="status">
      {rotulo}
    </p>
  );
}

export function FalhaAoCarregar({ erro, aoTentarDeNovo }: { erro: unknown; aoTentarDeNovo: () => void }) {
  const detalhe = erro instanceof ErroApi && erro.status >= 500
    ? "O estoque não respondeu."
    : "Verifique a conexão com o estoque-service.";
  return (
    <div className="estado estado--erro" role="alert">
      <p>Não foi possível carregar. {detalhe}</p>
      <button type="button" className="botao botao--secundario" onClick={aoTentarDeNovo}>
        Tentar de novo
      </button>
    </div>
  );
}

export function Vazio({ children }: { children: ReactNode }) {
  return <div className="estado">{children}</div>;
}
