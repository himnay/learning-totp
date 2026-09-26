-- RFC 6238 §5.2: once a code has been accepted, the same OTP must not be accepted again.
-- Remember the last accepted time step per app; validation only succeeds for a later step.
ALTER TABLE totp_seed ADD COLUMN last_used_step BIGINT;
