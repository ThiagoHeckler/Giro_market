import { screen, within } from "@testing-library/react";
import { App } from "./App";
import { json, renderizar, simularApi } from "./testes/renderizar";

const RESUMO = { skusCadastrados: 1248, skusSemSaldo: 5, demandasAbertas: 2, lotesVencendo: 3 };

function apiDoPainel(sobrescrever: Record<string, () => Response> = {}) {
  return simularApi({
    "GET /api/painel/resumo": () => json(RESUMO),
    "GET /api/painel/demandas": () =>
      json([
        { id: 1, sku: "7891000068019", descricao: "LEITE INTEGRAL 1L", qtdSolicitada: 48, registradoEm: "2026-09-27T12:00:00Z" },
        { id: 2, sku: "7894900011517", descricao: "COCA COLA 2L PET", qtdSolicitada: 30, registradoEm: "2026-09-28T12:00:00Z" },
      ]),
    "GET /api/painel/reposicoes?limite=20": () =>
      json([
        {
          eventId: "a", sku: "7894900011517", descricao: "COCA COLA 2L PET", qtd: 18, codigoLote: "LT-2287",
          expedidoEm: "2026-09-29T12:15:00Z", entregueEm: null,
        },
        {
          eventId: "b", sku: "7891000068019", descricao: "LEITE INTEGRAL 1L", qtd: 15, codigoLote: "LT-2288",
          expedidoEm: "2026-09-29T12:00:00Z", entregueEm: "2026-09-29T12:00:01Z",
        },
      ]),
    ...sobrescrever,
  });
}

describe("Painel do estoque", () => {
  it("mostra os KPIs do estoque", async () => {
    apiDoPainel();
    renderizar(<App />);

    // "Demanda reprimida" também é título de seção: o KPI é o <dt> e o valor, o <dd> seguinte.
    const kpi = (rotulo: string) =>
      screen.getAllByText(rotulo).find((e) => e.tagName === "DT")?.nextElementSibling;
    expect(await screen.findByText("1.248")).toBeInTheDocument();
    expect(kpi("SKUs cadastrados")).toHaveTextContent("1.248");
    expect(kpi("SKUs sem saldo válido")).toHaveTextContent("5");
    expect(kpi("Demanda reprimida")).toHaveTextContent("2");
    expect(kpi("Lotes vencendo em 30 dias")).toHaveTextContent("3");
  });

  it("lista a demanda reprimida em ordem de atendimento", async () => {
    apiDoPainel();
    renderizar(<App />);

    const secao = screen.getByRole("region", { name: "Demanda reprimida" });
    expect(await within(secao).findByText("2 pedidos aguardando lote")).toBeInTheDocument();
    const linhas = within(secao).getAllByRole("row").slice(1);
    expect(linhas.map((l) => within(l).getAllByRole("cell")[1]?.textContent)).toEqual([
      "LEITE INTEGRAL 1L",
      "COCA COLA 2L PET",
    ]);
  });

  it("mostra o lote e o status de entrega de cada reposição", async () => {
    apiDoPainel();
    renderizar(<App />);

    const secao = screen.getByRole("region", { name: "Reposições recentes" });
    const [enviando, entregue] = (await within(secao).findAllByRole("row")).slice(1);
    expect(enviando).toHaveTextContent("LT-2287");
    expect(enviando).toHaveTextContent("enviando");
    expect(entregue).toHaveTextContent("LT-2288");
    expect(entregue).toHaveTextContent("entregue");
  });

  it("sem demanda em aberto mostra estado vazio", async () => {
    apiDoPainel({ "GET /api/painel/demandas": () => json([]) });
    renderizar(<App />);

    expect(await screen.findByText("Nenhum pedido do mercado esperando saldo.")).toBeInTheDocument();
  });

  it("estoque fora do ar mostra falha com opção de tentar de novo", async () => {
    apiDoPainel({ "GET /api/painel/reposicoes?limite=20": () => json({ status: 503 }, 503) });
    renderizar(<App />);

    const secao = screen.getByRole("region", { name: "Reposições recentes" });
    expect(await within(secao).findByRole("alert")).toHaveTextContent("O estoque não respondeu.");
    expect(within(secao).getByRole("button", { name: "Tentar de novo" })).toBeInTheDocument();
  });
});
