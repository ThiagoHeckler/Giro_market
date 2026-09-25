/** Rótulos das categorias fixas do tag-worker. Categoria só posiciona o produto na vitrine. */
const ROTULOS: Record<string, string> = {
  bebidas: "Bebidas",
  laticinios: "Laticínios",
  mercearia: "Mercearia",
  hortifruti: "Hortifrúti",
  padaria: "Padaria",
  carnes: "Carnes",
  congelados: "Congelados",
  limpeza: "Limpeza",
  higiene: "Higiene",
  pet: "Pet",
  outros: "Outros",
};

export type Tom = "brand" | "accent" | "brand-2" | "highlight";

/** Tom do monograma por categoria — sempre um token de marca, misturado no CSS. */
const TONS: Record<string, Tom> = {
  bebidas: "brand",
  carnes: "brand",
  hortifruti: "accent",
  laticinios: "accent",
  congelados: "brand-2",
  limpeza: "brand-2",
  higiene: "brand-2",
  pet: "brand-2",
  mercearia: "highlight",
  padaria: "highlight",
};

export function rotuloCategoria(categoria: string): string {
  return ROTULOS[categoria] ?? categoria.charAt(0).toUpperCase() + categoria.slice(1);
}

export function tomCategoria(categoria: string | null): Tom {
  return (categoria && TONS[categoria]) || "brand";
}
