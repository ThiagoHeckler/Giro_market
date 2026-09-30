-- Cada envio de lote para a prateleira do mercado, gravado na mesma transação da ReposicaoEnviada.
-- É o rastro de recall (que envios saíram de um lote) e o histórico do painel do estoque.
CREATE TABLE reposicao_expedida (
    id             BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- eventId da ReposicaoEnviada; é também o id da linha em outbox_event (status de entrega).
    event_id       UUID         NOT NULL,
    -- eventId da ReposicaoSolicitada atendida.
    correlation_id UUID         NOT NULL,
    sku            VARCHAR(14)  NOT NULL REFERENCES produto_estoque (sku),
    lote_id        BIGINT       NOT NULL REFERENCES lote (id),
    qtd            INTEGER      NOT NULL,
    expedido_em    TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uk_reposicao_expedida_evento UNIQUE (event_id),
    CONSTRAINT ck_reposicao_expedida_qtd    CHECK (qtd > 0)
);

CREATE INDEX ix_reposicao_expedida_recentes ON reposicao_expedida (expedido_em DESC);
CREATE INDEX ix_reposicao_expedida_lote ON reposicao_expedida (lote_id);

-- Envios anteriores a esta migration só existem na outbox: recupera o histórico a partir deles.
INSERT INTO reposicao_expedida (event_id, correlation_id, sku, lote_id, qtd, expedido_em)
SELECT o.id, (o.payload ->> 'correlationId')::uuid, l.sku, l.id, (o.payload ->> 'qtd')::int, o.criado_em
FROM outbox_event o
JOIN lote l ON l.sku = o.payload ->> 'sku' AND l.codigo_lote = o.payload ->> 'lote'
WHERE o.tipo = 'ReposicaoEnviada';
