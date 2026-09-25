# Girô — identidade e tokens

Identidade da vitrine do Estoque 2.0. A `vitrine-web` (e o admin) consomem `tokens.json` — nunca cores ou medidas soltas.

**Âncora:** *o estoque que gira sozinho.* O nome vem de *giro de estoque*; o logo é uma seta circular com o item (verde) no centro da prateleira.

## Cores (claro / escuro)

| Token | Claro | Escuro | Uso |
|---|---|---|---|
| `surface` | `#fff8f1` | `#16120e` | Fundo da página |
| `surface-raised` | `#ffffff` | `#201a14` | Cartões, painéis |
| `surface-sunken` | `#f5ebdf` | `#2a221b` | Inputs, listras de tabela |
| `ink` | `#241a11` | `#f7f0e7` | Texto primário |
| `ink-muted` | `#6a5b4b` | `#b6a591` | Texto secundário |
| `border` | `#e8dccb` | `#372f27` | Bordas e divisores |
| `brand` | `#d1490f` | `#f9731a` | Ação primária, marca (preenchimento, não texto) |
| `brand-strong` | `#b23c0a` | `#ff8a3d` | Hover/pressionado do brand |
| `ink-on-brand` | `#ffffff` | `#21130a` | Texto sobre brand |
| `brand-2` | `#0e7d87` | `#2bb3bf` | **Secundária (petróleo):** ações secundárias, links, chrome do painel do estoque |
| `brand-2-strong` | `#0a626b` | `#4ecdd8` | Hover/pressionado do brand-2 |
| `ink-on-brand-2` | `#ffffff` | `#04262a` | Texto sobre brand-2 |
| `brand-2-tint` | `#e2f1f2` | `#123b40` | Fundo suave do brand-2 (pills, nav ativo) |
| `accent` | `#1f8f4e` | `#37c47d` | Frescor, "em estoque" |
| `highlight` | `#f4a91b` | `#ffc44d` | Promoção, selos |
| `success` | `#1f8f4e` | `#37c47d` | Confirmação |
| `danger` | `#cf2a35` | `#ff5a63` | Erro, **esgotado** |
| `info` | `#2563eb` | `#6f9bff` | Aviso neutro (links preferem `brand-2`) |

**Duas cores de marca:** laranja (protagonista, ações primárias) + petróleo (coadjuvante: links, ações secundárias, e a identidade do painel do estoque, que separa visualmente o mercado do almoxarifado). Verde/amarelo/vermelho são **estado**, não decoração: não use como cor de marca. Neutros fazem ~90% da tela. Estados se distinguem por luminosidade além do tom. Todos os pares de texto passam 4.5:1 no fundo indicado, nos dois temas.

## Tipografia

- **Bricolage Grotesque** — títulos (`display`, `title`).
- **Inter** — corpo e rótulos.
- **JetBrains Mono** — dados técnicos: SKU/EAN, preço, lote, `event_id`.

Escala: `display-lg` 44/46 700 · `display` 32/36 700 · `title` 24/28 600 · `body` 15/23 400 · `small` 13/20 400 · `label` 14/16 600 · `code` 13/20 500 (mono).

## Espaçamento e forma

Base 4: `space-1` 4 · `space-2` 8 · `space-3` 12 · `space-4` 16 · `space-6` 24 · `space-8` 32 · `space-12` 48.
Raios: `radius-sm` 6 · `radius-md` 10 (botões/inputs/cartões) · `radius-lg` 16 (tiles/painéis) · `radius-pill` 999.

## Arquivos

- `tokens.json` — todos os tokens em formato consumível (mesma estrutura acima, com notas de uso).
- `giro-mark.svg` — o logo. Sobre fundo claro; em fundo escuro, use o quadro `surface`.
- `mockups/` — as quatro telas de referência (vitrine, produto, carrinho, painel do estoque). Ver `mockups/README.md`.

> O design system completo (com preview vivo dos tokens, brand book e capa) vive como artefato no Claude. Exporte de lá quando precisar de fontes ou imagens adicionais.
