import { haQuantoTempo, quando } from "./tempo";

const agora = new Date("2026-09-29T15:00:00-03:00");

describe("haQuantoTempo", () => {
  it.each([
    ["2026-09-29T14:59:30-03:00", "agora"],
    ["2026-09-29T14:55:00-03:00", "há 5 minutos"],
    ["2026-09-29T12:00:00-03:00", "há 3 horas"],
    ["2026-09-27T15:00:00-03:00", "há 2 dias"],
  ])("%s → %s", (instante, esperado) => {
    expect(haQuantoTempo(instante, agora)).toBe(esperado);
  });
});

describe("quando", () => {
  it("mostra só a hora para hoje", () => {
    expect(quando("2026-09-29T09:42:00-03:00", agora)).toMatch(/^\d{2}:\d{2}$/);
  });

  it("mostra dia e hora para dias anteriores", () => {
    expect(quando("2026-09-20T09:42:00-03:00", agora)).toMatch(/^\d{2}\/\d{2},? \d{2}:\d{2}$/);
  });
});
