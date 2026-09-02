package com.org.learning.totp.repository;

import com.org.learning.totp.domain.TotpSeed;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Plain {@link JdbcTemplate} persistence for {@code totp_seed}, keyed by {@code device_id}. The
 * secret is stored as-is (not encrypted at rest) — see the README for what a production
 * deployment would add here.
 */
@Repository
public class TotpSeedRepository {

    private final JdbcTemplate jdbc;

    public TotpSeedRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Inserts a new seed, or replaces the existing one for this device (re-enrollment rotates the secret). */
    public void upsert(String deviceId, String issuer, String secret) {
        jdbc.update(
                """
                INSERT INTO totp_seed (device_id, issuer, secret) VALUES (?, ?, ?)
                ON CONFLICT (device_id) DO UPDATE SET issuer = EXCLUDED.issuer, secret = EXCLUDED.secret, created_at = NOW()
                """,
                deviceId,
                issuer,
                secret);
    }

    /** Returns {@code null} when no seed exists for this device (not an exception). */
    public TotpSeed findByDeviceId(String deviceId) {
        List<TotpSeed> matches =
                jdbc.query(
                        "SELECT id, device_id, issuer, secret, created_at FROM totp_seed WHERE device_id = ?",
                        (rs, rowNum) ->
                                new TotpSeed(
                                        rs.getLong("id"),
                                        rs.getString("device_id"),
                                        rs.getString("issuer"),
                                        rs.getString("secret"),
                                        toInstant(rs.getTimestamp("created_at"))),
                        deviceId);
        return matches.isEmpty() ? null : matches.get(0);
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }
}
