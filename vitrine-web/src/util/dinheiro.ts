const formatador = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

export function formatarPreco(valor: number): string {
  return formatador.format(valor);
}

/** Somas de dinheiro em centavos inteiros: 0.1 + 0.2 não vira 0.30000000000000004. */
export function emCentavos(valor: number): number {
  return Math.round(valor * 100);
}

export function formatarCentavos(centavos: number): string {
  return formatador.format(centavos / 100);
}
