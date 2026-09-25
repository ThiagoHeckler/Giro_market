import type { StatusVitrine } from "../api/mercado";

export function SeloEstoque({ status, className = "" }: { status: StatusVitrine; className?: string }) {
  const esgotado = status === "ESGOTADO";
  return (
    <span className={`selo ${esgotado ? "selo--esgotado" : "selo--em-estoque"} ${className}`}>
      {esgotado ? "Esgotado" : "Em estoque"}
    </span>
  );
}
