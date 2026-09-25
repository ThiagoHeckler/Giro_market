import type { Produto } from "../api/mercado";
import { emCentavos } from "../util/dinheiro";

/**
 * O carrinho é só intenção de compra, guardado no navegador. A reserva de verdade acontece no
 * checkout (POST /pedidos), que é quem decide se há unidades — o limite aqui é cortesia de UX.
 */
export interface ItemCarrinho {
  sku: string;
  nome: string;
  preco: number;
  categoria: string | null;
  qtd: number;
  /** Unidades na prateleira na última vez que vimos o produto. */
  disponivel: number;
}

export type AcaoCarrinho =
  | { tipo: "adicionar"; produto: Produto; qtd: number }
  | { tipo: "alterar"; sku: string; qtd: number }
  | { tipo: "remover"; sku: string }
  | { tipo: "atualizarDisponivel"; sku: string; disponivel: number }
  | { tipo: "limpar" };

function limitar(qtd: number, disponivel: number): number {
  return Math.max(1, Math.min(Math.floor(qtd), disponivel));
}

export function reduzirCarrinho(itens: ItemCarrinho[], acao: AcaoCarrinho): ItemCarrinho[] {
  switch (acao.tipo) {
    case "adicionar": {
      const { produto } = acao;
      if (produto.status === "ESGOTADO" || produto.disponivel <= 0 || acao.qtd <= 0) return itens;
      const existente = itens.find((i) => i.sku === produto.sku);
      if (!existente) {
        return [
          ...itens,
          {
            sku: produto.sku,
            nome: produto.nome,
            preco: produto.preco,
            categoria: produto.categoria,
            qtd: limitar(acao.qtd, produto.disponivel),
            disponivel: produto.disponivel,
          },
        ];
      }
      return itens.map((i) =>
        i.sku === produto.sku
          ? { ...i, preco: produto.preco, disponivel: produto.disponivel, qtd: limitar(i.qtd + acao.qtd, produto.disponivel) }
          : i,
      );
    }
    case "alterar":
      return itens.map((i) => (i.sku === acao.sku ? { ...i, qtd: limitar(acao.qtd, i.disponivel) } : i));
    case "remover":
      return itens.filter((i) => i.sku !== acao.sku);
    case "atualizarDisponivel":
      return itens.flatMap((i) => {
        if (i.sku !== acao.sku) return [i];
        if (acao.disponivel <= 0) return [];
        return [{ ...i, disponivel: acao.disponivel, qtd: Math.min(i.qtd, acao.disponivel) }];
      });
    case "limpar":
      return [];
  }
}

export function totalEmCentavos(itens: ItemCarrinho[]): number {
  return itens.reduce((soma, i) => soma + emCentavos(i.preco) * i.qtd, 0);
}

export function quantidadeDeItens(itens: ItemCarrinho[]): number {
  return itens.reduce((soma, i) => soma + i.qtd, 0);
}

/** Lê o carrinho salvo, descartando qualquer coisa que não tenha o formato esperado. */
export function lerCarrinhoSalvo(bruto: string | null): ItemCarrinho[] {
  if (!bruto) return [];
  try {
    const dados: unknown = JSON.parse(bruto);
    if (!Array.isArray(dados)) return [];
    return dados.filter(
      (i): i is ItemCarrinho =>
        typeof i === "object" && i !== null &&
        typeof i.sku === "string" && typeof i.nome === "string" &&
        typeof i.preco === "number" && typeof i.qtd === "number" && i.qtd > 0 &&
        typeof i.disponivel === "number",
    );
  } catch {
    return [];
  }
}
