-- Pedido e seus itens formam um único agregado.
CREATE TABLE pedido (
    id        UUID          PRIMARY KEY,
    status    VARCHAR(25)   NOT NULL,
    total     NUMERIC(12,2) NOT NULL,
    criado_em TIMESTAMPTZ   NOT NULL,

    CONSTRAINT ck_pedido_status CHECK (status IN ('AGUARDANDO_PAGAMENTO', 'PAGO', 'CANCELADO')),
    CONSTRAINT ck_pedido_total  CHECK (total >= 0)
);

CREATE TABLE item_pedido (
    id             BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pedido_id      UUID          NOT NULL REFERENCES pedido (id),
    sku            VARCHAR(14)   NOT NULL REFERENCES produto_vitrine (sku),
    qtd            INTEGER       NOT NULL,
    preco_unitario NUMERIC(10,2) NOT NULL,

    CONSTRAINT uk_item_pedido_sku   UNIQUE (pedido_id, sku),
    CONSTRAINT ck_item_pedido_qtd   CHECK (qtd > 0),
    CONSTRAINT ck_item_pedido_preco CHECK (preco_unitario > 0)
);
