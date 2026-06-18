---
phase: 3
plan: 05
type: report
requirement: OPS-04
date: 2026-06-18
status: placeholder
---

# Phase 3 — Performance Test Report

> 50-user concurrent load test results for search (FULLTEXT UNION ALL) and
> dashboard (Redis-cached aggregation) endpoints.

---

## 1. Test Environment

| Parameter | Value |
|-----------|-------|
| **Date** | `[FILL AFTER RUN]` |
| **Docker** | MySQL 8.4 (port 3306), Redis 7.x (port 6379) |
| **Backend** | Spring Boot 4.1.x / JDK 21 |
| **Database** | `achievement_db` on MySQL 8.4 with ngram FULLTEXT indexes |
| **Cache** | Redis 7.x, 5-minute TTL for dashboard cache (D-04) |
| **Concurrent users** | 50 |
| **Search keywords** | 8 variations: `机器`, `深度`, `专利`, `算法`, `图像`, `数据`, `控制系统`, `通信` |
| **Test harness** | JUnit 5 `ExecutorService.newFixedThreadPool(50)` + `CountDownLatch` |
| **HTTP client** | Spring `RestTemplate` (5s connect / 10s read timeout) |
| **Data volume** | `[FILL AFTER RUN — approximate row counts per table]` |
| **MySQL config** | `innodb_buffer_pool_size=512M`, `innodb_ft_cache_size=8M` (default) |

---

## 2. Search Endpoint — 50 Concurrent Users

### 2.1 Response Time Distribution

| Metric | Value |
|--------|-------|
| **Min** | `[FILL AFTER RUN]` ms |
| **Average** | `[FILL AFTER RUN]` ms |
| **P50 (median)** | `[FILL AFTER RUN]` ms |
| **P95** | `[FILL AFTER RUN]` ms |
| **P99** | `[FILL AFTER RUN]` ms |
| **Max** | `[FILL AFTER RUN]` ms |
| **Total wall-clock** | `[FILL AFTER RUN]` ms |
| **Successful requests** | `[FILL AFTER RUN]` / 50 |
| **Failed requests** | `[FILL AFTER RUN]` |
| **All threads completed** | `[YES / NO]` |

### 2.2 Status

> **Phase 1 target:** P95 < 3000ms

- **PASS** (if P95 < 3000ms): `[CHECK IF MET]`
- **PASS with conditions** (if P95 3000-5000ms): `[CHECK IF MET]`
- **FAIL** (if P95 > 5000ms or errors > 5): `[CHECK IF MET]`

---

## 3. Dashboard Cache Performance

### 3.1 Annual Trend

| Call | Source | Response Time | HTTP Status |
|------|--------|--------------|-------------|
| First (uncached) | MySQL query | `[FILL AFTER RUN]` ms | 200 |
| Second (cached) | Redis cache | `[FILL AFTER RUN]` ms | 200 |
| **Improvement** | | `[FILL AFTER RUN]` ms (`[FILL]`%) | |

### 3.2 Type Distribution

| Call | Source | Response Time | HTTP Status |
|------|--------|--------------|-------------|
| First (uncached) | MySQL query | `[FILL AFTER RUN]` ms | 200 |
| Second (cached) | Redis cache | `[FILL AFTER RUN]` ms | 200 |
| **Improvement** | | `[FILL AFTER RUN]` ms (`[FILL]`%) | |

### 3.3 Department Ranking

| Call | Source | Response Time | HTTP Status |
|------|--------|--------------|-------------|
| First (uncached) | MySQL query | `[FILL AFTER RUN]` ms | 200 |
| Second (cached) | Redis cache | `[FILL AFTER RUN]` ms | 200 |
| **Improvement** | | `[FILL AFTER RUN]` ms (`[FILL]`%) | |

### 3.4 Patent Status

| Call | Source | Response Time | HTTP Status |
|------|--------|--------------|-------------|
| First (uncached) | MySQL query | `[FILL AFTER RUN]` ms | 200 |
| Second (cached) | Redis cache | `[FILL AFTER RUN]` ms | 200 |
| **Improvement** | | `[FILL AFTER RUN]` ms (`[FILL]`%) | |

### 3.5 Cache Status

> **D-04 check:** Is `@Cacheable(value = "dashboard", ...)` with 5-minute TTL working?

- `[PASS / FAIL]` — Cached response is `[faster / same / slower]` than uncached
- If FAIL: Check `spring.cache.type=redis` config, Redis connectivity, and `@EnableCaching` annotation

---

## 4. Error Analysis

| Error Type | Count | Details |
|------------|-------|---------|
| HTTP 4xx/5xx | `[FILL]` | `[FILL with error messages]` |
| Connection timeout | `[FILL]` | `[FILL]` |
| Read timeout | `[FILL]` | `[FILL]` |
| Other | `[FILL]` | `[FILL]` |

---

## 5. OPS-04 Verification Conclusion

### Requirement

> OPS-04: The system must support 50 concurrent users with acceptable response times.

### Verdict

> **Status:** `[PASS / PASS WITH CONDITIONS / FAIL]`

| Condition | Met? | Evidence |
|-----------|------|----------|
| All 50 concurrent requests complete | `[YES / NO]` | Response time distribution above |
| Search P95 response time < 3000ms | `[YES / NO]` | `[FILL]` ms measured |
| No more than 5% request failures | `[YES / NO]` | `[FILL]`% failure rate |
| Dashboard cached response faster than uncached | `[YES / NO]` | Cache comparison above |

### If FAIL: Roadmap for Phase 2/4

If the test does not meet the 50-user concurrency target, the following
infrastructure upgrades are recommended for implementation in later phases:

1. **Elasticsearch 8.x + IK** (Phase 2): Replace MySQL ngram FULLTEXT for
   search with dedicated search engine. Expected to improve search P99 by
   10-50x under concurrent load.
2. **Connection pooling tuning** (Phase 1.5): Increase HikariCP
   `maximum-pool-size` from 20 to 50 and adjust `minimum-idle` accordingly.
3. **MySQL FULLTEXT cache tuning**: Increase `innodb_ft_cache_size` from
   default 8MB to 32MB, `innodb_ft_total_cache_size` to 128MB.
4. **Composite BTREE indexes**: Add `(dept_id, status)` composite indexes on
   `paper`, `patent`, and `software_copyright` tables to speed up WHERE
   filtering before FULLTEXT MATCH.

---

## 6. Optimization Recommendations

### 6.1 MySQL FULLTEXT Cache (if search P95 > 3s)

```ini
# my.cnf — increase InnoDB FULLTEXT cache
innodb_ft_cache_size = 32M          # default: 8M
innodb_ft_total_cache_size = 128M   # default: 128M (may need increase)
```

### 6.2 Composite BTREE Indexes (if UNION ALL sorting is slow)

```sql
-- Add covering composite indexes for WHERE filtering
CREATE INDEX idx_paper_dept_status ON paper(dept_id, status);
CREATE INDEX idx_patent_dept_status ON patent(dept_id, status);
CREATE INDEX idx_software_dept_status ON software_copyright(dept_id, status);
```

### 6.3 InnoDB Buffer Pool (if overall system slow)

```ini
# Set to 70% of available RAM for dedicated DB server
# Current: 512MB (docker-compose.yml)
innodb_buffer_pool_size = 4G        # adjust based on server memory
```

### 6.4 HikariCP Connection Pool

```yaml
# application-dev.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50          # increase from 20 for 50 concurrent users
      minimum-idle: 10               # increase from 5
      connection-timeout: 30000
```

### 6.5 Redis Connection Pool

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 32             # increase from 16
          max-idle: 16               # increase from 8
```

### 6.6 PDF Export (if export included in load test)

Consider streaming PDF generation with iText `PdfFlusher` to reduce memory
pressure during concurrent export requests.

---

## 7. Setup Instructions for Re-running

### Prerequisites

```bash
# 1. Start infrastructure
docker-compose up -d

# 2. Wait for MySQL + Redis to be ready
docker-compose ps

# 3. Build backend
mvn compile -pl achievement-framework -am -q

# 4. Run performance test
mvn test -pl achievement-framework -Dtest="ConcurrentSearchTest"
```

### Expected Output

The test logs a formatted results table (via SLF4J) at INFO level:
- Search: Min, Avg, P50, P95, P99, Max response times in milliseconds
- Dashboard: Uncached vs cached comparison for all 4 endpoints
- Error details (if any)

### Test Class Location

- `achievement-framework/src/test/java/com/institute/achievement/framework/performance/ConcurrentSearchTest.java`
- `achievement-framework/src/test/java/com/institute/achievement/framework/performance/TestApplication.java`

---

## 8. Known Limitations (Phase 1)

1. **Single-node testing**: All 50 threads hit one JVM instance. In production
   with multiple instances, this test's throughput numbers do not directly translate.
2. **MySQL ngram**: For >50k records, MySQL ngram FULLTEXT degrades significantly.
   Phase 2 should migrate to Elasticsearch 8.x + IK.
3. **No JWT overhead**: Security filter chain overridden to permit-all for testing.
   Actual production requests have ~1-5ms additional JWT validation overhead.
4. **Single department filter**: All requests use `deptId=1`. Different departments
   would hit different index segments (minor impact).
5. **Cache state**: Dashboard cache test assumes Redis is empty on first call.
   If previous tests populated the cache, the "uncached" measurement may
   actually be cached.
