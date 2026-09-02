package com.org.learning.totp.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Response body for {@code POST /api/v1/totp/validate}. */
public record ValidateTotpResponse(@Schema(example = "true") boolean valid) {}
