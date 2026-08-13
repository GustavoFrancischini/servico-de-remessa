CREATE TABLE users (
    id              UUID            NOT NULL PRIMARY KEY,
    full_name       VARCHAR(160)    NOT NULL,
    email           VARCHAR(160)    NOT NULL,
    password_hash   VARCHAR(100)    NOT NULL,
    document_type   VARCHAR(4)      NOT NULL CHECK (document_type IN ('CPF', 'CNPJ')),
    document_value  VARCHAR(14)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_document_value UNIQUE (document_value)
);

CREATE TABLE wallets (
    id              UUID            NOT NULL PRIMARY KEY,
    user_id         UUID            NOT NULL,
    balance_brl     NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    balance_usd     NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL,
    CONSTRAINT uk_wallets_user_id UNIQUE (user_id),
    CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users (id)
);
