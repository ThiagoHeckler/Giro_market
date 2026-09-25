# Estoque 2.0

Mercado virtual cujo estoque se repõe sozinho. Dois sistemas independentes — **mercado** (vitrine) e **estoque** (almoxarifado) — que conversam por eventos e disparam reposição automática de prateleira, sem broker externo. Projeto de portfólio com foco em engenharia backend.

Marca da vitrine: **Girô** — *o estoque que gira sozinho.*

## Como funciona (resumo)

Uma venda debita a prateleira. Quando o saldo cai abaixo do mínimo, um processo de fundo pede reposição ao estoque, que transfere um lote até o nível ideal. Se o estoque está zerado, o produto vira *esgotado* e a demanda fica registrada; quando um lote novo entra, a reposição dispara sozinha.

Detalhes completos em [`ARCHITECTURE.md`](./ARCHITECTURE.md).

## Serviços

| Serviço | Stack | Porta |
|---|---|---|
| `mercado-service` | Java 25 + Spring Boot 4 | 8080 |
| `estoque-service` | Java 25 + Spring Boot 4 | 8081 |
| `tag-worker` | Python 3.12 + FastAPI | 8000 |
| `vitrine-web` | React + Vite | 5173 |

## Rodando

```bash
docker compose up -d
```

## Princípios

- Núcleo de reposição **determinístico e auditável**; LLM só na periferia (tags, resumos).
- Comunicação por **Transactional Outbox** sobre PostgreSQL, consumo **idempotente** (inbox).
- **SKU/EAN** é a identidade entre os sistemas; tags só posicionam na vitrine.

## Documentos

- [`ARCHITECTURE.md`](./ARCHITECTURE.md) — arquitetura, modelo de dados, contratos de evento, casos de borda.
- [`CLAUDE.md`](./CLAUDE.md) — regras e convenções para desenvolvimento assistido.
- [`START-HERE.md`](./START-HERE.md) — roteiro de arranque com o Claude Code.
- [`design/BRAND.md`](./design/BRAND.md) — identidade Girô e tokens.
