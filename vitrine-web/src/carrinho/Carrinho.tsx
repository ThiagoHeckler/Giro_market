import { createContext, useContext, useEffect, useMemo, useReducer, type ReactNode } from "react";
import { lerCarrinhoSalvo, reduzirCarrinho, type AcaoCarrinho, type ItemCarrinho } from "./estado";

const CHAVE = "giro.carrinho";

interface ValorCarrinho {
  itens: ItemCarrinho[];
  despachar: (acao: AcaoCarrinho) => void;
}

const ContextoCarrinho = createContext<ValorCarrinho | null>(null);

function carregar(): ItemCarrinho[] {
  try {
    return lerCarrinhoSalvo(window.localStorage.getItem(CHAVE));
  } catch {
    return []; // armazenamento bloqueado (aba privada, política do navegador)
  }
}

export function ProvedorCarrinho({ children }: { children: ReactNode }) {
  const [itens, despachar] = useReducer(reduzirCarrinho, undefined, carregar);

  useEffect(() => {
    try {
      window.localStorage.setItem(CHAVE, JSON.stringify(itens));
    } catch {
      // sem persistência: o carrinho vive só nesta aba
    }
  }, [itens]);

  const valor = useMemo(() => ({ itens, despachar }), [itens]);
  return <ContextoCarrinho.Provider value={valor}>{children}</ContextoCarrinho.Provider>;
}

export function useCarrinho(): ValorCarrinho {
  const valor = useContext(ContextoCarrinho);
  if (!valor) throw new Error("useCarrinho fora do ProvedorCarrinho");
  return valor;
}
