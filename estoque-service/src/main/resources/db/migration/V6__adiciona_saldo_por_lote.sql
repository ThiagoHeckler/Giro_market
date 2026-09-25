-- Saldo por lote: expedição FEFO (vence primeiro, sai primeiro) e rastreio do lote de cada reposição.
-- Invariante mantido pelo domínio: produto_estoque.saldo_disponivel = soma de quantidade_disponivel.
ALTER TABLE lote ADD COLUMN quantidade_disponivel INTEGER;

UPDATE lote SET quantidade_disponivel = quantidade;

ALTER TABLE lote
    ALTER COLUMN quantidade_disponivel SET NOT NULL,
    ADD CONSTRAINT ck_lote_quantidade_disponivel CHECK (quantidade_disponivel BETWEEN 0 AND quantidade);

CREATE INDEX ix_lote_expedicao ON lote (sku, validade NULLS LAST, recebido_em) WHERE quantidade_disponivel > 0;
