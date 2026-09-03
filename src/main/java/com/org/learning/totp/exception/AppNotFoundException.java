package com.org.learning.totp.exception;

/** No {@code totp_seed} row exists for the given app — {@code /register} was never called for it. */
public class AppNotFoundException extends RuntimeException {

    public AppNotFoundException(String appId) {
        super("No TOTP seed found for appId: " + appId);
    }
}
