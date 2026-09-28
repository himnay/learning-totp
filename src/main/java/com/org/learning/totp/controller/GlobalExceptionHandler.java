package com.org.learning.totp.controller;

import com.org.learning.totp.dto.ApiError;
import com.org.learning.totp.exception.AppNotFoundException;
import com.org.learning.totp.exception.OtpGenerationException;
import com.org.learning.totp.exception.QrCodeRenderException;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps every exception that escapes {@link TotpController} to a consistent {@link ApiError} JSON body.
 * Extends {@link ResponseEntityExceptionHandler} so Spring MVC's own exceptions (malformed JSON, an
 * unsupported method or media type, an unknown path) keep their 4xx status instead of falling
 * through to the catch-all 500.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message =
                ex.getBindingResult().getFieldErrors().stream()
                        .findFirst()
                        .map(f -> f.getField() + ": " + f.getDefaultMessage())
                        .orElse("Validation failed");
        return ResponseEntity.badRequest().headers(headers).body(error(HttpStatus.BAD_REQUEST, message));
    }

    /** Every other Spring MVC exception, with the status and detail Spring assigns to it. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        String message = body instanceof ProblemDetail problem && problem.getDetail() != null
                ? problem.getDetail()
                : ex.getMessage();
        return ResponseEntity.status(status).headers(headers).body(error(status, message));
    }

    @ExceptionHandler(AppNotFoundException.class)
    public ResponseEntity<ApiError> handleAppNotFound(AppNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(QrCodeRenderException.class)
    public ResponseEntity<ApiError> handleQrCodeRender(QrCodeRenderException ex) {
        log.error("LEARNING_TOTP | QR generation failed | {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage()));
    }

    @ExceptionHandler(OtpGenerationException.class)
    public ResponseEntity<ApiError> handleOtpGeneration(OtpGenerationException ex) {
        log.error("LEARNING_TOTP | OTP generation failed | {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("LEARNING_TOTP | unhandled error | {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage()));
    }

    private static ApiError error(HttpStatus status, String message) {
        return new ApiError(OffsetDateTime.now(), status.value(), status.getReasonPhrase(), message);
    }
}
