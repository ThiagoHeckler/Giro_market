import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { App } from "./App";
import { ProvedorCarrinho } from "./carrinho/Carrinho";
import "./styles/tokens.css";
import "./styles/app.css";

const clienteConsultas = new QueryClient({
  defaultOptions: {
    // Prateleira muda com vendas e reposições: dado de vitrine envelhece rápido.
    queries: { staleTime: 10_000, refetchOnWindowFocus: true, retry: 1 },
  },
});

createRoot(document.getElementById("raiz")!).render(
  <StrictMode>
    <QueryClientProvider client={clienteConsultas}>
      <BrowserRouter>
        <ProvedorCarrinho>
          <App />
        </ProvedorCarrinho>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
);
