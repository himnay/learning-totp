package com.org.learning.totp.domain;

import java.time.Instant;

/** A persisted row from {@code totp_seed} — one secret per registered MFA app install (appId). */
public record TotpSeed(Long id, String appId, String issuer, String secret, Instant createdAt) {}
