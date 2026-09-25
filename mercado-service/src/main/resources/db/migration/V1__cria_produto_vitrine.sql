-- Produto exposto na vitrine. O SKU (EAN/GTIN) é a identidade canônica compartilhada com o estoque.
CREATE TABLE produto_vitrine (
    sku                VARCHAR(14)   PRIMARY KEY,
    nome               VARCHAR(255)  NOT NULL,
    -- Só posicionamento na vitrine; NULL = ainda não classificado. Nunca é identidade.
    tags               JSONB,
    preco              NUMERIC(10,2) NOT NULL,
    estoque_prateleira INTEGER       NOT NULL,
    estoque_minimo     INTEGER       NOT NULL,
    estoque_ideal      INTEGER       NOT NULL,
    status             VARCHAR(10)   NOT NULL,
    -- Lock otimista; o checkout ainda trava a linha com SELECT ... FOR UPDATE.
    versao             BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT ck_produto_vitrine_sku_gtin   CHECK (sku ~ '^([0-9]{8}|[0-9]{12,14})$'),
    CONSTRAINT ck_produto_vitrine_tags_array CHECK (tags IS NULL OR jsonb_typeof(tags) = 'array'),
    CONSTRAINT ck_produto_vitrine_preco      CHECK (preco > 0),
    -- Última linha de defesa contra overselling: a prateleira nunca fica negativa.
    CONSTRAINT ck_produto_vitrine_prateleira CHECK (estoque_prateleira >= 0),
    CONSTRAINT ck_produto_vitrine_min_max    CHECK (estoque_minimo >= 1 AND estoque_ideal >= estoque_minimo),
    CONSTRAINT ck_produto_vitrine_status     CHECK (status IN ('DISPONIVEL', 'ESGOTADO'))
);
