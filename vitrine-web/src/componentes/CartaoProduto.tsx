import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import type { Produto } from "../api/mercado";
import { formatarPreco } from "../util/dinheiro";
import { Monograma } from "./Monograma";
import { SeloEstoque } from "./SeloEstoque";

interface Props {
  produto: Produto;
  aoAdicionar: (produto: Produto) => void;
}

export function CartaoProduto({ produto, aoAdicionar }: Props) {
  const esgotado = produto.status === "ESGOTADO";
  const [adicionado, setAdicionado] = useState(false);

  useEffect(() => {
    if (!adicionado) return;
    const t = window.setTimeout(() => setAdicionado(false), 1500);
    return () => window.clearTimeout(t);
  }, [adicionado]);

  return (
    <article className={`cartao-produto${esgotado ? " cartao-produto--esgotado" : ""}`}>
      <Link to={`/produtos/${produto.sku}`} className="cartao-produto__link">
        <div className="cartao-produto__imagem">
          <Monograma nome={produto.nome} categoria={produto.categoria} apagado={esgotado} />
          <SeloEstoque status={produto.status} className="cartao-produto__selo" />
        </div>
        <h3 className="cartao-produto__nome" title={produto.nome}>{produto.nome}</h3>
      </Link>
      <div className="cartao-produto__rodape">
        <span className="preco">{formatarPreco(produto.preco)}</span>
        {esgotado ? (
          <p className="cartao-produto__reposicao">Reposição a caminho</p>
        ) : (
          <button
            type="button"
            className="botao botao--primario"
            onClick={() => {
              aoAdicionar(produto);
              setAdicionado(true);
            }}
            aria-label={`Adicionar ${produto.nome} ao carrinho`}
          >
            {adicionado ? "Adicionado ✓" : "Adicionar"}
          </button>
        )}
        {/* O aria-label do botão esconde o "Adicionado ✓"; o anúncio vem daqui. */}
        <span className="vh" role="status">{adicionado ? `${produto.nome} adicionado ao carrinho` : ""}</span>
      </div>
    </article>
  );
}
