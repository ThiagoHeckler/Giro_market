import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render } from "@testing-library/react";
import type { ReactElement } from "react";

/** Renderiza com um cliente de consultas novo, sem novas tentativas. */
export function renderizar(ui: ReactElement) {
  const consultas = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(<QueryClientProvider client={consultas}>{ui}</QueryClientProvider>);
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
