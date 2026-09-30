const relativo = new Intl.RelativeTimeFormat("pt-BR", { numeric: "always" });
const hora = new Intl.DateTimeFormat("pt-BR", { hour: "2-digit", minute: "2-digit" });
const diaEHora = new Intl.DateTimeFormat("pt-BR", {
  day: "2-digit",
  month: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
});

const MINUTO = 60_000;
const HORA = 60 * MINUTO;
const DIA = 24 * HORA;

/** "há 5 minutos", "há 3 horas", "há 2 dias" — idade de uma demanda em aberto. */
export function haQuantoTempo(instante: string, agora: Date = new Date()): string {
  const decorrido = agora.getTime() - new Date(instante).getTime();
  if (decorrido < MINUTO) return "agora";
  if (decorrido < HORA) return relativo.format(-Math.floor(decorrido / MINUTO), "minute");
  if (decorrido < DIA) return relativo.format(-Math.floor(decorrido / HORA), "hour");
  return relativo.format(-Math.floor(decorrido / DIA), "day");
}

/** Só a hora para hoje; dia e hora para datas anteriores. */
export function quando(instante: string, agora: Date = new Date()): string {
  const data = new Date(instante);
  return data.toDateString() === agora.toDateString() ? hora.format(data) : diaEHora.format(data);
}
