import { useQuery } from "@tanstack/react-query";
import { estoque, type Resumo } from "../api/estoque";
import { FalhaAoCarregar } from "./Estados";

type Tom = "neutro" | "perigo" | "alerta" | "brand-2";

const KPIS: { chave: keyof Resumo; rotulo: string; tom: Tom }[] = [
  { chave: "skusCadastrados", rotulo: "SKUs cadastrados", tom: "neutro" },
  { chave: "skusSemSaldo", rotulo: "SKUs sem saldo válido", tom: "perigo" },
  { chave: "demandasAbertas", rotulo: "Demanda reprimida", tom: "alerta" },
  { chave: "lotesVencendo", rotulo: "Lotes vencendo em 30 dias", tom: "brand-2" },
];

const numero = new Intl.NumberFormat("pt-BR");

export function Kpis() {
  const resumo = useQuery({ queryKey: ["painel", "resumo"], queryFn: estoque.resumo });

  if (resumo.isError) {
    return <FalhaAoCarregar erro={resumo.error} aoTentarDeNovo={() => resumo.refetch()} />;
  }

  return (
    <dl className="kpis" aria-busy={resumo.isPending}>
      {KPIS.map(({ chave, rotulo, tom }) => (
        <div key={chave} className={`kpi kpi--${tom}`}>
          <dt className="kpi__rotulo">{rotulo}</dt>
          <dd className="kpi__valor">{resumo.data ? numero.format(resumo.data[chave]) : "—"}</dd>
        </div>
      ))}
    </dl>
  );
}
