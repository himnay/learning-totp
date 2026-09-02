package com.org.learning.totp.exception;

/** Wraps a checked {@code dev.samstevens.totp.exceptions.QrGenerationException} as unchecked. */
public class QrCodeRenderException extends RuntimeException {

    public QrCodeRenderException(String message, Throwable cause) {
        super(message, cause);
    }
}
