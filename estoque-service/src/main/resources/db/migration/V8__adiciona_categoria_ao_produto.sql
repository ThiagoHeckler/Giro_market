-- Categoria sugerida pelo tag-worker; só posicionamento na vitrine. NULL enquanto não classificado.
ALTER TABLE produto_estoque ADD COLUMN categoria VARCHAR(30);
