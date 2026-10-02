import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { App } from "../App";
import { COOKIE_CSRF } from "../api/estoque";
import { json, OPERADOR, renderizar, semConteudo, simularApi } from "../testes/renderizar";

const PAINEL = {
  "GET /api/painel/resumo": () => json({ skusCadastrados: 1, skusSemSaldo: 0, demandasAbertas: 0, lotesVencendo: 0 }),
  "GET /api/painel/demandas": () => json([]),
  "GET /api/painel/reposicoes?limite=20": () => json([]),
};

const naoLogado = () => {
  document.cookie = `${COOKIE_CSRF}=token-anonimo`;
  return json({ status: 401, detail: "nenhum operador logado" }, 401);
};

describe("Sessão do operador", () => {
  it("sem sessão mostra o login e, ao entrar, o painel", async () => {
    let logado = false;
    const chamadas = simularApi({
      ...PAINEL,
      "GET /api/sessao": () => (logado ? json(OPERADOR) : naoLogado()),
      "POST /api/sessao": () => {
        logado = true;
        return semConteudo();
      },
    });
    const usuario = userEvent.setup();
    renderizar(<App />);

    await usuario.type(await screen.findByLabelText("Usuário"), "operador");
    await usuario.type(screen.getByLabelText("Senha"), "segredo-123");
    await usuario.click(screen.getByRole("button", { name: "Entrar" }));

    expect(await screen.findByRole("heading", { name: "Painel do estoque" })).toBeInTheDocument();
    expect(screen.getByText("operador")).toBeInTheDocument();
    const login = chamadas.find((c) => c.metodo === "POST");
    expect(login?.corpo).toEqual({ usuario: "operador", senha: "segredo-123" });
    expect(login?.cabecalhos["X-XSRF-TOKEN"]).toBe("token-anonimo");
  });

  it("senha errada avisa sem sair do login", async () => {
    simularApi({
      "GET /api/sessao": naoLogado,
      "POST /api/sessao": () => json({ status: 401, detail: "usuário ou senha inválidos" }, 401),
    });
    const usuario = userEvent.setup();
    renderizar(<App />);

    await usuario.type(await screen.findByLabelText("Usuário"), "operador");
    await usuario.type(screen.getByLabelText("Senha"), "errada");
    await usuario.click(screen.getByRole("button", { name: "Entrar" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Usuário ou senha inválidos.");
    expect(screen.getByRole("button", { name: "Entrar" })).toBeInTheDocument();
  });

  it("sessão expirada no meio do uso volta para o login", async () => {
    simularApi({ ...PAINEL, "GET /api/painel/resumo": () => json({ status: 401 }, 401) });
    renderizar(<App />);

    expect(await screen.findByRole("heading", { name: "Entrar no painel" })).toBeInTheDocument();
  });

  it("sair encerra a sessão com o token CSRF e volta para o login", async () => {
    const chamadas = simularApi({ ...PAINEL, "DELETE /api/sessao": semConteudo });
    const usuario = userEvent.setup();
    renderizar(<App />);

    await usuario.click(await screen.findByRole("button", { name: "Sair" }));

    expect(await screen.findByRole("heading", { name: "Entrar no painel" })).toBeInTheDocument();
    expect(chamadas.find((c) => c.metodo === "DELETE")?.cabecalhos["X-XSRF-TOKEN"]).toBe("token-csrf");
  });
});
