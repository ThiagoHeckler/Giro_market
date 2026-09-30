import { useQuery } from "@tanstack/react-query";
import { estoque } from "../api/estoque";
import { quando } from "../util/tempo";
import { Carregando, FalhaAoCarregar, Vazio } from "./Estados";

export function TabelaReposicoes() {
  const reposicoes = useQuery({ queryKey: ["painel", "reposicoes"], queryFn: () => estoque.reposicoesRecentes() });

  return (
    <section className="cartao cartao--tabela" aria-labelledby="titulo-reposicoes" id="reposicoes">
      <div className="cartao__topo">
        <h2 id="titulo-reposicoes" className="cartao__titulo">Reposições recentes</h2>
      </div>

      {reposicoes.isPending && <Carregando />}
      {reposicoes.isError && (
        <FalhaAoCarregar erro={reposicoes.error} aoTentarDeNovo={() => reposicoes.refetch()} />
      )}
      {reposicoes.data?.length === 0 && <Vazio>Nenhum lote enviado ao mercado ainda.</Vazio>}
      {reposicoes.data && reposicoes.data.length > 0 && (
        <div className="tabela-rolagem">
          <table className="tabela">
            <thead>
              <tr>
                <th scope="col">Quando</th>
                <th scope="col">SKU</th>
                <th scope="col">Produto</th>
                <th scope="col" className="tabela__numero">Qtd</th>
                <th scope="col">Lote</th>
                <th scope="col">Status</th>
              </tr>
            </thead>
            <tbody>
              {reposicoes.data.map((r) => (
                <tr key={r.eventId}>
                  <td className="tabela__tempo">
                    <time dateTime={r.expedidoEm}>{quando(r.expedidoEm)}</time>
                  </td>
                  <td className="mono">{r.sku}</td>
                  <td>{r.descricao}</td>
                  <td className="tabela__numero mono">{r.qtd}</td>
                  <td className="mono">{r.codigoLote}</td>
                  <td>
                    {r.entregueEm
                      ? <span className="selo selo--entregue">entregue</span>
                      : <span className="selo selo--enviando">enviando</span>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
