package com.org.learning.totp.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(
        classes = {
            DataSourceAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            TotpSeedRepository.class
        })
class TotpSeedRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private TotpSeedRepository repository;

    @Test
    @DisplayName("upsert() then findByAppId() round-trips a seed")
    void upsertThenFind() {
        repository.upsert("app-a", "learning-totp", "JBSWY3DPEHPK3PXP");

        var seed = repository.findByAppId("app-a");

        assertThat(seed).isNotNull();
        assertThat(seed.appId()).isEqualTo("app-a");
        assertThat(seed.issuer()).isEqualTo("learning-totp");
        assertThat(seed.secret()).isEqualTo("JBSWY3DPEHPK3PXP");
        assertThat(seed.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("upsert() called again for the same app rotates the secret instead of duplicating the row")
    void upsertRotatesSecretOnConflict() {
        repository.upsert("app-b", "learning-totp", "OLDSECRET00000000");
        repository.upsert("app-b", "learning-totp", "NEWSECRET00000000");

        var seed = repository.findByAppId("app-b");

        assertThat(seed.secret()).isEqualTo("NEWSECRET00000000");
    }

    @Test
    @DisplayName("findByAppId() returns null (not an exception) when nothing is saved")
    void findReturnsNullWhenMissing() {
        assertThat(repository.findByAppId("nobody")).isNull();
    }
}
