import { Link } from "react-router-dom";

export function PaginaNaoEncontrada() {
  return (
    <div className="estado">
      <h1 className="titulo-pagina">Não encontramos esta página</h1>
      <Link to="/" className="botao botao--primario">Voltar à vitrine</Link>
    </div>
  );
}
