import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ErroApi, mercado } from "../api/mercado";
import { useCarrinho } from "../carrinho/Carrinho";
import { quantidadeDeItens, totalEmCentavos, type ItemCarrinho } from "../carrinho/estado";
import { Vazio } from "../componentes/Estados";
import { IconeLixeira, IconeVoltar } from "../componentes/Icones";
import { Monograma } from "../componentes/Monograma";
import { SeletorQuantidade } from "../componentes/SeletorQuantidade";
import { emCentavos, formatarCentavos, formatarPreco } from "../util/dinheiro";

/** Problemas que o checkout apontou por item (SKU → mensagem). */
type ProblemasPorItem = Record<string, string>;

export function PaginaCarrinho() {
  const { itens, despachar } = useCarrinho();
  const navegar = useNavigate();
  const clienteConsultas = useQueryClient();
  const [problemas, setProblemas] = useState<ProblemasPorItem>({});
  const [erroGeral, setErroGeral] = useState<string | null>(null);

  const checkout = useMutation({
    mutationFn: () => mercado.fecharPedido(itens.map(({ sku, qtd }) => ({ sku, qtd }))),
    onSuccess: (pedido) => {
      despachar({ tipo: "limpar" });
      clienteConsultas.invalidateQueries({ queryKey: ["produtos"] });
      navegar(`/pedidos/${pedido.pedidoId}`);
    },
    onError: async (erro) => {
      setProblemas({});
      setErroGeral(null);
      if (erro instanceof ErroApi && erro.status === 409 && erro.problema.sku) {
        // Alguém levou antes: atualiza o disponível daquele item e explica.
        const sku = erro.problema.sku;
        const atual = await mercado.buscarProduto(sku).catch(() => null);
        const disponivel = atual?.status === "DISPONIVEL" ? atual.disponivel : 0;
        despachar({ tipo: "atualizarDisponivel", sku, disponivel });
        setProblemas({
          [sku]: disponivel > 0
            ? `Só ${disponivel === 1 ? "resta 1 unidade" : `restam ${disponivel} unidades`} — ajustamos a quantidade.`
            : "Esgotou enquanto você comprava — removemos do carrinho.",
        });
        clienteConsultas.invalidateQueries({ queryKey: ["produtos"] });
      } else if (erro instanceof ErroApi && erro.status === 422) {
        setErroGeral("Um dos produtos saiu da vitrine. Remova-o e tente de novo.");
      } else {
        setErroGeral("Não conseguimos finalizar agora. Seu carrinho continua salvo — tente de novo.");
      }
    },
  });

  if (itens.length === 0) {
    return (
      <>
        <h1 className="titulo-pagina">Seu carrinho</h1>
        {Object.keys(problemas).length > 0 && <p className="aviso aviso--erro" role="alert">{Object.values(problemas)[0]}</p>}
        <Vazio>
          <p>Seu carrinho está vazio.</p>
          <Link to="/" className="botao botao--primario">Ver produtos</Link>
        </Vazio>
      </>
    );
  }

  const total = totalEmCentavos(itens);
  const quantidade = quantidadeDeItens(itens);

  return (
    <>
      <h1 className="titulo-pagina">Seu carrinho</h1>
      <p className="aviso aviso--sucesso">
        Ao finalizar, os itens ficam <strong>reservados na prateleira</strong> até o pagamento — ninguém leva o último
        antes de você.
      </p>

      <div className="carrinho">
        <ul className="carrinho__itens" aria-label="Itens do carrinho">
          {itens.map((item) => (
            <LinhaCarrinho
              key={item.sku}
              item={item}
              problema={problemas[item.sku]}
              aoMudar={(qtd) => despachar({ tipo: "alterar", sku: item.sku, qtd })}
              aoRemover={() => despachar({ tipo: "remover", sku: item.sku })}
            />
          ))}
          <li>
            <Link to="/" className="link-secundario"><IconeVoltar /> Continuar comprando</Link>
          </li>
        </ul>

        <aside className="resumo" aria-labelledby="titulo-resumo">
          <h2 id="titulo-resumo" className="titulo-cartao">Resumo</h2>
          <div className="resumo__linha">
            <span>Subtotal ({quantidade} {quantidade === 1 ? "item" : "itens"})</span>
            <span className="valor">{formatarCentavos(total)}</span>
          </div>
          <hr />
          <div className="resumo__linha resumo__total">
            <span>Total</span>
            <span className="valor">{formatarCentavos(total)}</span>
          </div>
          {erroGeral && <p className="aviso aviso--erro" role="alert">{erroGeral}</p>}
          <button
            type="button"
            className="botao botao--primario botao--grande"
            onClick={() => checkout.mutate()}
            disabled={checkout.isPending}
          >
            {checkout.isPending ? "Reservando…" : "Finalizar compra"}
          </button>
        </aside>
      </div>
    </>
  );
}

interface LinhaProps {
  item: ItemCarrinho;
  problema?: string;
  aoMudar: (qtd: number) => void;
  aoRemover: () => void;
}

function LinhaCarrinho({ item, problema, aoMudar, aoRemover }: LinhaProps) {
  return (
    <li className={`item-carrinho${problema ? " item-carrinho--problema" : ""}`}>
      <Monograma nome={item.nome} categoria={item.categoria} tamanho="p" />
      <div className="item-carrinho__info">
        <Link to={`/produtos/${item.sku}`} className="item-carrinho__nome">{item.nome}</Link>
        <span className="codigo">SKU {item.sku} · {formatarPreco(item.preco)} un.</span>
        {problema && <span className="item-carrinho__problema" role="alert">{problema}</span>}
      </div>
      <SeletorQuantidade valor={item.qtd} maximo={item.disponivel} aoMudar={aoMudar} rotulo={`Quantidade de ${item.nome}`} compacto />
      <span className="valor item-carrinho__subtotal">{formatarCentavos(emCentavos(item.preco) * item.qtd)}</span>
      <button type="button" className="botao-icone botao-icone--perigo" aria-label={`Remover ${item.nome}`} onClick={aoRemover}>
        <IconeLixeira />
      </button>
    </li>
  );
}
