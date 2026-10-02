import { QueryClientProvider } from "@tanstack/react-query";
import { render } from "@testing-library/react";
import type { ReactElement } from "react";
import { COOKIE_CSRF } from "../api/estoque";
import { criarClienteConsultas } from "../consultas";

/** Renderiza com um cliente de consultas novo (com o tratamento de 401 do app), sem novas tentativas. */
export function renderizar(ui: ReactElement) {
  const consultas = criarClienteConsultas({ queries: { retry: false }, mutations: { retry: false } });
  return render(<QueryClientProvider client={consultas}>{ui}</QueryClientProvider>);
}

export const OPERADOR = { usuario: "operador" };

export interface Chamada {
  metodo: string;
  url: string;
  corpo?: unknown;
  cabecalhos: Record<string, string>;
}

/** Como o estoque: GET /sessao entrega o cookie do token CSRF. */
function sessaoComCookie(): Response {
  document.cookie = `${COOKIE_CSRF}=token-csrf`;
  return json(OPERADOR);
}

function lerCorpo(corpo: RequestInit["body"]): unknown {
  if (corpo instanceof URLSearchParams) return Object.fromEntries(corpo);
  return corpo ? JSON.parse(String(corpo)) : undefined;
}

/**
 * Substitui o fetch global por respostas por rota ("METODO /caminho"). GET /api/sessao responde com
 * o operador logado, a menos que a rota seja sobrescrita.
 */
export function simularApi(rotas: Record<string, () => Response>) {
  const tabela: Record<string, () => Response> = { "GET /api/sessao": sessaoComCookie, ...rotas };
  const chamadas: Chamada[] = [];
  vi.stubGlobal("fetch", vi.fn(async (url: string, init?: RequestInit) => {
    const metodo = init?.method ?? "GET";
    chamadas.push({
      metodo,
      url,
      corpo: lerCorpo(init?.body),
      cabecalhos: (init?.headers ?? {}) as Record<string, string>,
    });
    const resposta = tabela[`${metodo} ${url}`];
    if (!resposta) throw new Error(`rota não simulada: ${metodo} ${url}`);
    return resposta();
  }));
  return chamadas;
}

export const json = (corpo: unknown, status = 200) =>
  new Response(JSON.stringify(corpo), { status, headers: { "Content-Type": "application/json" } });

export const semConteudo = () => new Response(null, { status: 204 });
