import { produto } from "../testes/renderizar";
import { lerCarrinhoSalvo, quantidadeDeItens, reduzirCarrinho, totalEmCentavos, type ItemCarrinho } from "./estado";

const coca = produto({ disponivel: 3 });

describe("reduzirCarrinho", () => {
  it("adiciona e soma quantidades do mesmo SKU", () => {
    let itens = reduzirCarrinho([], { tipo: "adicionar", produto: coca, qtd: 1 });
    itens = reduzirCarrinho(itens, { tipo: "adicionar", produto: coca, qtd: 1 });

    expect(itens).toHaveLength(1);
    expect(itens[0]?.qtd).toBe(2);
  });

  it("não passa do que há na prateleira", () => {
    const itens = reduzirCarrinho([], { tipo: "adicionar", produto: coca, qtd: 10 });

    expect(itens[0]?.qtd).toBe(3);
  });

  it("ignora produto esgotado", () => {
    const esgotado = produto({ status: "ESGOTADO", disponivel: 0 });

    expect(reduzirCarrinho([], { tipo: "adicionar", produto: esgotado, qtd: 1 })).toEqual([]);
  });

  it("alterar mantém entre 1 e o disponível", () => {
    const itens = reduzirCarrinho([], { tipo: "adicionar", produto: coca, qtd: 2 });

    expect(reduzirCarrinho(itens, { tipo: "alterar", sku: coca.sku, qtd: 0 })[0]?.qtd).toBe(1);
    expect(reduzirCarrinho(itens, { tipo: "alterar", sku: coca.sku, qtd: 99 })[0]?.qtd).toBe(3);
  });

  it("disponível atualizado reduz a quantidade; zerado remove o item", () => {
    const itens = reduzirCarrinho([], { tipo: "adicionar", produto: coca, qtd: 3 });

    expect(reduzirCarrinho(itens, { tipo: "atualizarDisponivel", sku: coca.sku, disponivel: 1 })[0]?.qtd).toBe(1);
    expect(reduzirCarrinho(itens, { tipo: "atualizarDisponivel", sku: coca.sku, disponivel: 0 })).toEqual([]);
  });
});

describe("totais", () => {
  it("soma em centavos, sem erro de ponto flutuante", () => {
    const itens: ItemCarrinho[] = [
      { sku: "1", nome: "a", preco: 0.1, categoria: null, qtd: 1, disponivel: 5 },
      { sku: "2", nome: "b", preco: 0.2, categoria: null, qtd: 1, disponivel: 5 },
      { sku: "3", nome: "c", preco: 8.99, categoria: null, qtd: 3, disponivel: 5 },
    ];

    expect(totalEmCentavos(itens)).toBe(2727);
    expect(quantidadeDeItens(itens)).toBe(5);
  });
});

describe("lerCarrinhoSalvo", () => {
  it("descarta lixo e itens malformados", () => {
    expect(lerCarrinhoSalvo("não é json")).toEqual([]);
    expect(lerCarrinhoSalvo('{"sku":"1"}')).toEqual([]);
    expect(lerCarrinhoSalvo('[{"sku":"1","nome":"a","preco":"caro","qtd":1,"disponivel":1}]')).toEqual([]);
  });
});
