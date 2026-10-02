/**
 * Cliente da API do estoque-service. Em dev, /api passa pelo proxy do Vite; no compose, pelo nginx.
 * O operador entra com sessão (cookie HttpOnly); toda escrita leva o token CSRF no header.
 */

export interface Sessao {
  usuario: string;
}

export interface Resumo {
  skusCadastrados: number;
  /** Saldo zerado ou só em lotes vencidos (a expedição FEFO não os envia). */
  skusSemSaldo: number;
  demandasAbertas: number;
  lotesVencendo: number;
}

export interface DemandaAberta {
  id: number;
  sku: string;
  descricao: string;
  qtdSolicitada: number;
  registradoEm: string;
}

export interface ReposicaoRecente {
  eventId: string;
  sku: string;
  descricao: string;
  qtd: number;
  codigoLote: string;
  expedidoEm: string;
  /** Nulo enquanto o evento ainda não foi entregue ao mercado. */
  entregueEm: string | null;
}

export interface ProdutoEstoque {
  sku: string;
  descricao: string;
  ncm: string;
  tags: string[] | null;
  categoria: string | null;
  saldoDisponivel: number;
}

/** {@code descricao} e {@code ncm} só são exigidos para SKU ainda não cadastrado. */
export interface EntradaLote {
  sku: string;
  descricao?: string;
  ncm?: string;
  codigoLote: string;
  quantidade: number;
  validade?: string;
}

export interface LoteRegistrado {
  loteId: number;
  sku: string;
  saldoDisponivel: number;
  classificado: boolean;
  demandasAtendidas: number;
}

/** ProblemDetail (RFC 9457) devolvido pelo estoque. */
export interface Problema {
  status: number;
  title?: string;
  detail?: string;
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

/** Cookie (legível pelo front) onde o estoque entrega o token CSRF; volta no header X-XSRF-TOKEN. */
export const COOKIE_CSRF = "XSRF-ESTOQUE";

function lerCookie(nome: string): string | null {
  const par = document.cookie.split("; ").find((c) => c.startsWith(`${nome}=`));
  return par ? decodeURIComponent(par.slice(nome.length + 1)) : null;
}

/**
 * O estoque só grava o cookie do token quando o lê, e o troca no login e no logout. Sem cookie,
 * GET /sessao o entrega antes da escrita.
 */
async function tokenCsrf(): Promise<string | null> {
  if (!lerCookie(COOKIE_CSRF)) {
    await fetch("/api/sessao", { headers: { Accept: "application/json" } });
  }
  return lerCookie(COOKIE_CSRF);
}

async function requisitar<T>(caminho: string, init?: RequestInit): Promise<T> {
  const escrita = (init?.method ?? "GET") !== "GET";
  const csrf = escrita ? await tokenCsrf() : null;
  const resposta = await fetch(`/api${caminho}`, {
    ...init,
    headers: {
      Accept: "application/json",
      // Corpo em texto é JSON; formulário (URLSearchParams) leva o Content-Type que o fetch define.
      ...(typeof init?.body === "string" ? { "Content-Type": "application/json" } : {}),
      ...(csrf ? { "X-XSRF-TOKEN": csrf } : {}),
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
  if (resposta.status === 204) return undefined as T;
  return resposta.json() as Promise<T>;
}

/** Sessão ausente ou expirada: o app volta para a tela de login. */
export function naoAutenticado(erro: unknown): boolean {
  return erro instanceof ErroApi && erro.status === 401;
}

export const estoque = {
  /** Operador logado, ou null se não há sessão. */
  async sessaoAtual(): Promise<Sessao | null> {
    try {
      return await requisitar("/sessao");
    } catch (erro) {
      if (naoAutenticado(erro)) return null;
      throw erro;
    }
  },

  entrar(usuario: string, senha: string): Promise<void> {
    return requisitar("/sessao", { method: "POST", body: new URLSearchParams({ usuario, senha }) });
  },

  sair(): Promise<void> {
    return requisitar("/sessao", { method: "DELETE" });
  },

  resumo(): Promise<Resumo> {
    return requisitar("/painel/resumo");
  },

  demandasAbertas(): Promise<DemandaAberta[]> {
    return requisitar("/painel/demandas");
  },

  reposicoesRecentes(limite = 20): Promise<ReposicaoRecente[]> {
    return requisitar(`/painel/reposicoes?limite=${limite}`);
  },

  /** Vazio (null) quando o SKU ainda não existe no estoque. */
  async buscarProduto(sku: string): Promise<ProdutoEstoque | null> {
    try {
      return await requisitar(`/produtos/${encodeURIComponent(sku)}`);
    } catch (erro) {
      if (erro instanceof ErroApi && erro.status === 404) return null;
      throw erro;
    }
  },

  registrarLote(entrada: EntradaLote): Promise<LoteRegistrado> {
    return requisitar("/lotes", { method: "POST", body: JSON.stringify(entrada) });
  },
};

/** Mesma regra do backend (Sku.REGEX): EAN-8 ou GTIN-12/13/14. */
export const SKU_VALIDO = /^(\d{8}|\d{12,14})$/;
