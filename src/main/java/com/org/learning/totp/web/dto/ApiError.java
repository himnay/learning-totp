package com.org.learning.totp.web.dto;

import java.time.OffsetDateTime;

/** Uniform error body returned by {@link com.org.learning.totp.web.GlobalExceptionHandler}. */
public record ApiError(OffsetDateTime timestamp, int status, String error, String message) {}
