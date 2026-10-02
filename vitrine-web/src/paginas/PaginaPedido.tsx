import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ErroApi, mercado, type PedidoDetalhe } from "../api/mercado";
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

function prazoTerminou(pedido: PedidoDetalhe, agora: number): boolean {
  return pedido.reservaExpiraEm !== null && new Date(pedido.reservaExpiraEm).getTime() <= agora;
}

const TITULOS: Record<PedidoDetalhe["status"], string> = {
  AGUARDANDO_PAGAMENTO: "Pedido reservado",
  PAGO: "Pedido pago",
  EXPIRADO: "Reserva expirada",
  CANCELADO: "Pedido cancelado",
};

export function PaginaPedido() {
  const { id = "" } = useParams();
  const agora = useAgora();
  const consultas = useQueryClient();
  const consulta = useQuery({
    queryKey: ["pedido", id],
    queryFn: () => mercado.buscarPedido(id),
    // Prazo vencido e ainda aguardando: a varredura do mercado expira o pedido em instantes.
    refetchInterval: (q) =>
      q.state.data?.status === "AGUARDANDO_PAGAMENTO" && prazoTerminou(q.state.data, Date.now()) ? 5_000 : false,
  });
  const pagar = useMutation({
    mutationFn: () => mercado.pagarPedido(id),
    onSuccess: (pago) => consultas.setQueryData(["pedido", id], pago),
    // Recusado (prazo vencido): o pedido mudou no servidor, então a tela busca o estado novo.
    onError: () => consultas.invalidateQueries({ queryKey: ["pedido", id] }),
  });

  if (consulta.isPending) return <Carregando rotulo="Carregando pedido…" />;
  if (consulta.isError) {
    if (consulta.error instanceof ErroApi && consulta.error.status === 404) return <PaginaNaoEncontrada />;
    return <FalhaAoCarregar erro={consulta.error} aoTentarDeNovo={() => consulta.refetch()} />;
  }

  const pedido = consulta.data;
  const aguardando = pedido.status === "AGUARDANDO_PAGAMENTO";
  const restante = pedido.reservaExpiraEm ? new Date(pedido.reservaExpiraEm).getTime() - agora : 0;
  const podePagar = aguardando && restante > 0;

  return (
    <>
      <h1 className="titulo-pagina">{TITULOS[pedido.status]}</h1>
      {podePagar && (
        <p className="aviso aviso--sucesso aviso--com-icone">
          <IconeRelogio />
          <span>
            Itens reservados no estoque por <strong className="valor">{formatarRestante(restante)}</strong> —
            garantimos que ninguém leva o último antes de você concluir o pagamento.
          </span>
        </p>
      )}
      {aguardando && !podePagar && (
        <p className="aviso aviso--neutro">O prazo da reserva terminou. Os itens voltam para a vitrine em instantes.</p>
      )}
      {pedido.status === "PAGO" && (
        <p className="aviso aviso--sucesso" role="status">Pagamento confirmado. Obrigado pela compra!</p>
      )}
      {pedido.status === "EXPIRADO" && (
        <p className="aviso aviso--neutro">O prazo terminou sem pagamento e os itens voltaram para a vitrine.</p>
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

        {podePagar && (
          <>
            <button
              type="button"
              className="botao botao--primario botao--grande"
              onClick={() => pagar.mutate()}
              disabled={pagar.isPending}
            >
              {pagar.isPending ? "Pagando…" : `Pagar ${formatarPreco(pedido.total)}`}
            </button>
            <p className="texto-auxiliar">Pagamento simulado: nenhum valor é cobrado.</p>
          </>
        )}
        {pagar.isError && (
          <p className="aviso aviso--erro" role="alert">
            {pagar.error instanceof ErroApi && pagar.error.status === 409
              ? "O prazo da reserva terminou antes do pagamento."
              : "Não foi possível pagar agora. Tente de novo."}
          </p>
        )}
      </section>

      <Link to="/" className="link-secundario">Voltar à vitrine</Link>
    </>
  );
}
