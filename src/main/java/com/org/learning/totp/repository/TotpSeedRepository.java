package com.org.learning.totp.repository;

import com.org.learning.totp.domain.TotpSeed;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Plain {@link JdbcTemplate} persistence for {@code totp_seed}, keyed by {@code app_id} (an MFA
 * app install — a device can host several). The secret is stored as-is (not encrypted at rest) —
 * see the README for what a production deployment would add here.
 */
@Repository
public class TotpSeedRepository {

    private final JdbcTemplate jdbc;

    public TotpSeedRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Inserts a new seed, or replaces the existing one for this app (re-registering rotates the secret). */
    public void upsert(String appId, String issuer, String secret) {
        jdbc.update(
                """
                INSERT INTO totp_seed (app_id, issuer, secret) VALUES (?, ?, ?)
                ON CONFLICT (app_id) DO UPDATE SET issuer = EXCLUDED.issuer, secret = EXCLUDED.secret, created_at = NOW()
                """,
                appId,
                issuer,
                secret);
    }

    /** Returns {@code null} when no seed exists for this app (not an exception). */
    public TotpSeed findByAppId(String appId) {
        List<TotpSeed> matches =
                jdbc.query(
                        "SELECT id, app_id, issuer, secret, created_at FROM totp_seed WHERE app_id = ?",
                        (rs, rowNum) ->
                                new TotpSeed(
                                        rs.getLong("id"),
                                        rs.getString("app_id"),
                                        rs.getString("issuer"),
                                        rs.getString("secret"),
                                        toInstant(rs.getTimestamp("created_at"))),
                        appId);
        return matches.isEmpty() ? null : matches.get(0);
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }
}
