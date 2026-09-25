/** Cliente da API do mercado-service. Em dev, /api passa pelo proxy do Vite. */

export type StatusVitrine = "DISPONIVEL" | "ESGOTADO";

export interface Produto {
  sku: string;
  nome: string;
  preco: number;
  status: StatusVitrine;
  /** Unidades na prateleira agora. */
  disponivel: number;
  categoria: string | null;
  tags: string[];
}

export interface ItemCheckout {
  sku: string;
  qtd: number;
}

export interface PedidoFechado {
  pedidoId: string;
  total: number;
  reservaExpiraEm: string;
}

export interface PedidoDetalhe {
  id: string;
  status: "AGUARDANDO_PAGAMENTO" | "PAGO" | "CANCELADO";
  total: number;
  criadoEm: string;
  reservaExpiraEm: string | null;
  itens: { sku: string; nome: string; qtd: number; precoUnitario: number }[];
}

/** ProblemDetail (RFC 9457) devolvido pelo mercado; {@code sku} vem no 409 de estoque insuficiente. */
export interface Problema {
  status: number;
  title?: string;
  detail?: string;
  sku?: string;
}

export class ErroApi extends Error {
  constructor(readonly problema: Problema) {
    super(problema.detail ?? `HTTP ${problema.status}`);
    this.name = "ErroApi";
  }

  get status(): number {
    return this.problema.status;
  }
}

async function requisitar<T>(caminho: string, init?: RequestInit): Promise<T> {
  const resposta = await fetch(`/api${caminho}`, {
    ...init,
    headers: {
      Accept: "application/json",
      ...(init?.body ? { "Content-Type": "application/json" } : {}),
    },
  });
  if (!resposta.ok) {
    let corpo: Partial<Problema> = {};
    try {
      corpo = await resposta.json();
    } catch {
      // corpo vazio ou não-JSON: fica só o status
    }
    throw new ErroApi({ ...corpo, status: resposta.status });
  }
  return resposta.json() as Promise<T>;
}

export interface FiltroVitrine {
  categoria?: string;
  busca?: string;
}

export const mercado = {
  listarProdutos(filtro: FiltroVitrine = {}): Promise<Produto[]> {
    const parametros = new URLSearchParams();
    if (filtro.categoria) parametros.set("categoria", filtro.categoria);
    if (filtro.busca) parametros.set("busca", filtro.busca);
    const consulta = parametros.toString();
    return requisitar(`/produtos${consulta ? `?${consulta}` : ""}`);
  },

  buscarProduto(sku: string): Promise<Produto> {
    return requisitar(`/produtos/${encodeURIComponent(sku)}`);
  },

  fecharPedido(itens: ItemCheckout[]): Promise<PedidoFechado> {
    return requisitar("/pedidos", { method: "POST", body: JSON.stringify({ itens }) });
  },

  buscarPedido(id: string): Promise<PedidoDetalhe> {
    return requisitar(`/pedidos/${encodeURIComponent(id)}`);
  },
};
