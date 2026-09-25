import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render } from "@testing-library/react";
import type { ReactElement } from "react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import type { Produto } from "../api/mercado";
import { ProvedorCarrinho } from "../carrinho/Carrinho";
import type { ItemCarrinho } from "../carrinho/estado";

export function produto(parcial: Partial<Produto> = {}): Produto {
  return {
    sku: "7894900011517",
    nome: "Coca-Cola 2L PET",
    preco: 8.99,
    status: "DISPONIVEL",
    disponivel: 10,
    categoria: "bebidas",
    tags: ["refrigerante"],
    ...parcial,
  };
}

interface Opcoes {
  rota?: string;
  caminho?: string;
  carrinho?: ItemCarrinho[];
}

/** Renderiza com os provedores reais (consultas, rotas, carrinho), com o carrinho pré-carregado. */
export function renderizar(ui: ReactElement, { rota = "/", caminho = "*", carrinho = [] }: Opcoes = {}) {
  window.localStorage.setItem("giro.carrinho", JSON.stringify(carrinho));
  const consultas = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={consultas}>
      <MemoryRouter initialEntries={[rota]}>
        <ProvedorCarrinho>
          <Routes>
            <Route path={caminho} element={ui} />
            <Route path="/pedidos/:id" element={<p>página do pedido</p>} />
          </Routes>
        </ProvedorCarrinho>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

/** Substitui o fetch global por respostas por rota ("METODO /caminho"). */
export function simularApi(rotas: Record<string, () => Response>) {
  const chamadas: { metodo: string; url: string; corpo?: unknown }[] = [];
  vi.stubGlobal("fetch", vi.fn(async (url: string, init?: RequestInit) => {
    const metodo = init?.method ?? "GET";
    chamadas.push({ metodo, url, corpo: init?.body ? JSON.parse(String(init.body)) : undefined });
    const resposta = rotas[`${metodo} ${url}`];
    if (!resposta) throw new Error(`rota não simulada: ${metodo} ${url}`);
    return resposta();
  }));
  return chamadas;
}

export const json = (corpo: unknown, status = 200) =>
  new Response(JSON.stringify(corpo), { status, headers: { "Content-Type": "application/json" } });
