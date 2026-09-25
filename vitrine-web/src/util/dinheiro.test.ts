import { emCentavos, formatarCentavos, formatarPreco } from "./dinheiro";

// Intl usa espaço não separável entre "R$" e o valor.
const semNbsp = (s: string) => s.replace(/ /g, " ");

it("formata em reais", () => {
  expect(semNbsp(formatarPreco(8.99))).toBe("R$ 8,99");
  expect(semNbsp(formatarCentavos(123456))).toBe("R$ 1.234,56");
});

it("converte para centavos arredondando", () => {
  expect(emCentavos(8.99)).toBe(899);
  expect(emCentavos(0.1 + 0.2)).toBe(30);
});
