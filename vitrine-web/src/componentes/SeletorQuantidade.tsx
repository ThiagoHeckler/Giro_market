interface Props {
  valor: number;
  maximo: number;
  aoMudar: (valor: number) => void;
  rotulo: string;
  compacto?: boolean;
}

export function SeletorQuantidade({ valor, maximo, aoMudar, rotulo, compacto = false }: Props) {
  return (
    <div className={`seletor${compacto ? " seletor--compacto" : ""}`} role="group" aria-label={rotulo}>
      <button type="button" aria-label="Diminuir quantidade" onClick={() => aoMudar(valor - 1)} disabled={valor <= 1}>
        −
      </button>
      <output aria-live="polite">{valor}</output>
      <button type="button" aria-label="Aumentar quantidade" onClick={() => aoMudar(valor + 1)} disabled={valor >= maximo}>
        +
      </button>
    </div>
  );
}
