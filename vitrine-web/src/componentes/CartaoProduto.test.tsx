import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { produto, renderizar } from "../testes/renderizar";
import { CartaoProduto } from "./CartaoProduto";

it("produto disponível tem selo de estoque e botão de adicionar", async () => {
  const aoAdicionar = vi.fn();
  renderizar(<CartaoProduto produto={produto()} aoAdicionar={aoAdicionar} />);

  expect(screen.getByText("Em estoque")).toBeInTheDocument();
  await userEvent.click(screen.getByRole("button", { name: "Adicionar Coca-Cola 2L PET ao carrinho" }));

  expect(aoAdicionar).toHaveBeenCalledOnce();
  expect(screen.getByRole("button", { name: /ao carrinho/ })).toHaveTextContent("Adicionado");
});

it("produto esgotado não oferece compra e avisa da reposição", () => {
  renderizar(<CartaoProduto produto={produto({ status: "ESGOTADO", disponivel: 0 })} aoAdicionar={vi.fn()} />);

  expect(screen.getByText("Esgotado")).toBeInTheDocument();
  expect(screen.queryByRole("button")).not.toBeInTheDocument();
  expect(screen.getByText("Reposição a caminho")).toBeInTheDocument();
});

it("leva ao detalhe do produto pelo SKU", () => {
  renderizar(<CartaoProduto produto={produto()} aoAdicionar={vi.fn()} />);

  expect(screen.getByRole("link", { name: /Coca-Cola 2L PET/ })).toHaveAttribute("href", "/produtos/7894900011517");
});
