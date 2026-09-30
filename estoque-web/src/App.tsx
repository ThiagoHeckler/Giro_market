import { FormularioLote } from "./componentes/FormularioLote";
import { Kpis } from "./componentes/Kpis";
import { MarcaGiro } from "./componentes/Logo";
import { TabelaDemanda } from "./componentes/TabelaDemanda";
import { TabelaReposicoes } from "./componentes/TabelaReposicoes";

const URL_VITRINE = import.meta.env.VITE_VITRINE_URL ?? "http://localhost:15173";

export function App() {
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
          <a href="#entrada" className="lateral__link">Entrada de lote</a>
          <a href="#demanda" className="lateral__link">Demanda reprimida</a>
          <a href="#reposicoes" className="lateral__link">Reposições</a>
        </nav>
        <a href={URL_VITRINE} className="lateral__vitrine">Ver a vitrine ›</a>
      </aside>

      <div className="principal">
        <header className="topo">
          <h1 className="topo__titulo">Painel do estoque</h1>
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
