/** Ícones de traço, herdando a cor do texto. Decorativos: o rótulo vem do elemento em volta. */
const base = {
  width: 18,
  height: 18,
  viewBox: "0 0 24 24",
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 2,
  strokeLinecap: "round" as const,
  strokeLinejoin: "round" as const,
  "aria-hidden": true,
};

export const IconeLote = () => (
  <svg {...base}><path d="M3 7l9-4 9 4-9 4-9-4z" /><path d="M3 7v10l9 4 9-4V7" /><path d="M12 11v10" /></svg>
);

export const IconeDemanda = () => (
  <svg {...base}><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></svg>
);

/** Seta de giro: reposição = a prateleira girando. */
export const IconeReposicao = () => (
  <svg {...base}><path d="M20 12a8 8 0 1 1-2.6-5.9" /><path d="M20 4v5h-5" /></svg>
);

export const IconeSair = () => (
  <svg {...base} width={16} height={16}><path d="M9 18l6-6-6-6" /></svg>
);
