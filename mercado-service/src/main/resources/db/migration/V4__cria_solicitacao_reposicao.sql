-- Pedido de reposição feito ao estoque. Responde temSolicitacaoPendente(sku) no gatilho.
-- AGUARDANDO_LOTE = negado por falta de saldo; o estoque registrou a demanda reprimida e
-- vai atendê-la sozinho quando entrar lote novo, então continua bloqueando novos pedidos.
CREATE TABLE solicitacao_reposicao (
    id             BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- eventId da ReposicaoSolicitada; volta como correlationId na resposta do estoque.
    event_id       UUID        NOT NULL,
    sku            VARCHAR(14) NOT NULL REFERENCES produto_vitrine (sku),
    qtd_solicitada INTEGER     NOT NULL,
    status         VARCHAR(20) NOT NULL,
    criado_em      TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_solicitacao_reposicao_event_id UNIQUE (event_id),
    CONSTRAINT ck_solicitacao_reposicao_qtd      CHECK (qtd_solicitada > 0),
    CONSTRAINT ck_solicitacao_reposicao_status   CHECK (status IN ('PENDENTE', 'AGUARDANDO_LOTE', 'ATENDIDA', 'CANCELADA'))
);

-- No máximo uma solicitação em aberto por SKU, garantido pelo banco mesmo sob concorrência.
CREATE UNIQUE INDEX uk_solicitacao_reposicao_ativa_por_sku
    ON solicitacao_reposicao (sku) WHERE status IN ('PENDENTE', 'AGUARDANDO_LOTE');
