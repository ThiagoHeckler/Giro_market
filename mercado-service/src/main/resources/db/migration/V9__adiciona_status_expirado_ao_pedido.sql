-- Pedido cuja reserva venceu sem pagamento: as unidades voltam para a prateleira.
ALTER TABLE pedido
    DROP CONSTRAINT ck_pedido_status,
    ADD CONSTRAINT ck_pedido_status CHECK (status IN ('AGUARDANDO_PAGAMENTO', 'PAGO', 'CANCELADO', 'EXPIRADO'));
