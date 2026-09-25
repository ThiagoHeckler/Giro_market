import { Link } from "react-router-dom";
import { rotuloCategoria } from "../util/categorias";

interface Props {
  categorias: string[];
  ativa: string | null;
}

export function ChipsCategoria({ categorias, ativa }: Props) {
  return (
    <nav className="chips" aria-label="Categorias">
      <Link to="/" className={`chip${ativa === null ? " chip--ativo" : ""}`} aria-current={ativa === null ? "page" : undefined}>
        Todos
      </Link>
      {categorias.map((c) => (
        <Link
          key={c}
          to={`/?categoria=${encodeURIComponent(c)}`}
          className={`chip${ativa === c ? " chip--ativo" : ""}`}
          aria-current={ativa === c ? "page" : undefined}
        >
          {rotuloCategoria(c)}
        </Link>
      ))}
    </nav>
  );
}
