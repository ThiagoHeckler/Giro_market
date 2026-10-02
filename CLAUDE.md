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
├── estoque-web/       # React + Vite (painel do estoque)
├── design/            # tokens e brand book da identidade Girô
├── scripts/           # popular-demo.sh (dados de demonstração via APIs)
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
- **React 18 + Vite + TypeScript** na `vitrine-web` e no `estoque-web`.
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

## Como rodar

Antes da primeira vez: `cp .env.example .env` e preencha os tokens e a senha do operador. O compose sobe tudo: Postgres do estoque em **15433** e do mercado em **15434**, tag-worker em 18000, estoque em 18081, mercado em 18080, vitrine em 15173 e painel em 15174 (portas altas para não colidir com outros projetos locais; todas ajustáveis por variável no `.env`). As UIs são servidas por nginx, que encaminha `/api` para o serviço.

```bash
docker compose up -d --build --wait
scripts/popular-demo.sh
```

Em desenvolvimento, só a infraestrutura no compose e os serviços fora dele:

```bash
docker compose up -d estoque-db mercado-db tag-worker
(cd estoque-service && ./mvnw spring-boot:run)     # http://localhost:18081
(cd mercado-service && ./mvnw spring-boot:run)     # http://localhost:18080
(cd vitrine-web && npm install && npm run dev)     # http://localhost:15173 (proxy /api → mercado)
(cd estoque-web && npm install && npm run dev)     # http://localhost:15174 (proxy /api → estoque)
```

Testes: `./mvnw test` em cada serviço Java (Docker precisa estar no ar), `uv run pytest` no `tag-worker`, `npm test` na `vitrine-web` e no `estoque-web`.

`GROQ_API_KEY` fica só no `.env` da raiz (ignorado pelo git; o compose e o worker leem de lá). **Nunca** escreva a chave em código, commit ou log. Sem chave, o worker usa o classificador por palavras-chave (é o modo dos testes).

## Estado atual (atualizado em 2026-10-02)

Branch de trabalho: `developer` (a `main` só recebe merge — o calendário do GitHub só conta commits na branch padrão). Passos 1–7 da seção 11 concluídos. Próximo: as pendências abaixo.

| Passo | Entregue |
|---|---|
| 1 | Fundação do estoque: entidades, migrations, contratos, Testcontainers |
| 2 | Mercado: `ProdutoVitrine` com regra min/max, checkout com reserva (`SELECT … FOR UPDATE`), gatilho, consumidor idempotente |
| 3 | Publicador da outbox nos dois serviços (`FOR UPDATE SKIP LOCKED`, backoff exponencial) + `POST /eventos`; estoque atende `ReposicaoSolicitada` (FEFO) |
| 4 | `tag-worker` (FastAPI + Groq `openai/gpt-oss-20b`, JSON Schema estrito); `POST /lotes` com classificação fora da transação e atendimento da demanda reprimida; reclassificação agendada; evento `ProdutoClassificado` |
| 5 | API REST do mercado (produtos, cadastro, pedidos) + `vitrine-web` (React 18, TanStack Query, tokens gerados) + `scripts/popular-demo.sh` |
| 6 | `estoque-web` (painel: KPIs, entrada de lote, demanda reprimida, reposições recentes); `GET /painel/*` no estoque; tabela `reposicao_expedida` (V9) |
| 7 | Stack completa no `docker-compose` (imagens dos serviços Java e das UIs com nginx); Spring Security: token entre serviços + login do operador (seção 12 do ARCHITECTURE.md) |

### Decisões já tomadas (não relitigar sem motivo novo)

- **`ESGOTADO` só com prateleira em 0.** `ReposicaoNegada` com unidades na prateleira mantém a venda até zerar.
- **`SolicitacaoReposicao`** (mercado) responde `temSolicitacaoPendente`: estados `PENDENTE`, `AGUARDANDO_LOTE` (negada por falta de saldo — continua bloqueando novos pedidos, o estoque atende sozinho na entrada de lote), `ATENDIDA`, `CANCELADA` (SKU desconhecido). Índice único parcial garante uma em aberto por SKU.
- **Um `ReposicaoEnviada` = um lote** (rastreabilidade para recall). Pedido maior que o lote envia o que o lote tem; o mercado reavalia o gatilho após cada crédito e pede o resto.
- **Saldo por lote** (`lote.quantidade_disponivel`), expedição FEFO ignorando vencidos. Invariante no domínio: `saldo_disponivel` = soma dos lotes; `Lote` só nasce por `ProdutoEstoque.receberLote`.
- **Transporte de eventos:** `POST {destino}/eventos` com header `Evento-Tipo`; 204 também para duplicata; 400 para evento inválido. Entrega at-least-once. Autenticado com o token de serviço do destino.
- **Contratos** implementam a interface selada `EventoIntegracao` (`ReposicaoSolicitada`, `ReposicaoEnviada`, `ReposicaoNegada`, `ProdutoClassificado`); `motivo` é o enum `MotivoNegacao`. Fixtures JSON idênticos em `src/test/resources/contratos/` dos dois serviços.
- **`ProdutoClassificado`**: publicado na mesma transação que grava as tags (`ClassificacaoDeProduto`); o mercado só aplica classificação mais nova (`classificado_em`). No cadastro, o mercado herda a classificação via `GET /produtos/{sku}` do estoque.
- **Cadastro na vitrine** entra com prateleira 0 e o próprio gatilho pede o primeiro lote. SKU desconhecido no estoque → 422; estoque fora do ar → cadastra sem classificação.
- **Colunas além da seção 6:** `versao` (`@Version`) em `ProdutoEstoque`/`ProdutoVitrine`; `preco`, `categoria`, `classificado_em` em `produto_vitrine`; `categoria` em `produto_estoque`; controle de reenvio na outbox (`tentativas`, `proxima_tentativa_em`, `ultimo_erro`, `enviado_em`).
- **Vitrine:** "Avise-me" do mockup virou "Reposição a caminho" (não prometer notificação que não existe); sem frete nem preço de oferta (não existem no backend); contagem da reserva na tela do pedido (a reserva nasce no checkout).
- **Painel do estoque em React** (`estoque-web`, app separado da vitrine), não Thymeleaf + HTMX como previa a primeira versão do ARCHITECTURE.md. Só lê dados do estoque: nada de "abaixo do mínimo" ou "esgotados no mercado" (isso é do mercado). KPI "sem saldo válido" conta saldo **expedível** (zerado ou só lotes vencidos); status da reposição = entrega do evento na outbox (`enviado_em`).
- **`reposicao_expedida`**: um registro por `ReposicaoEnviada`, na mesma transação (recall por lote + histórico). A V9 recupera os envios antigos a partir da outbox.
- **Relógio** (`Clock`) no fuso `America/Sao_Paulo` — pesa só em datas civis (validade de lote).
- **Segurança** (seção 12 do ARCHITECTURE.md): um token por serviço (quem chama manda o do destino) e um operador só, com sessão + CSRF no padrão SPA (`csrf().spa()`). Uma `SecurityFilterChain` por serviço com regra por rota e `denyAll` no resto. `GET /produtos/{sku}` do estoque aceita os dois papéis (mercado no cadastro, painel na entrada de lote). Checkout anônimo e sem CSRF.
- **Compose:** estoque e mercado não dependem um do outro para subir (a outbox reenvia). nginx resolve o upstream a cada requisição (`resolver 127.0.0.11`), então sobe com o serviço fora do ar.

### Pendências conhecidas

- Expiração da reserva (devolver unidades à prateleira) e confirmação de pagamento — não implementadas.
- Evento que recebe 400 é reenviado para sempre (com backoff até 5 min): falta status `FAILED`/dead-letter.
- O tag-worker não autentica (só na rede do compose; a porta no host é para dev).

### Convenções que surgiram na prática

- **Clientes HTTP Java sempre com `HttpClient.Version.HTTP_1_1`.** O padrão do JDK tenta upgrade h2c e o uvicorn descarta o corpo (bug real, pego pelo `ContratoTagWorkerTest`).
- Portas de saída transacionais usam `@Transactional(propagation = MANDATORY)` (`Outbox`, `Inbox`, `GatilhoReposicao`, `Expedicao`, `ClassificacaoDeProduto`): chamar fora de transação de negócio falha.
- Inbox grava com `INSERT … ON CONFLICT DO NOTHING` e o efeito do evento commita junto.
- Chamadas HTTP a outro serviço **fora** da transação de banco (worker de tags, consulta ao estoque), com timeout curto; falha nunca bloqueia a operação principal.
- Testes de integração estendem `IntegracaoTest` (um contexto e um container para a suíte, `TRUNCATE` a cada teste, agendadores desligados). `DestinoFalso`/`WorkerFalso` são servidores HTTP do JDK que fazem o papel do outro serviço. `ContratoTagWorkerTest` sobe o tag-worker real a partir do `Dockerfile`.
- Todo `switch` sobre `EventoIntegracao` é exaustivo — novo evento quebra a compilação onde precisa ser tratado.
- `vitrine-web` e `estoque-web`: nenhuma cor/medida solta (cada app tem sua cópia de `scripts/gerar-tokens.mjs`, como os contratos); `src/styles/tokens.css` é gerado por `npm run tokens` (roda sozinho antes de `dev`, `build` e `test`) e não vai para o git. Tons suaves via `color-mix()` sobre tokens.
- Segredos só no `.env` da raiz (modelo em `.env.example`). Os serviços Java o importam em dev (`spring.config.import: optional:file:../.env[.properties]`); no contêiner valem as variáveis de ambiente.
- Testes de rota protegida: `comoServico()`, `comoOperador()` e `comCsrf()` do `IntegracaoTest`. **Não use o `csrf()` do spring-security-test**: ele troca para sempre o repositório do `CsrfFilter` (bean compartilhado entre os testes) por um de sessão, e os testes do cookie passam a falhar conforme a ordem. `DestinoFalso` exige o token, então todo teste de envio já cobre o header.
- Fluxo de trabalho: um passo por vez, testes verdes, parar para revisão; commits em unidades lógicas, cada um verificado isoladamente (build + testes num `git worktree`).
