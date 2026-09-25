import { useQuery } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { mercado, type Produto } from "../api/mercado";
import { useCarrinho } from "../carrinho/Carrinho";
import { CartaoProduto } from "../componentes/CartaoProduto";
import { ChipsCategoria } from "../componentes/ChipsCategoria";
import { Carregando, FalhaAoCarregar, Vazio } from "../componentes/Estados";
import { MarcaGiro } from "../componentes/Logo";
import { rotuloCategoria } from "../util/categorias";

export function PaginaVitrine() {
  const [parametros] = useSearchParams();
  const categoria = parametros.get("categoria");
  const busca = parametros.get("busca");
  const { despachar } = useCarrinho();

  const produtos = useQuery({
    queryKey: ["produtos", { categoria, busca }],
    queryFn: () => mercado.listarProdutos({ categoria: categoria ?? undefined, busca: busca ?? undefined }),
  });
  // Chips vêm do catálogo inteiro, para não sumirem quando um filtro está ativo.
  const catalogo = useQuery({ queryKey: ["produtos", {}], queryFn: () => mercado.listarProdutos() });
  const categorias = [...new Set((catalogo.data ?? []).flatMap((p) => (p.categoria ? [p.categoria] : [])))].sort();

  const adicionar = (produto: Produto) => despachar({ tipo: "adicionar", produto, qtd: 1 });

  const titulo = busca ? `Resultados para “${busca}”` : categoria ? rotuloCategoria(categoria) : "Em alta";

  return (
    <>
      {!busca && !categoria && (
        <section className="heroi">
          <span className="sobretitulo">Girô mercado</span>
          <h1>O estoque que gira sozinho</h1>
          <p>A prateleira se repõe automaticamente: quando um item baixa do mínimo, o Girô pede reposição ao estoque.</p>
          <MarcaGiro tamanho={220} monocromatico className="heroi__marca" />
        </section>
      )}

      {categorias.length > 0 && <ChipsCategoria categorias={categorias} ativa={categoria} />}

      <section aria-labelledby="titulo-lista" className="secao">
        <h2 id="titulo-lista" className="titulo-secao">{titulo}</h2>
        {produtos.isPending ? (
          <Carregando rotulo="Carregando produtos…" />
        ) : produtos.isError ? (
          <FalhaAoCarregar erro={produtos.error} aoTentarDeNovo={() => produtos.refetch()} />
        ) : produtos.data.length === 0 ? (
          <Vazio>
            <p>{busca ? "Nenhum produto encontrado para essa busca." : "Nenhum produto nesta vitrine ainda."}</p>
          </Vazio>
        ) : (
          <div className="grade-produtos">
            {produtos.data.map((p) => (
              <CartaoProduto key={p.sku} produto={p} aoAdicionar={adicionar} />
            ))}
          </div>
        )}
      </section>
    </>
  );
}
