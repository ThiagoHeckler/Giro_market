import type { ReactNode } from "react";
import { ErroApi } from "../api/mercado";

export function Carregando({ rotulo = "Carregando…" }: { rotulo?: string }) {
  return (
    <p className="estado" role="status">
      {rotulo}
    </p>
  );
}

export function FalhaAoCarregar({ erro, aoTentarDeNovo }: { erro: unknown; aoTentarDeNovo: () => void }) {
  const detalhe = erro instanceof ErroApi && erro.status >= 500
    ? "O mercado não respondeu."
    : "Verifique sua conexão.";
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
