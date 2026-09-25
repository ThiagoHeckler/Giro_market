import { useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ErroApi, mercado } from "../api/mercado";
import { Carregando, FalhaAoCarregar } from "../componentes/Estados";
import { IconeRelogio } from "../componentes/Icones";
import { formatarPreco } from "../util/dinheiro";
import { PaginaNaoEncontrada } from "./PaginaNaoEncontrada";

function useAgora(intervaloMs = 1000): number {
  const [agora, setAgora] = useState(() => Date.now());
  useEffect(() => {
    const id = window.setInterval(() => setAgora(Date.now()), intervaloMs);
    return () => window.clearInterval(id);
  }, [intervaloMs]);
  return agora;
}

function formatarRestante(ms: number): string {
  const total = Math.max(0, Math.floor(ms / 1000));
  return `${String(Math.floor(total / 60)).padStart(2, "0")}:${String(total % 60).padStart(2, "0")}`;
}

export function PaginaPedido() {
  const { id = "" } = useParams();
  const agora = useAgora();
  const consulta = useQuery({ queryKey: ["pedido", id], queryFn: () => mercado.buscarPedido(id) });

  if (consulta.isPending) return <Carregando rotulo="Carregando pedido…" />;
  if (consulta.isError) {
    if (consulta.error instanceof ErroApi && consulta.error.status === 404) return <PaginaNaoEncontrada />;
    return <FalhaAoCarregar erro={consulta.error} aoTentarDeNovo={() => consulta.refetch()} />;
  }

  const pedido = consulta.data;
  const restante = pedido.reservaExpiraEm ? new Date(pedido.reservaExpiraEm).getTime() - agora : 0;

  return (
    <>
      <h1 className="titulo-pagina">Pedido reservado</h1>
      {restante > 0 ? (
        <p className="aviso aviso--sucesso aviso--com-icone">
          <IconeRelogio />
          <span>
            Itens reservados no estoque por <strong className="valor">{formatarRestante(restante)}</strong> —
            garantimos que ninguém leva o último antes de você concluir o pagamento.
          </span>
        </p>
      ) : (
        <p className="aviso aviso--neutro">O prazo da reserva terminou.</p>
      )}

      <section className="resumo resumo--pedido" aria-labelledby="titulo-itens">
        <h2 id="titulo-itens" className="titulo-cartao">Itens</h2>
        <ul className="lista-pedido">
          {pedido.itens.map((i) => (
            <li key={i.sku} className="resumo__linha">
              <span>{i.qtd}× {i.nome}</span>
              <span className="valor">{formatarPreco(i.precoUnitario * i.qtd)}</span>
            </li>
          ))}
        </ul>
        <hr />
        <div className="resumo__linha resumo__total">
          <span>Total</span>
          <span className="valor">{formatarPreco(pedido.total)}</span>
        </div>
        <span className="codigo">Pedido {pedido.id}</span>
        <p className="texto-auxiliar">O pagamento entra numa próxima etapa do projeto; por enquanto o pedido fica aguardando.</p>
      </section>

      <Link to="/" className="link-secundario">Voltar à vitrine</Link>
    </>
  );
}
