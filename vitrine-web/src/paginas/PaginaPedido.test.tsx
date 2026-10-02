import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { PedidoDetalhe } from "../api/mercado";
import { json, renderizar, simularApi } from "../testes/renderizar";
import { PaginaPedido } from "./PaginaPedido";

const ID = "3f2a6c1e-0000-4000-8000-000000000001";

function pedido(parcial: Partial<PedidoDetalhe> = {}): PedidoDetalhe {
  return {
    id: ID,
    status: "AGUARDANDO_PAGAMENTO",
    total: 17.98,
    criadoEm: "2026-10-02T12:00:00Z",
    reservaExpiraEm: new Date(Date.now() + 10 * 60_000).toISOString(),
    itens: [{ sku: "7894900011517", nome: "Coca-Cola 2L PET", qtd: 2, precoUnitario: 8.99 }],
    ...parcial,
  };
}

const abrir = () => renderizar(<PaginaPedido />, { rota: `/pedidos/${ID}`, caminho: "/pedidos/:id" });

it("reserva no prazo mostra a contagem e paga", async () => {
  const chamadas = simularApi({
    [`GET /api/pedidos/${ID}`]: () => json(pedido()),
    [`POST /api/pedidos/${ID}/pagamento`]: () => json(pedido({ status: "PAGO" })),
  });
  abrir();

  expect(await screen.findByText(/Itens reservados no estoque por/)).toBeInTheDocument();
  await userEvent.click(screen.getByRole("button", { name: /^Pagar/ }));

  expect(await screen.findByRole("heading", { name: "Pedido pago" })).toBeInTheDocument();
  expect(screen.getByRole("status")).toHaveTextContent("Pagamento confirmado.");
  expect(screen.queryByRole("button", { name: /^Pagar/ })).not.toBeInTheDocument();
  expect(chamadas.filter((c) => c.metodo === "POST")).toHaveLength(1);
});

it("pagamento recusado por prazo vencido avisa e mostra o pedido expirado", async () => {
  let expirado = false;
  simularApi({
    [`GET /api/pedidos/${ID}`]: () => json(pedido(expirado ? { status: "EXPIRADO" } : {})),
    [`POST /api/pedidos/${ID}/pagamento`]: () => {
      expirado = true;
      return json({ status: 409, title: "Pagamento recusado" }, 409);
    },
  });
  abrir();

  await userEvent.click(await screen.findByRole("button", { name: /^Pagar/ }));

  expect(await screen.findByRole("heading", { name: "Reserva expirada" })).toBeInTheDocument();
  expect(screen.getByText(/os itens voltaram para a vitrine/)).toBeInTheDocument();
});

it("prazo vencido sem pagamento não oferece pagar", async () => {
  simularApi({
    [`GET /api/pedidos/${ID}`]: () => json(pedido({ reservaExpiraEm: "2026-01-01T00:00:00Z" })),
  });
  abrir();

  expect(await screen.findByText(/O prazo da reserva terminou/)).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: /^Pagar/ })).not.toBeInTheDocument();
});

it("pedido pago mostra a confirmação", async () => {
  simularApi({ [`GET /api/pedidos/${ID}`]: () => json(pedido({ status: "PAGO" })) });
  abrir();

  expect(await screen.findByRole("heading", { name: "Pedido pago" })).toBeInTheDocument();
});
