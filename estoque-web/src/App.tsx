import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { estoque } from "./api/estoque";
import { Carregando, FalhaAoCarregar } from "./componentes/Estados";
import { FormularioLote } from "./componentes/FormularioLote";
import { IconeDemanda, IconeLote, IconeReposicao, IconeSair } from "./componentes/Icones";
import { Kpis } from "./componentes/Kpis";
import { MarcaGiro } from "./componentes/Logo";
import { TabelaDemanda } from "./componentes/TabelaDemanda";
import { TabelaReposicoes } from "./componentes/TabelaReposicoes";
import { TelaLogin } from "./componentes/TelaLogin";
import { CHAVE_SESSAO } from "./consultas";

const URL_VITRINE = import.meta.env.VITE_VITRINE_URL ?? "http://localhost:15173";

export function App() {
  const sessao = useQuery({
    queryKey: CHAVE_SESSAO,
    queryFn: estoque.sessaoAtual,
    // Só muda por login, logout ou um 401 em outra consulta (ver consultas.ts).
    staleTime: Infinity,
    refetchInterval: false,
  });

  if (sessao.isPending) return <Carregando />;
  if (sessao.isError) return <FalhaAoCarregar erro={sessao.error} aoTentarDeNovo={() => sessao.refetch()} />;
  if (!sessao.data) return <TelaLogin />;
  return <Painel operador={sessao.data.usuario} />;
}

function Painel({ operador }: { operador: string }) {
  const consultas = useQueryClient();
  const sair = useMutation({
    mutationFn: estoque.sair,
    onSuccess: () => {
      // Nada do painel fica em cache depois do logout.
      consultas.removeQueries({ predicate: (consulta) => consulta.queryKey[0] !== CHAVE_SESSAO[0] });
      consultas.setQueryData(CHAVE_SESSAO, null);
    },
  });

  return (
    <div className="painel">
      <a href="#conteudo" className="pular-para-conteudo">Pular para o conteúdo</a>

      <aside className="lateral">
        <div className="lateral__marca">
          <MarcaGiro />
          <div className="lateral__nomes">
            <span className="lateral__nome">Girô</span>
            <span className="lateral__sistema">Estoque</span>
          </div>
        </div>
        <nav aria-label="Seções do painel" className="lateral__nav">
          <a href="#entrada" className="lateral__link"><IconeLote />Entrada de lote</a>
          <a href="#demanda" className="lateral__link"><IconeDemanda />Demanda reprimida</a>
          <a href="#reposicoes" className="lateral__link"><IconeReposicao />Reposições</a>
        </nav>
        <a href={URL_VITRINE} className="lateral__vitrine"><IconeSair />Ver a vitrine</a>
      </aside>

      <div className="principal">
        <header className="topo">
          <h1 className="topo__titulo">Painel do estoque</h1>
          <div className="topo__operador">
            <span>{operador}</span>
            <button
              type="button"
              className="botao botao--secundario"
              onClick={() => sair.mutate()}
              disabled={sair.isPending}
            >
              Sair
            </button>
          </div>
        </header>
        <main id="conteudo" className="conteudo">
          <Kpis />
          <div className="colunas">
            <FormularioLote />
            <TabelaDemanda />
          </div>
          <TabelaReposicoes />
        </main>
      </div>
    </div>
  );
}
