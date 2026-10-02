import { QueryClientProvider } from "@tanstack/react-query";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { App } from "./App";
import { criarClienteConsultas } from "./consultas";
import "./styles/tokens.css";
import "./styles/app.css";

const clienteConsultas = criarClienteConsultas({
  // Reposições e demandas mudam por eventos do mercado, sem ação do operador: o painel se atualiza sozinho.
  queries: { staleTime: 5_000, refetchInterval: 10_000, refetchOnWindowFocus: true, retry: 1 },
});

createRoot(document.getElementById("raiz")!).render(
  <StrictMode>
    <QueryClientProvider client={clienteConsultas}>
      <App />
    </QueryClientProvider>
  </StrictMode>,
);
