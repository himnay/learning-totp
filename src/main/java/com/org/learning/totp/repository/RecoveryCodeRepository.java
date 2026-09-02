package com.org.learning.totp.repository;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Plain {@link JdbcTemplate} persistence for {@code totp_recovery_code}. Codes are stored as-is
 * (not hashed) — see the README for what a production deployment would add here.
 */
@Repository
public class RecoveryCodeRepository {

    private final JdbcTemplate jdbc;

    public RecoveryCodeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Deletes any codes already issued for this device, then inserts a fresh batch. */
    public void replaceAll(String deviceId, List<String> codes) {
        jdbc.update("DELETE FROM totp_recovery_code WHERE device_id = ?", deviceId);

        List<Object[]> batchArgs = codes.stream().map(code -> new Object[] {deviceId, code}).toList();
        jdbc.batchUpdate("INSERT INTO totp_recovery_code (device_id, code) VALUES (?, ?)", batchArgs);
    }

    /**
     * Atomically redeems a code: flips {@code used} only if a matching, still-unused row exists.
     * Returns {@code true} exactly once per valid code — a second attempt with the same code (or
     * one that never existed) affects zero rows and returns {@code false}. Deliberately doesn't
     * distinguish "wrong code" from "already used" from "unknown device" in its return value —
     * leaking that distinction would help an attacker enumerate valid codes.
     */
    public boolean redeem(String deviceId, String code) {
        int updated =
                jdbc.update(
                        """
                        UPDATE totp_recovery_code SET used = TRUE, used_at = NOW()
                        WHERE device_id = ? AND code = ? AND used = FALSE
                        """,
                        deviceId,
                        code);
        return updated == 1;
    }
}
