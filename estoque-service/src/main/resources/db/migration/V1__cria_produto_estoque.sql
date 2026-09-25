-- Produto do almoxarifado. O SKU (EAN/GTIN) é a identidade canônica compartilhada com o mercado.
CREATE TABLE produto_estoque (
    sku              VARCHAR(14)  PRIMARY KEY,
    descricao        VARCHAR(255) NOT NULL,
    ncm              CHAR(8)      NOT NULL,
    -- NULL = ainda não classificado pelo tag-worker (reprocessar depois). Tags nunca são identidade.
    tags             JSONB,
    saldo_disponivel INTEGER      NOT NULL DEFAULT 0,
    -- Lock otimista: evita que duas transferências concorrentes sobrescrevam o saldo.
    versao           BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT ck_produto_estoque_sku_gtin  CHECK (sku ~ '^([0-9]{8}|[0-9]{12,14})$'),
    CONSTRAINT ck_produto_estoque_ncm       CHECK (ncm ~ '^[0-9]{8}$'),
    CONSTRAINT ck_produto_estoque_tags_array CHECK (tags IS NULL OR jsonb_typeof(tags) = 'array'),
    CONSTRAINT ck_produto_estoque_saldo     CHECK (saldo_disponivel >= 0)
);

-- Produtos pendentes de classificação, para o reprocessamento de tags.
CREATE INDEX ix_produto_estoque_sem_tags ON produto_estoque (sku) WHERE tags IS NULL;
