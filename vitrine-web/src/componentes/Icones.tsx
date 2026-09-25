/** Ícones de traço, herdando a cor do texto. Decorativos: o rótulo vem do elemento em volta. */
const base = {
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 2,
  strokeLinecap: "round" as const,
  strokeLinejoin: "round" as const,
  "aria-hidden": true,
};

export const IconeBusca = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" {...base}><circle cx="11" cy="11" r="7" /><path d="m20 20-3.2-3.2" /></svg>
);

export const IconeCarrinho = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" {...base}>
    <circle cx="9" cy="20" r="1.4" /><circle cx="18" cy="20" r="1.4" />
    <path d="M2 3h3l2.4 12.4a1.5 1.5 0 0 0 1.5 1.2h8.2a1.5 1.5 0 0 0 1.5-1.2L21 7H6" />
  </svg>
);

export const IconeRelogio = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" {...base}><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></svg>
);

export const IconeLixeira = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" {...base}><path d="M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14" /></svg>
);

export const IconeVoltar = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" {...base}><path d="M15 18l-6-6 6-6" /></svg>
);
