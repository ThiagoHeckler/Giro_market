import { tomCategoria } from "../util/categorias";

interface Props {
  nome: string;
  categoria: string | null;
  tamanho?: "p" | "m" | "g";
  apagado?: boolean;
}

/** Sem fotos de produto por enquanto: iniciais sobre o tom da categoria, como nos mockups. */
export function Monograma({ nome, categoria, tamanho = "m", apagado = false }: Props) {
  const iniciais = nome.replace(/[^\p{L}\p{N}]/gu, "").slice(0, 2);
  return (
    <div className={`monograma monograma--${tamanho} tom--${tomCategoria(categoria)}${apagado ? " monograma--apagado" : ""}`} aria-hidden="true">
      <span>{iniciais.charAt(0).toUpperCase() + iniciais.slice(1).toLowerCase()}</span>
    </div>
  );
}
