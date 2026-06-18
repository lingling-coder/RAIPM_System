package com.institute.achievement.framework.performance;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal Spring Boot application class for integration/performance tests.
 * <p>
 * Scans all {@code com.institute.achievement} packages so that controllers,
 * services, mappers, and infrastructure beans (MyBatis-Plus, Redis, Security,
 * etc.) are picked up by the test context.
 * <p>
 * Used by {@link ConcurrentSearchTest} which boots a full application context
 * with a random HTTP port. Requires Docker Compose (MySQL 8.4 + Redis 7) to
 * be running for the application context to start successfully.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * mvn test -pl achievement-framework -Dtest="ConcurrentSearchTest"
 * }</pre>
 * <p>
 * Prerequisites:
 * <ol>
 *   <li>{@code docker-compose up -d} (MySQL + Redis)</li>
 *   <li>Backend built: {@code mvn compile -pl achievement-framework -am}</li>
 * </ol>
 */
@SpringBootApplication(scanBasePackages = "com.institute.achievement")
public class TestApplication {

    /**
     * Main method — not typically invoked directly; Spring Boot test infrastructure
     * uses this class as the primary {@code @SpringBootConfiguration} source.
     */
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(TestApplication.class, args);
    }
}
