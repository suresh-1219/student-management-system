package com.suresh.sms.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for every test that needs a real database.
 *
 * <p>Extending this class gives a test a real, throwaway MySQL database
 * running in Docker (via Testcontainers) instead of the developer's own
 * MySQL. No environment variables and no locally running MySQL are needed
 * to run the test suite - only Docker.
 *
 * <p>The container is started ONCE per test JVM (the "singleton container"
 * pattern) and shared by every test class that extends this one, rather
 * than being restarted for each class, which would make the suite very
 * slow. Testcontainers' own cleanup process removes it after the JVM exits;
 * there is nothing to shut down manually.
 *
 * <p>{@code @ServiceConnection} tells Spring Boot to point the datasource
 * (and Flyway) at this container automatically - the application's own
 * {@code spring.datasource.*} properties are not used by tests at all.
 */
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL_CONTAINER =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
                    .withDatabaseName("student_db_test")
                    .withUsername("test")
                    .withPassword("test");

    static {
        MYSQL_CONTAINER.start();
    }
}
