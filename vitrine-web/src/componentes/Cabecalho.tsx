import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useCarrinho } from "../carrinho/Carrinho";
import { quantidadeDeItens } from "../carrinho/estado";
import { IconeBusca, IconeCarrinho } from "./Icones";
import { MarcaGiro } from "./Logo";

export function Cabecalho() {
  const { itens } = useCarrinho();
  const quantidade = quantidadeDeItens(itens);
  const navegar = useNavigate();
  const [parametros] = useSearchParams();
  const [busca, setBusca] = useState(parametros.get("busca") ?? "");

  useEffect(() => setBusca(parametros.get("busca") ?? ""), [parametros]);

  function buscar(evento: FormEvent) {
    evento.preventDefault();
    const termo = busca.trim();
    navegar(termo ? `/?busca=${encodeURIComponent(termo)}` : "/");
  }

  return (
    <header className="cabecalho">
      <div className="cabecalho__interno">
        <Link to="/" className="cabecalho__marca" aria-label="Girô — início">
          <MarcaGiro />
          <span className="cabecalho__nome">Girô</span>
        </Link>

        <form role="search" className="cabecalho__busca" onSubmit={buscar}>
          <IconeBusca />
          <label className="vh" htmlFor="busca">Buscar produtos</label>
          <input
            id="busca"
            type="search"
            placeholder="Buscar produtos, marcas ou SKU…"
            autoComplete="off"
            spellCheck={false}
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
          />
        </form>

        <nav className="cabecalho__nav" aria-label="Conta e carrinho">
          <Link to="/carrinho" className="botao botao--primario cabecalho__carrinho">
            <IconeCarrinho />
            Carrinho
            {quantidade > 0 && (
              <span className="contador" aria-hidden="true">
                {quantidade}
              </span>
            )}
            <span className="vh">, {quantidade} {quantidade === 1 ? "item" : "itens"}</span>
          </Link>
        </nav>
      </div>
    </header>
  );
}
