package com.org.learning.totp.domain;

import java.time.Instant;

/** A persisted row from {@code totp_seed} — one secret per device. */
public record TotpSeed(Long id, String deviceId, String issuer, String secret, Instant createdAt) {}
