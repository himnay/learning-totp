CREATE TABLE IF NOT EXISTS totp_seed (
    id         BIGSERIAL    PRIMARY KEY,
    device_id  VARCHAR(100) NOT NULL UNIQUE,
    issuer     VARCHAR(100) NOT NULL,
    secret     VARCHAR(64)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
