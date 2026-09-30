import { useQuery } from "@tanstack/react-query";
import { estoque } from "../api/estoque";
import { haQuantoTempo } from "../util/tempo";
import { Carregando, FalhaAoCarregar, Vazio } from "./Estados";

export function TabelaDemanda() {
  const demandas = useQuery({ queryKey: ["painel", "demandas"], queryFn: estoque.demandasAbertas });
  const total = demandas.data?.length ?? 0;

  return (
    <section className="cartao cartao--tabela" aria-labelledby="titulo-demanda" id="demanda">
      <div className="cartao__topo">
        <h2 id="titulo-demanda" className="cartao__titulo">Demanda reprimida</h2>
        {demandas.data && (
          <span className="cartao__subtitulo">
            {total === 1 ? "1 pedido aguardando lote" : `${total} pedidos aguardando lote`}
          </span>
        )}
      </div>

      {demandas.isPending && <Carregando />}
      {demandas.isError && <FalhaAoCarregar erro={demandas.error} aoTentarDeNovo={() => demandas.refetch()} />}
      {demandas.data && total === 0 && <Vazio>Nenhum pedido do mercado esperando saldo.</Vazio>}
      {demandas.data && total > 0 && (
        <div className="tabela-rolagem">
          <table className="tabela">
            <thead>
              <tr>
                <th scope="col">SKU</th>
                <th scope="col">Produto</th>
                <th scope="col" className="tabela__numero">Qtd</th>
                <th scope="col">Desde</th>
              </tr>
            </thead>
            <tbody>
              {demandas.data.map((d) => (
                <tr key={d.id}>
                  <td className="mono">{d.sku}</td>
                  <td>{d.descricao}</td>
                  <td className="tabela__numero mono">{d.qtdSolicitada}</td>
                  <td className="tabela__tempo">
                    <time dateTime={d.registradoEm}>{haQuantoTempo(d.registradoEm)}</time>
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
