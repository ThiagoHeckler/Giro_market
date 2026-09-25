-- Pedido de reposição negado por falta de saldo. Atendido automaticamente na entrada de lote.
CREATE TABLE demanda_reprimida (
    id                   BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku                  VARCHAR(14)  NOT NULL REFERENCES produto_estoque (sku),
    qtd_solicitada       INTEGER      NOT NULL,
    -- eventId da ReposicaoSolicitada que originou a demanda; UNIQUE torna o registro idempotente.
    solicitacao_original UUID         NOT NULL,
    atendida             BOOLEAN      NOT NULL DEFAULT FALSE,
    registrado_em        TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uk_demanda_reprimida_solicitacao UNIQUE (solicitacao_original),
    CONSTRAINT ck_demanda_reprimida_qtd         CHECK (qtd_solicitada > 0)
);

-- Varredura da entrada de lote: demandas em aberto de um SKU, das mais antigas para as mais novas.
CREATE INDEX ix_demanda_reprimida_aberta ON demanda_reprimida (sku, registrado_em) WHERE NOT atendida;
