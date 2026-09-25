import { Route, Routes } from "react-router-dom";
import { Cabecalho } from "./componentes/Cabecalho";
import { PaginaCarrinho } from "./paginas/PaginaCarrinho";
import { PaginaNaoEncontrada } from "./paginas/PaginaNaoEncontrada";
import { PaginaPedido } from "./paginas/PaginaPedido";
import { PaginaProduto } from "./paginas/PaginaProduto";
import { PaginaVitrine } from "./paginas/PaginaVitrine";

export function App() {
  return (
    <>
      <a href="#conteudo" className="pular-para-conteudo">Pular para o conteúdo</a>
      <Cabecalho />
      <main id="conteudo" className="conteudo">
        <Routes>
          <Route path="/" element={<PaginaVitrine />} />
          <Route path="/produtos/:sku" element={<PaginaProduto />} />
          <Route path="/carrinho" element={<PaginaCarrinho />} />
          <Route path="/pedidos/:id" element={<PaginaPedido />} />
          <Route path="*" element={<PaginaNaoEncontrada />} />
        </Routes>
      </main>
    </>
  );
}
