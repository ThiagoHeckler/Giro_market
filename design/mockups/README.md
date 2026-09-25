# Mockups de referência — Girô

Estas são as quatro telas do fluxo, como **referência visual** para construir a `vitrine-web` (React) e o painel do estoque. Elas foram desenhadas no canvas de Design do Claude e usam a paleta e a tipografia de `design/tokens.json`.

## Formato

Os arquivos são `.dc.html` (Design Component). **Não são para rodar** — o miolo dentro de `<x-dc>…</x-dc>` é HTML com estilos inline que você **traduz para componentes React**. Ignore o `<script src="./support.js">`, o bloco `<script type="text/x-dc">` e a tag `<helmet>` (isso é do editor); aproveite a estrutura, o espaçamento, as cores (que batem com os tokens) e os textos em português.

## As telas

| Arquivo | Tela | Papel no sistema |
|---|---|---|
| `Main.dc.html` | Vitrine — listagem | Grade de produtos com selos **Em estoque** / **Esgotado**; esgotado troca "Adicionar" por "Avise-me" (gancho da demanda reprimida) |
| `Produto.dc.html` | Produto — detalhe | Galeria, preço/oferta, SKU-EAN em mono, quantidade + carrinho, card de reposição automática |
| `Carrinho.dc.html` | Carrinho — checkout | Faixa de **reserva com cronômetro** (reserva no checkout, não no pagamento), resumo com frete |
| `Estoque.dc.html` | Painel do estoque (admin) | KPIs, **entrada de lote**, tabela de **demanda reprimida**, reposições recentes — chrome em petróleo |

## Ao traduzir para React

- Puxe cores, tipografia, espaçamento e raios de `design/tokens.json` (crie CSS custom properties ou um tema). Nada de hex solto no JSX.
- Componentes naturais: `Header`, `ProductCard` (com `StockBadge`), `ProductGrid`, `CategoryChips`, `CartItem`, `OrderSummary`, e no admin `KpiTile`, `LoteForm`, `DemandaTable`.
- O painel do estoque é operacional (a arquitetura sugere Thymeleaf + HTMX). Se preferir React nele também, tudo bem — mas trate como app interno, não vitrine.
- Acessibilidade já embutida nos mockups: `<button>`, `<a href>`, `<label>` reais. Mantenha isso no React.
