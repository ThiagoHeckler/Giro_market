interface Props {
  tamanho?: number;
  className?: string;
}

/** Marca Girô: seta de giro em volta do item na prateleira. */
export function MarcaGiro({ tamanho = 28, className }: Props) {
  return (
    <svg width={tamanho} height={tamanho} viewBox="0 0 96 96" fill="none" aria-hidden="true" className={className}>
      <path d="M72.4 39.1 A26 26 0 1 1 61 25.5" style={{ stroke: "var(--color-brand)" }} strokeWidth="9" strokeLinecap="round" />
      <path d="M73.1 32.5 L52.9 33.5 L63.9 14.5 Z" style={{ fill: "var(--color-brand)" }} />
      <circle cx="48" cy="48" r="9" style={{ fill: "var(--color-accent)" }} />
    </svg>
  );
}
