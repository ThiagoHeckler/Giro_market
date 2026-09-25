-- Lote recebido no almoxarifado.
CREATE TABLE lote (
    id           BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku          VARCHAR(14)  NOT NULL REFERENCES produto_estoque (sku),
    codigo_lote  VARCHAR(50)  NOT NULL,
    quantidade   INTEGER      NOT NULL,
    validade     DATE,
    recebido_em  TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uk_lote_sku_codigo   UNIQUE (sku, codigo_lote),
    CONSTRAINT ck_lote_quantidade   CHECK (quantidade > 0)
);
