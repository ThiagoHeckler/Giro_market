# START-HERE — arranque com o Claude Code

Este arquivo é o roteiro de partida. O prompt abaixo é para colar no Claude Code na primeira sessão, dentro da pasta vazia do projeto (com `ARCHITECTURE.md`, `CLAUDE.md` e `design/` já presentes).

---

## Prompt inicial (copie e cole)

> Você vai construir o **Estoque 2.0**. Leia `ARCHITECTURE.md` e `CLAUDE.md` primeiro — são a fonte da verdade e as regras inegociáveis. Não gere código antes de tê-los lido.
>
> Vamos construir na ordem da seção 11 do `ARCHITECTURE.md`, um passo por vez, cada passo com testes passando antes de seguir. **Comece só pelo Passo 1** e pare para eu revisar.
>
> **Passo 1 — `estoque-service`, fundação:**
> 1. Scaffold do projeto: Maven, Java 25, Spring Boot 4, dependências (web, data-jpa, validation, postgresql, flyway, testcontainers, junit).
> 2. Entidades JPA + migrations Flyway para: `ProdutoEstoque`, `Lote`, `DemandaReprimida`, `OutboxEvent`, `InboxEvent` (modelo na seção 6 do `ARCHITECTURE.md`).
> 3. Os três `record` de contrato de evento (seção 5).
> 4. `docker-compose.yml` inicial subindo só o Postgres do estoque.
> 5. Um teste de integração com Testcontainers que sobe o schema via Flyway e grava/lê um `ProdutoEstoque` — provando que a fundação está de pé.
>
> Ao terminar, me mostre a estrutura de pastas, as migrations e o resultado dos testes. Não avance para o `mercado-service`.

---

## Ordem completa (referência — seção 11 do ARCHITECTURE.md)

1. `estoque-service`: entidades JPA + migrations + outbox/inbox  ← **começa aqui**
2. `mercado-service`: entidades + gatilho de reposição + consumer
3. Contratos de evento e o par publisher/consumer com Testcontainers
4. `tag-worker` FastAPI + integração na entrada de lote
5. `vitrine-web` React consumindo o mercado (usa `design/tokens.json`)
6. `estoque-web` React: painel do estoque
7. Docker Compose amarrando tudo

## Critérios para considerar um passo "pronto"

- Migrations aplicam do zero num banco limpo.
- Testes de integração (Testcontainers) verdes, cobrindo os casos de borda da seção 4.
- Nada de LLM no núcleo; outbox na mesma transação; consumo idempotente via inbox.

## A identidade visual

A marca é **Girô** (laranja `#d1490f` + petróleo `#0e7d87`). Os tokens (cores claro/escuro, tipografia, espaçamento, raios) estão em `design/tokens.json` e resumidos em `design/BRAND.md`. As quatro telas de referência (vitrine, produto, carrinho, painel do estoque) estão em `design/mockups/` — traduza a estrutura e o estilo delas para React no Passo 5 (ver `design/mockups/README.md`). A `vitrine-web` consome os tokens — nunca valores soltos. O design system completo, com logo e capa, vive como artefato no Claude e pode ser exportado quando precisar dos arquivos de fonte/imagem.
