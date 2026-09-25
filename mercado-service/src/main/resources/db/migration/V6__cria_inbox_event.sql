-- Inbox: garante consumo idempotente. Evento já presente aqui é descartado.
CREATE TABLE inbox_event (
    event_id      UUID        PRIMARY KEY,
    processado_em TIMESTAMPTZ NOT NULL
);
