package com.org.learning.totp.exception;

/** No {@code totp_seed} row exists for the given device — {@code /generate} was never called for it. */
public class DeviceNotFoundException extends RuntimeException {

    public DeviceNotFoundException(String deviceId) {
        super("No TOTP seed found for deviceId: " + deviceId);
    }
}
