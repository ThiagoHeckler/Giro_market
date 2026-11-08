-- Dead-letter: evento que o destino recusa de vez (4xx definitivo) fica FAILED, fora do polling.
ALTER TABLE outbox_event DROP CONSTRAINT ck_outbox_event_status;
ALTER TABLE outbox_event
    ADD CONSTRAINT ck_outbox_event_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'));
