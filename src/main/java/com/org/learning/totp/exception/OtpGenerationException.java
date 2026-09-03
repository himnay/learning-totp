package com.org.learning.totp.exception;

/** Wraps a checked {@code CodeGenerationException} from the TOTP library's {@code CodeGenerator}. */
public class OtpGenerationException extends RuntimeException {

    public OtpGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
