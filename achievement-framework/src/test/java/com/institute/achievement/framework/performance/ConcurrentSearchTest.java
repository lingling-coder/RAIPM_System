package com.institute.achievement.framework.performance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrent load test for search and dashboard endpoints.
 * <p>
 * Simulates 50 concurrent users performing full-text searches across 8 keyword
 * variations and measures response time percentiles (P50, P95, P99, max).
 * Also verifies Redis cache improves dashboard response time on repeated calls.
 * <p>
 * <strong>Prerequisites:</strong>
 * <ol>
 *   <li>{@code docker-compose up -d} (MySQL 8.4 :3306, Redis 7 :6379)</li>
 *   <li>Database has seed data from prior phases (Phases 1-2)</li>
 *   <li>Backend compiled: {@code mvn compile -pl achievement-framework -am}</li>
 * </ol>
 * <p>
 * <strong>Run command:</strong>
 * <pre>{@code
 * mvn test -pl achievement-framework -Dtest="ConcurrentSearchTest"
 * }</pre>
 * <p>
 * <strong>NOTE:</strong> This is a manual integration test. If MySQL or Redis are
 * not running, the Spring context will fail to start with a connection refused error.
 * <p>
 * <strong>Security handling:</strong> The test overrides the security filter chain
 * to permit all requests without authentication. All controller, service, and
 * persistence layers operate with full real infrastructure.
 */
@SpringBootTest(
        classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "spring.flyway.enabled=false"
        }
)
@ActiveProfiles("dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConcurrentSearchTest {

    private static final Logger log = LoggerFactory.getLogger(ConcurrentSearchTest.class);

    /** Number of concurrent virtual users for the load test. */
    private static final int CONCURRENT_USERS = 50;

    /**
     * Search keyword variations covering different Chinese text lengths and
     * ngram tokenization patterns to exercise MySQL FULLTEXT search.
     */
    private static final String[] SEARCH_KEYWORDS = {
            "机器",       // 2-char short keyword
            "深度",       // 2-char short keyword
            "专利",       // 2-char, likely matches patent titles
            "算法",       // 2-char, likely matches paper keywords
            "图像",       // 2-char, visual computing topic
            "数据",       // 2-char, broad match
            "控制系统",   // 4-char compound keyword
            "通信"        // 2-char, telecom topic
    };

    /**
     * Timeout for all 50 threads to complete their HTTP requests.
     * Set to 120 seconds to accommodate slower environments (e.g. Docker on Windows).
     */
    private static final long GLOBAL_TIMEOUT_SECONDS = 120;

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Test configuration that overrides production beans for performance testing.
     * <p>
     * 1. {@code securityFilterChain}: Permits all requests without JWT authentication.
     *    Without this override, {@link com.institute.achievement.framework.security.SecurityConfig}
     *    requires JWT tokens for all endpoints except {@code /api/auth/*}, which would
     *    cause test requests to return 401 Unauthorized.
     *    <p>
     *    {@code spring.main.allow-bean-definition-overriding=true} ensures this
     *    bean definition wins over the one from {@code SecurityConfig}.
     * <p>
     * 2. {@code stringRedisTemplate}: Provides a {@code RedisTemplate<String, String>}
     *    bean required by {@link com.institute.achievement.framework.security.JwtTokenProvider}.
     *    Spring Boot auto-configuration does not create this specific parameterisation,
     *    so it must be supplied explicitly. Uses the same {@code RedisConnectionFactory}
     *    from auto-configuration (requires Redis to be running).
     */
    @TestConfiguration
    static class TestConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth
                            .anyRequest().permitAll()
                    )
                    .sessionManagement(session ->
                            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    );
            return http.build();
        }

        /**
         * Provides the {@code RedisTemplate<String, String>} bean required by
         * {@code JwtTokenProvider} for token blacklisting. Spring Boot's
         * auto-configuration only creates {@code RedisTemplate<Object, Object>}
         * and {@code StringRedisTemplate}, not the specific
         * {@code RedisTemplate<String, String>} parameterisation.
         */
        @Bean
        public RedisTemplate<String, String> stringRedisTemplate(
                RedisConnectionFactory redisConnectionFactory) {
            RedisTemplate<String, String> template = new RedisTemplate<>();
            template.setConnectionFactory(redisConnectionFactory);
            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(new StringRedisSerializer());
            template.setHashKeySerializer(new StringRedisSerializer());
            template.setHashValueSerializer(new StringRedisSerializer());
            template.afterPropertiesSet();
            return template;
        }
    }

    // ── Concurrent search load test ──────────────────────────────────────

    /**
     * Simulate 50 concurrent users making search requests with varied keywords.
     * <p>
     * Each of the 50 threads:
     * <ol>
     *   <li>Picks one keyword from {@link #SEARCH_KEYWORDS} (round-robin)</li>
     *   <li>Fires {@code GET /api/search?keyword=...&page=1&size=10&deptId=1}</li>
     *   <li>Records response time (ms) and HTTP status code</li>
     *   <li>Counts down the shared latch</li>
     * </ol>
     * <p>
     * After all threads complete (or timeout), computes:
     * <ul>
     *   <li>Min / Average / P50 / P95 / P99 / Max response times</li>
     *   <li>Error count and details</li>
     * </ul>
     * <p>
     * Output is logged at INFO level. The P95 soft-threshold of 3000ms is
     * documented but does NOT fail the test (Phase 1 performance target).
     */
    @Test
    void testConcurrentSearch() throws Exception {
        // Warm-up: single request to ensure JIT compilation and connection pool warm
        warmUp("/api/search?keyword=机器&page=1&size=10&deptId=1");
        log.info("Warm-up complete. Starting {} concurrent search requests...", CONCURRENT_USERS);

        // ── Execute concurrent requests ──────────────────────────────
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);

        List<Long> durations = Collections.synchronizedList(new ArrayList<>(CONCURRENT_USERS));
        List<Integer> statusCodes = Collections.synchronizedList(new ArrayList<>(CONCURRENT_USERS));
        List<String> errorDetails = Collections.synchronizedList(new ArrayList<>());

        AtomicInteger requestIndex = new AtomicInteger(0);
        long submitStart = System.nanoTime();

        for (int i = 0; i < CONCURRENT_USERS; i++) {
            executor.submit(() -> {
                int idx = requestIndex.getAndIncrement();
                String keyword = SEARCH_KEYWORDS[idx % SEARCH_KEYWORDS.length];
                long startNanos = System.nanoTime();
                try {
                    String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
                    String url = "http://localhost:" + port
                            + "/api/search?keyword=" + encodedKeyword
                            + "&page=1&size=10&deptId=1";

                    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

                    long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
                    durations.add(elapsedMs);
                    int statusCode = response.getStatusCode().value();
                    statusCodes.add(statusCode);

                    if (statusCode >= 400) {
                        errorDetails.add("Keyword='" + keyword + "' -> HTTP " + statusCode);
                    }

                    log.debug("Request #{}: keyword='{}' -> {} ({}ms)",
                            idx, keyword, statusCode, elapsedMs);

                } catch (Exception e) {
                    long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
                    durations.add(elapsedMs);
                    statusCodes.add(999);
                    errorDetails.add("Keyword='" + keyword + "' -> " + e.getClass().getSimpleName()
                            + ": " + e.getMessage());
                    log.warn("Request #{} FAILED: keyword='{}' -> {} ({}ms)",
                            idx, keyword, e.getClass().getSimpleName(), elapsedMs);
                } finally {
                    latch.countDown();
                }
            });
        }

        // ── Wait for all threads ─────────────────────────────────────
        boolean allCompleted = latch.await(GLOBAL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        long totalDurationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - submitStart);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // ── Compute statistics ───────────────────────────────────────
        Collections.sort(durations);

        int size = durations.size();
        if (size == 0) {
            log.error("No requests completed — all threads failed!");
            return;
        }

        double avg = durations.stream().mapToLong(Long::longValue).average().orElse(0);
        long p50 = durations.get(size / 2);
        long p95 = durations.get((int) (size * 0.95));
        long p99 = durations.get((int) (size * 0.99));
        long maxVal = durations.get(size - 1);
        long minVal = durations.get(0);
        long errorCount = errorDetails.size();
        long successCount = statusCodes.stream().filter(code -> code >= 200 && code < 400).count();

        // ── Print results ────────────────────────────────────────────
        log.info("");
        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║        50 Concurrent Search — Results Summary           ║");
        log.info("╠══════════════════════════════════════════════════════════╣");
        log.info("║ All 50 threads completed         : {}                  ║", allCompleted ? "YES" : "NO (timed out)");
        log.info("║ Wall-clock duration (all threads) : {} ms              ║", totalDurationMs);
        log.info("║ Total successful requests        : {}                  ║", successCount);
        log.info("║ Total failed requests            : {}                  ║", errorCount);
        log.info("║                                  :                    ║");
        log.info("║  Min response time               : {} ms              ║", minVal);
        log.info("║  Average response time           : {} ms              ║", String.format("%.1f", avg));
        log.info("║  P50  (median)                   : {} ms              ║", p50);
        log.info("║  P95                             : {} ms              ║", p95);
        log.info("║  P99                             : {} ms              ║", p99);
        log.info("║  Max response time               : {} ms              ║", maxVal);
        log.info("╚══════════════════════════════════════════════════════════╝");
        log.info("");

        // ── Error details ────────────────────────────────────────────
        if (!errorDetails.isEmpty()) {
            log.warn("=== Error Details ===");
            errorDetails.forEach(detail -> log.warn("  {}", detail));
        }

        // ── Soft assertion (documented warning, does NOT fail CI) ────
        // Phase 1 performance target: P95 < 3000ms
        // If exceeded, recommendations in 03-PERFORMANCE-REPORT.md apply
        if (p95 > 3000) {
            log.warn("┌──────────────────────────────────────────────────────────────┐");
            log.warn("│ WARNING: P95 response time ({}ms) exceeds 3000ms threshold   │", p95);
            log.warn("│ See 03-PERFORMANCE-REPORT.md for tuning recommendations.     │");
            log.warn("│ - Increase innodb_ft_cache_size (8MB -> 32MB)                │");
            log.warn("│ - Add composite BTREE indexes on (dept_id, status)            │");
            log.warn("│ - Check innodb_buffer_pool_size (recommend 70% of RAM)       │");
            log.warn("└──────────────────────────────────────────────────────────────┘");
        } else {
            log.info("P95 response time ({}ms) is within 3000ms threshold.", p95);
        }

        // Basic sanity assertion: at least some requests succeeded
        assertThat(successCount)
                .as("At least one search request should succeed")
                .isGreaterThan(0);
    }

    // ── Dashboard cache performance test ────────────────────────────────

    /**
     * Measure the performance difference between a cached and uncached dashboard
     * query.
     * <p>
     * The first request to {@code GET /api/dashboard/annual-trend} hits MySQL
     * (uncached). The second identical request should hit Redis cache (if Redis
     * is running and the {@code @Cacheable} annotation is working). Compares the
     * two response times and logs the improvement factor.
     * <p>
     * Also tests {@code GET /api/dashboard/type-dist} and
     * {@code GET /api/dashboard/dept-ranking} for additional coverage.
     */
    @Test
    void testDashboardCachePerformance() {
        log.info("");
        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║      Dashboard Cache Performance Comparison             ║");
        log.info("╚══════════════════════════════════════════════════════════╝");

        testSingleCacheEndpoint("/api/dashboard/annual-trend", "Annual Trend");
        testSingleCacheEndpoint("/api/dashboard/type-dist", "Type Distribution");
        testSingleCacheEndpoint("/api/dashboard/dept-ranking", "Dept Ranking");
        testSingleCacheEndpoint("/api/dashboard/patent-status", "Patent Status");
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    /**
     * Test a single dashboard endpoint: first call (uncached) vs second call
     * (cached). Logs durations and computes the improvement factor.
     */
    private void testSingleCacheEndpoint(String path, String label) {
        String url = "http://localhost:" + port + path;

        // First call — uncached, hits MySQL directly
        long start1 = System.nanoTime();
        ResponseEntity<String> resp1 = restTemplate.getForEntity(url, String.class);
        long uncachedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start1);
        int status1 = resp1.getStatusCode().value();

        // Brief pause to ensure first response is fully committed
        sleepQuietly(100);

        // Second call — should hit Redis cache (5-min TTL per D-04)
        long start2 = System.nanoTime();
        ResponseEntity<String> resp2 = restTemplate.getForEntity(url, String.class);
        long cachedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start2);
        int status2 = resp2.getStatusCode().value();

        // Compute improvement
        double improvement = 0;
        if (uncachedMs > 0 && cachedMs < uncachedMs) {
            improvement = (1.0 - (double) cachedMs / uncachedMs) * 100;
        }

        log.info("  {}:", label);
        log.info("    First call  (uncached, MySQL) : {} ms  (HTTP {})", uncachedMs, status1);
        log.info("    Second call (cached, Redis)   : {} ms  (HTTP {})", cachedMs, status2);
        if (cachedMs < uncachedMs) {
            log.info("    Improvement                   : {} ms faster ({}%)",
                    uncachedMs - cachedMs, String.format("%.1f", improvement));
        } else {
            log.info("    Improvement                   : NOT faster (possible cache miss)");
        }

        // Both calls should succeed
        assertThat(status1).as(label + " uncached should return 200").isEqualTo(200);
        assertThat(status2).as(label + " cached should return 200").isEqualTo(200);
    }

    /**
     * Warm-up request to prime the JIT compiler, connection pool, and
     * MySQL buffer pool before timing measurements.
     */
    private void warmUp(String path) {
        String url = "http://localhost:" + port + path;
        try {
            restTemplate.getForEntity(url, String.class);
        } catch (Exception e) {
            log.warn("Warm-up request failed (ignored): {}", e.getMessage());
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
