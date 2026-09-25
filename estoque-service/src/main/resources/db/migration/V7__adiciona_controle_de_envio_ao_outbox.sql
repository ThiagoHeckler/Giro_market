-- Controle de reenvio do publicador: falha de entrega não perde o evento, só adia com backoff.
ALTER TABLE outbox_event
    ADD COLUMN tentativas           INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN proxima_tentativa_em TIMESTAMPTZ,
    ADD COLUMN ultimo_erro          VARCHAR(500),
    ADD COLUMN enviado_em           TIMESTAMPTZ;

UPDATE outbox_event SET proxima_tentativa_em = criado_em;

ALTER TABLE outbox_event ALTER COLUMN proxima_tentativa_em SET NOT NULL;

-- Polling: SELECT ... WHERE status = 'PENDING' AND proxima_tentativa_em <= now() FOR UPDATE SKIP LOCKED.
DROP INDEX ix_outbox_event_pendente;
CREATE INDEX ix_outbox_event_pendente ON outbox_event (proxima_tentativa_em) WHERE status = 'PENDING';
