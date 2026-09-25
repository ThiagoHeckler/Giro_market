import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ErroApi, mercado } from "../api/mercado";
import { useCarrinho } from "../carrinho/Carrinho";
import { Carregando, FalhaAoCarregar } from "../componentes/Estados";
import { IconeCarrinho } from "../componentes/Icones";
import { MarcaGiro } from "../componentes/Logo";
import { Monograma } from "../componentes/Monograma";
import { SeletorQuantidade } from "../componentes/SeletorQuantidade";
import { SeloEstoque } from "../componentes/SeloEstoque";
import { rotuloCategoria } from "../util/categorias";
import { formatarPreco } from "../util/dinheiro";
import { PaginaNaoEncontrada } from "./PaginaNaoEncontrada";

export function PaginaProduto() {
  const { sku = "" } = useParams();
  const { despachar } = useCarrinho();
  const [qtd, setQtd] = useState(1);
  const [adicionado, setAdicionado] = useState(false);
  const consulta = useQuery({ queryKey: ["produto", sku], queryFn: () => mercado.buscarProduto(sku) });

  if (consulta.isPending) return <Carregando rotulo="Carregando produto…" />;
  if (consulta.isError) {
    if (consulta.error instanceof ErroApi && consulta.error.status === 404) return <PaginaNaoEncontrada />;
    return <FalhaAoCarregar erro={consulta.error} aoTentarDeNovo={() => consulta.refetch()} />;
  }

  const produto = consulta.data;
  const esgotado = produto.status === "ESGOTADO";

  return (
    <>
      <nav aria-label="Trilha" className="trilha">
        <Link to="/">Início</Link>
        <span aria-hidden="true">/</span>
        {produto.categoria && (
          <>
            <Link to={`/?categoria=${encodeURIComponent(produto.categoria)}`}>{rotuloCategoria(produto.categoria)}</Link>
            <span aria-hidden="true">/</span>
          </>
        )}
        <span aria-current="page">{produto.nome}</span>
      </nav>

      <div className="produto">
        <Monograma nome={produto.nome} categoria={produto.categoria} tamanho="g" apagado={esgotado} />

        <div className="produto__info">
          <SeloEstoque status={produto.status} className="produto__selo" />
          <h1 className="titulo-pagina">{produto.nome}</h1>
          <span className="preco preco--grande">{formatarPreco(produto.preco)}</span>
          <span className="codigo">SKU/EAN {produto.sku}</span>

          {esgotado ? (
            <p className="aviso aviso--neutro" role="status">
              Acabou na prateleira. A reposição já foi pedida ao estoque e o produto volta sozinho.
            </p>
          ) : (
            <div className="produto__compra">
              <SeletorQuantidade valor={qtd} maximo={produto.disponivel} aoMudar={setQtd} rotulo="Quantidade" />
              <button
                type="button"
                className="botao botao--primario botao--grande"
                onClick={() => {
                  despachar({ tipo: "adicionar", produto, qtd });
                  setAdicionado(true);
                }}
              >
                <IconeCarrinho />
                {adicionado ? "Adicionado ao carrinho ✓" : "Adicionar ao carrinho"}
              </button>
            </div>
          )}
          {!esgotado && (
            <span className="texto-auxiliar">
              {produto.disponivel} {produto.disponivel === 1 ? "unidade" : "unidades"} na prateleira
            </span>
          )}

          {produto.tags.length > 0 && (
            <ul className="tags" aria-label="Tags">
              {produto.tags.map((t) => <li key={t}>{t}</li>)}
            </ul>
          )}

          <div className="cartao-reposicao">
            <MarcaGiro />
            <p>
              Reposição automática ativa. Se este item baixar do mínimo, o Girô repõe a prateleira sozinho — você
              raramente vê “esgotado”.
            </p>
          </div>
        </div>
      </div>
    </>
  );
}
