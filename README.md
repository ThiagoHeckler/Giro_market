# Estoque 2.0

Mercado virtual cujo estoque se repõe sozinho. Dois sistemas independentes — **mercado** (vitrine) e **estoque** (almoxarifado) — que conversam por eventos e disparam reposição automática de prateleira, sem broker externo. Projeto de portfólio com foco em engenharia backend.

Marca da vitrine: **Girô** — *o estoque que gira sozinho.*

## Como funciona (resumo)

Uma venda debita a prateleira. Quando o saldo cai abaixo do mínimo, um processo de fundo pede reposição ao estoque, que transfere um lote até o nível ideal. Se o estoque está zerado, o produto vira *esgotado* e a demanda fica registrada; quando um lote novo entra, a reposição dispara sozinha.

Detalhes completos em [`ARCHITECTURE.md`](./ARCHITECTURE.md).

## Serviços

| Serviço | Stack | Porta |
|---|---|---|
| `mercado-service` | Java 25 + Spring Boot 4 | 18080 |
| `estoque-service` | Java 25 + Spring Boot 4 | 18081 |
| `tag-worker` | Python 3.12 + FastAPI | 18000 |
| `vitrine-web` | React + Vite (nginx no compose) | 15173 |
| `estoque-web` | React + Vite (nginx no compose) | 15174 |

## Rodando

Tudo em contêineres:

```bash
docker compose up -d --build --wait                    # bancos, serviços, tag-worker e as duas UIs
scripts/popular-demo.sh                                # produtos de demonstração
```

Vitrine em http://localhost:15173 e painel do estoque em http://localhost:15174. Cada UI é servida por um nginx que encaminha `/api` para o seu serviço (mesma origem, sem CORS).

Em desenvolvimento, só a infraestrutura no compose e o resto com recarga:

```bash
docker compose up -d estoque-db mercado-db tag-worker
(cd estoque-service && ./mvnw spring-boot:run)         # :18081
(cd mercado-service && ./mvnw spring-boot:run)         # :18080
(cd vitrine-web && npm install && npm run dev)         # :15173, com proxy /api → mercado
(cd estoque-web && npm install && npm run dev)         # :15174, com proxy /api → estoque
```

As portas do host podem ser trocadas no `.env` (`ESTOQUE_PORTA`, `MERCADO_PORTA`, `VITRINE_PORTA`, `ESTOQUE_WEB_PORTA`, `TAG_WORKER_PORTA`, `ESTOQUE_DB_PORTA`, `MERCADO_DB_PORTA`).
O tag-worker usa a Groq se `GROQ_API_KEY` estiver no `.env` (fora do git); sem ela, classifica por palavras-chave.

## Princípios

- Núcleo de reposição **determinístico e auditável**; LLM só na periferia (tags, resumos).
- Comunicação por **Transactional Outbox** sobre PostgreSQL, consumo **idempotente** (inbox).
- **SKU/EAN** é a identidade entre os sistemas; tags só posicionam na vitrine.

## Documentos

- [`ARCHITECTURE.md`](./ARCHITECTURE.md) — arquitetura, modelo de dados, contratos de evento, casos de borda.
- [`CLAUDE.md`](./CLAUDE.md) — regras e convenções para desenvolvimento assistido.
- [`START-HERE.md`](./START-HERE.md) — roteiro de arranque com o Claude Code.
- [`design/BRAND.md`](./design/BRAND.md) — identidade Girô e tokens.
