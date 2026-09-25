-- Reserva feita no checkout (não no pagamento) para evitar overselling do último item.
CREATE TABLE reserva (
    id        UUID        PRIMARY KEY,
    sku       VARCHAR(14) NOT NULL REFERENCES produto_vitrine (sku),
    qtd       INTEGER     NOT NULL,
    pedido_id UUID        NOT NULL REFERENCES pedido (id),
    expira_em TIMESTAMPTZ NOT NULL,
    status    VARCHAR(10) NOT NULL,

    CONSTRAINT ck_reserva_qtd    CHECK (qtd > 0),
    CONSTRAINT ck_reserva_status CHECK (status IN ('ATIVA', 'CONFIRMADA', 'EXPIRADA'))
);

-- Varredura de expiração: reservas ativas vencidas.
CREATE INDEX ix_reserva_ativa_expira ON reserva (expira_em) WHERE status = 'ATIVA';
CREATE INDEX ix_reserva_pedido ON reserva (pedido_id);
