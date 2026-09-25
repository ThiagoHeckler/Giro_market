# CLAUDE.md — Estoque 2.0

Guia para o Claude Code trabalhar neste repositório. Leia antes de gerar código.

## O que é

Dois sistemas separados e integrados por eventos: um **mercado virtual** (vitrine) e um **estoque (almoxarifado)**. O mercado vende; quando a prateleira cai abaixo do mínimo, um processo de fundo pede reposição ao estoque, que transfere um lote. Projeto de portfólio — as decisões precisam ser defensáveis e o código, de qualidade de produção.

A especificação completa está em `ARCHITECTURE.md`. Ela é a fonte da verdade; se algo aqui divergir, `ARCHITECTURE.md` vence.

## Monorepo

```
estoque-2.0/
├── estoque-service/   # Java 25 + Spring Boot 4
├── mercado-service/   # Java 25 + Spring Boot 4
├── tag-worker/        # Python 3.12 + FastAPI (classificação de tags via LLM)
├── vitrine-web/       # React + Vite (vitrine do mercado)
├── design/            # tokens e brand book da identidade Girô
├── docker-compose.yml
├── ARCHITECTURE.md
└── CLAUDE.md
```

## Stack e versões (fixas)

- **Java 25 LTS** + **Spring Boot 4** / Spring Framework 7 — usar virtual threads, `record`, sealed types, pattern matching.
- **PostgreSQL 17** — um banco por serviço, sem FK cruzada entre bancos.
- **Flyway** para migrations (nunca `ddl-auto=update` fora de teste).
- **JPA 3.2 / Hibernate 7**.
- **JUnit 5 + Testcontainers** — Postgres real nos testes, nunca H2.
- **Python 3.12 + FastAPI + Pydantic v2** no `tag-worker`.
- **React 18 + Vite + TypeScript** na `vitrine-web`.
- Idioma do domínio e dos comentários: **português**. Nomes de classe/variável em português quando forem termos de domínio (`ProdutoVitrine`, `DemandaReprimida`).

## Regras inegociáveis (é o que dá valor ao projeto)

1. **Núcleo determinístico.** A lógica de reposição é aritmética pura: caiu abaixo de `estoqueMinimo`, pede `estoqueIdeal - estoquePrateleira`. **Nenhum LLM** decide quantidade, momento ou se repõe. LLM só na periferia: classificar tags e resumir demanda.
2. **Reposição no ponto de reposição (min/max)**, nunca uma unidade por venda. Gatilho: `estoquePrateleira < estoqueMinimo && !temSolicitacaoPendente(sku)`.
3. **Transactional Outbox.** Todo evento publicado é gravado em `outbox_event` **na mesma transação** que muda o estado. Um `@Scheduled` faz `SELECT ... FOR UPDATE SKIP LOCKED` e publica. Nunca publique um evento fora da transação de negócio.
4. **Idempotência no consumo.** Todo consumidor checa `inbox_event` (PK = `eventId`) antes de processar. Duplicatas são esperadas.
5. **Reserva no checkout, não no pagamento.** Evita overselling do último item.
6. **SKU/EAN é a identidade canônica** entre os dois sistemas. **Tags só decidem posicionamento** na vitrine — nunca são chave de identidade.
7. **O worker de tags nunca está no caminho crítico da venda.** Se ele cair, o produto é salvo sem tags e reprocessado depois. A venda jamais depende dele.
8. **Contratos de evento duplicados** nos dois serviços (não um módulo compartilhado) — mantém a independência real entre eles. São `record` + Bean Validation.

## Convenções de código

- Camadas por serviço: `domain` (entidades, regras), `application` (casos de uso), `infra` (JPA, outbox, clients), `web` (controllers). Domínio não depende de Spring.
- DTO de entrada/saída separado das entidades JPA. Nunca exponha entidade direto no controller.
- Migrations Flyway versionadas: `V1__cria_produto.sql`, `V2__...`. Uma migration por mudança, nunca editar uma já aplicada.
- Testes: todo caso de uso de reposição tem teste de integração com Testcontainers cobrindo os casos de borda (zerado → esgotado + demanda reprimida; duplicata de evento; reserva concorrente).
- Sem `System.out`; use logging estruturado.

## Commits

Mensagens em português, no imperativo (`Adiciona outbox no mercado-service`). Um commit por unidade lógica.

## Como rodar (quando existir)

```bash
docker compose up -d        # sobe os dois Postgres + serviços
# estoque-service : http://localhost:8081
# mercado-service : http://localhost:8080
# tag-worker      : http://localhost:8000
# vitrine-web     : http://localhost:5173
```
