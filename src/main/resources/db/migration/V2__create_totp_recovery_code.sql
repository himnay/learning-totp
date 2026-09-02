CREATE TABLE IF NOT EXISTS totp_recovery_code (
    id         BIGSERIAL    PRIMARY KEY,
    device_id  VARCHAR(100) NOT NULL REFERENCES totp_seed (device_id) ON DELETE CASCADE,
    code       VARCHAR(32)  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (device_id, code)
);

CREATE INDEX IF NOT EXISTS idx_totp_recovery_code_device_id ON totp_recovery_code (device_id);
