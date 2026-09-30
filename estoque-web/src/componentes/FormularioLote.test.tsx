import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { json, renderizar, simularApi } from "../testes/renderizar";
import { FormularioLote } from "./FormularioLote";

const COCA = "7894900011517";
const PRODUTO = {
  sku: COCA,
  descricao: "COCA COLA 2L PET",
  ncm: "22021000",
  tags: ["refrigerante"],
  categoria: "bebidas",
  saldoDisponivel: 34,
};
const REGISTRADO = { loteId: 7, sku: COCA, saldoDisponivel: 154, classificado: true, demandasAtendidas: 2 };

async function preencher(usuario: ReturnType<typeof userEvent.setup>, sku: string) {
  await usuario.type(screen.getByLabelText("SKU / EAN"), sku);
  await usuario.type(screen.getByLabelText("Quantidade"), "120");
  await usuario.type(screen.getByLabelText("Código do lote"), "LT-2291");
}

describe("FormularioLote", () => {
  it("mostra o produto do SKU existente e registra o lote sem pedir cadastro", async () => {
    const chamadas = simularApi({
      [`GET /api/produtos/${COCA}`]: () => json(PRODUTO),
      "POST /api/lotes": () => json(REGISTRADO, 201),
    });
    const usuario = userEvent.setup();
    renderizar(<FormularioLote />);

    await preencher(usuario, COCA);
    expect(await screen.findByText("COCA COLA 2L PET · saldo 34")).toBeInTheDocument();
    expect(screen.queryByLabelText("Descrição")).not.toBeInTheDocument();

    await usuario.click(screen.getByRole("button", { name: "Dar entrada no estoque" }));

    expect(await screen.findByRole("status")).toHaveTextContent(
      "Lote LT-2291 registrado. Saldo do SKU: 154. 2 demandas reprimidas atendidas.",
    );
    expect(chamadas.find((c) => c.metodo === "POST")?.corpo).toEqual({
      sku: COCA,
      codigoLote: "LT-2291",
      quantidade: 120,
    });
    expect(screen.getByLabelText("SKU / EAN")).toHaveValue("");
  });

  it("SKU novo pede descrição e NCM e os envia", async () => {
    const chamadas = simularApi({
      [`GET /api/produtos/${COCA}`]: () => json({ status: 404 }, 404),
      "POST /api/lotes": () => json({ ...REGISTRADO, saldoDisponivel: 120, demandasAtendidas: 0 }, 201),
    });
    const usuario = userEvent.setup();
    renderizar(<FormularioLote />);

    await preencher(usuario, COCA);
    expect(await screen.findByText("SKU novo: informe descrição e NCM.")).toBeInTheDocument();
    await usuario.type(screen.getByLabelText("Descrição"), "COCA COLA 2L PET");
    await usuario.type(screen.getByLabelText("NCM"), "22021000");
    await usuario.type(screen.getByLabelText(/Validade/), "2027-03-01");
    await usuario.click(screen.getByRole("button", { name: "Dar entrada no estoque" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Lote LT-2291 registrado. Saldo do SKU: 120.");
    expect(chamadas.find((c) => c.metodo === "POST")?.corpo).toEqual({
      sku: COCA,
      codigoLote: "LT-2291",
      quantidade: 120,
      validade: "2027-03-01",
      descricao: "COCA COLA 2L PET",
      ncm: "22021000",
    });
  });

  it("mostra o detalhe do problema quando o estoque recusa o lote", async () => {
    simularApi({
      [`GET /api/produtos/${COCA}`]: () => json(PRODUTO),
      "POST /api/lotes": () => json({ status: 409, detail: "lote já registrado para este SKU" }, 409),
    });
    const usuario = userEvent.setup();
    renderizar(<FormularioLote />);

    await preencher(usuario, COCA);
    await screen.findByText("COCA COLA 2L PET · saldo 34");
    await usuario.click(screen.getByRole("button", { name: "Dar entrada no estoque" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("lote já registrado para este SKU");
    expect(screen.getByLabelText("Código do lote")).toHaveValue("LT-2291");
  });

  it("não consulta o estoque enquanto o SKU é inválido", async () => {
    const chamadas = simularApi({});
    const usuario = userEvent.setup();
    renderizar(<FormularioLote />);

    await usuario.type(screen.getByLabelText("SKU / EAN"), "12345");

    expect(await screen.findByText("EAN-8 ou GTIN de 12 a 14 dígitos.")).toBeInTheDocument();
    expect(chamadas).toHaveLength(0);
  });
});
