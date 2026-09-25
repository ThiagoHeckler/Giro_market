-- Transactional Outbox: gravado na mesma transação que muda o estado de negócio.
-- O id é o próprio eventId do contrato publicado.
CREATE TABLE outbox_event (
    id         UUID         PRIMARY KEY,
    tipo       VARCHAR(100) NOT NULL,
    payload    JSONB        NOT NULL,
    status     VARCHAR(10)  NOT NULL DEFAULT 'PENDING',
    criado_em  TIMESTAMPTZ  NOT NULL,

    CONSTRAINT ck_outbox_event_status CHECK (status IN ('PENDING', 'SENT'))
);

-- Polling do publicador: SELECT ... WHERE status = 'PENDING' ORDER BY criado_em FOR UPDATE SKIP LOCKED.
CREATE INDEX ix_outbox_event_pendente ON outbox_event (criado_em) WHERE status = 'PENDING';
