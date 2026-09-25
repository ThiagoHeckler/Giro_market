import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ItemCarrinho } from "../carrinho/estado";
import { json, produto, renderizar, simularApi } from "../testes/renderizar";
import { PaginaCarrinho } from "./PaginaCarrinho";

const noCarrinho: ItemCarrinho[] = [
  { sku: "7894900011517", nome: "Coca-Cola 2L PET", preco: 8.99, categoria: "bebidas", qtd: 3, disponivel: 10 },
];

it("finalizar envia os itens e abre o pedido reservado", async () => {
  const chamadas = simularApi({
    "POST /api/pedidos": () =>
      json({ pedidoId: "p-1", total: 26.97, reservaExpiraEm: "2099-01-01T00:00:00Z" }, 201),
  });
  renderizar(<PaginaCarrinho />, { rota: "/carrinho", carrinho: noCarrinho });

  await userEvent.click(screen.getByRole("button", { name: "Finalizar compra" }));

  expect(await screen.findByText("página do pedido")).toBeInTheDocument();
  expect(chamadas[0]?.corpo).toEqual({ itens: [{ sku: "7894900011517", qtd: 3 }] });
  expect(JSON.parse(window.localStorage.getItem("giro.carrinho") ?? "")).toEqual([]);
});

it("409 do checkout ajusta o item que acabou e explica o motivo", async () => {
  simularApi({
    "POST /api/pedidos": () =>
      json({ status: 409, title: "Estoque insuficiente", sku: "7894900011517" }, 409),
    "GET /api/produtos/7894900011517": () => json(produto({ disponivel: 1 })),
  });
  renderizar(<PaginaCarrinho />, { rota: "/carrinho", carrinho: noCarrinho });

  await userEvent.click(screen.getByRole("button", { name: "Finalizar compra" }));

  expect(await screen.findByText(/Só resta 1 unidade/)).toBeInTheDocument();
  expect(screen.getByRole("group", { name: "Quantidade de Coca-Cola 2L PET" })).toHaveTextContent("1");
});

it("falha de rede mantém o carrinho e oferece tentar de novo", async () => {
  simularApi({ "POST /api/pedidos": () => json({}, 503) });
  renderizar(<PaginaCarrinho />, { rota: "/carrinho", carrinho: noCarrinho });

  await userEvent.click(screen.getByRole("button", { name: "Finalizar compra" }));

  expect(await screen.findByRole("alert")).toHaveTextContent("Seu carrinho continua salvo");
  expect(screen.getByText("Coca-Cola 2L PET")).toBeInTheDocument();
});

it("carrinho vazio convida a ver produtos", () => {
  renderizar(<PaginaCarrinho />, { rota: "/carrinho" });

  expect(screen.getByText("Seu carrinho está vazio.")).toBeInTheDocument();
});
