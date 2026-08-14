CREATE TABLE transfers (
    id            UUID             NOT NULL PRIMARY KEY,
    sender_id     UUID             NOT NULL,
    receiver_id   UUID             NOT NULL,
    amount_brl    NUMERIC(19, 4)   NOT NULL,
    amount_usd    NUMERIC(19, 4)   NOT NULL,
    exchange_rate NUMERIC(19, 6)   NOT NULL,
    executed_at   TIMESTAMP        NOT NULL,
    CONSTRAINT fk_transfers_sender   FOREIGN KEY (sender_id)   REFERENCES users (id),
    CONSTRAINT fk_transfers_receiver FOREIGN KEY (receiver_id) REFERENCES users (id)
);

CREATE INDEX idx_transfers_sender_executed_at ON transfers (sender_id, executed_at);
