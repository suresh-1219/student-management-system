package com.suresh.sms.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Proves that Flyway ran on startup and that no migration is left pending.
 * (If V2 failed, for example because of duplicate student e-mails, the
 * application context would not even start and this test would error.)
 */
@SpringBootTest
class FlywayMigrationTest {

    @Autowired
    private Flyway flyway;

    @Test
    void allMigrationsAreApplied() {

        MigrationInfoService info = flyway.info();

        assertEquals(0, info.pending().length, "No migration should be pending");

        assertNotNull(info.current(), "Flyway should have a current version");

        assertTrue(
                info.current().getVersion()
                        .compareTo(MigrationVersion.fromVersion("2")) >= 0,
                "Schema should be at version 2 or newer");
    }
}
