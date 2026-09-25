-- Classificação vinda do estoque (evento ProdutoClassificado); só posicionamento na vitrine.
-- classificado_em guarda o ocorridoEm do evento aplicado: um evento mais antigo que chegue
-- atrasado não sobrescreve uma classificação mais nova.
ALTER TABLE produto_vitrine
    ADD COLUMN categoria       VARCHAR(30),
    ADD COLUMN classificado_em TIMESTAMPTZ;
