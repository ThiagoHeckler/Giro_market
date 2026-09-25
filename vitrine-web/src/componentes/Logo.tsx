interface Props {
  tamanho?: number;
  /** Uma cor só (currentColor), para usar sobre preenchimento de marca. */
  monocromatico?: boolean;
  className?: string;
}

/** Marca Girô: seta de giro em volta do item na prateleira. */
export function MarcaGiro({ tamanho = 30, monocromatico = false, className }: Props) {
  const seta = monocromatico ? "currentColor" : "var(--color-brand)";
  const item = monocromatico ? "currentColor" : "var(--color-accent)";
  return (
    <svg width={tamanho} height={tamanho} viewBox="0 0 96 96" fill="none" aria-hidden="true" className={className}>
      <path d="M72.4 39.1 A26 26 0 1 1 61 25.5" style={{ stroke: seta }} strokeWidth="9" strokeLinecap="round" />
      <path d="M73.1 32.5 L52.9 33.5 L63.9 14.5 Z" style={{ fill: seta }} />
      <circle cx="48" cy="48" r="9" style={{ fill: item }} />
    </svg>
  );
}
