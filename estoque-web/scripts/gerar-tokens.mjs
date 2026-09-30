// Gera src/styles/tokens.css a partir de design/tokens.json — a única fonte de cores, tipos e medidas.
import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const raiz = dirname(fileURLToPath(import.meta.url));
const origem = resolve(raiz, "../../design/tokens.json");
const destino = resolve(raiz, "../src/styles/tokens.css");
const tokens = JSON.parse(readFileSync(origem, "utf8"));

const claro = [];
const escuro = [];
for (const { name, value } of tokens.color.tokens) {
  claro.push(`  --color-${name}: ${value.light};`);
  escuro.push(`  --color-${name}: ${value.dark};`);
}
for (const { name, value } of tokens.shadow.tokens) {
  claro.push(`  --${name}: ${value.light};`);
  escuro.push(`  --${name}: ${value.dark};`);
}

const fixos = [];
for (const [familia, pilha] of Object.entries(tokens.type.families)) {
  fixos.push(`  --font-${familia}: ${pilha};`);
}
for (const grupo of tokens.type.groups) {
  for (const estilo of grupo.styles) {
    fixos.push(`  --type-${estilo.name}-family: var(--font-${grupo.family});`);
    fixos.push(`  --type-${estilo.name}-size: ${estilo.fontSize};`);
    fixos.push(`  --type-${estilo.name}-line: ${estilo.lineHeight};`);
    fixos.push(`  --type-${estilo.name}-weight: ${estilo.fontWeight};`);
  }
}
for (const grupo of [tokens.spacing, tokens.radius]) {
  for (const { name, value } of grupo.tokens) fixos.push(`  --${name}: ${value};`);
}

const css = `/* GERADO por scripts/gerar-tokens.mjs a partir de design/tokens.json — não edite à mão. */
:root {
  color-scheme: light dark;
${fixos.join("\n")}
${claro.join("\n")}
}

@media (prefers-color-scheme: dark) {
  :root {
${escuro.map((l) => "  " + l).join("\n")}
  }
}
`;

mkdirSync(dirname(destino), { recursive: true });
writeFileSync(destino, css);
console.log(`tokens.css gerado: ${tokens.color.tokens.length} cores, tema claro e escuro`);
