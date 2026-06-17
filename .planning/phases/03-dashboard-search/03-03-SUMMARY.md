---
phase: 3
plan: 03-03
phase_name: dashboard-search
plan_name: ngram-fulltext-search-backend
subsystem: search
tags:
  - backend
  - fulltext-search
  - ngram
  - flyway
  - security
  - tests
requires:
  - "Phase 0: Foundation (DataPermissionInterceptor, SecurityUtils, JwtUser)"
  - "Phase 1: Paper/Patent/Copyright registration (table schemas)"
provides:
  - SRCH-01: Basic search across title/abstract/authors/inventors
  - SRCH-02: Chinese ngram segmentation, fuzzy match, relevance sorting, keyword highlighting
affects:
  - achievement-framework (search package, mapper XML, pom.xml)
  - achievement-module/achievement-system (Flyway V16 migration)
  - achievement-web (api/search.ts)
tech-stack:
  added:
    - MySQL ngram FULLTEXT parser (WITH PARSER ngram) — built-in MySQL 8.4
  patterns:
    - UNION ALL + MATCH...AGAINST IN BOOLEAN MODE across 3 heterogeneous tables
    - Inline dept_id filter in each UNION branch (bypass DataPermissionInterceptor)
    - Static utility class for BOOLEAN MODE sanitization (SearchQuerySanitizer)
    - MyBatis OGNL static method call via `<bind>` for sanitizer integration
key-files:
  created:
    - achievement-module/achievement-system/src/main/resources/db/migration/V16__add_ngram_fulltext_indexes.sql
    - achievement-framework/src/main/java/com/institute/achievement/framework/search/util/SearchQuerySanitizer.java
    - achievement-framework/src/main/java/com/institute/achievement/framework/search/dto/SearchQueryDTO.java
    - achievement-framework/src/main/java/com/institute/achievement/framework/search/dto/SearchResultVO.java
    - achievement-framework/src/main/java/com/institute/achievement/framework/search/mapper/SearchMapper.java
    - achievement-framework/src/main/resources/mapper/SearchMapper.xml
    - achievement-framework/src/main/java/com/institute/achievement/framework/search/service/SearchService.java
    - achievement-framework/src/main/java/com/institute/achievement/framework/search/service/impl/SearchServiceImpl.java
    - achievement-framework/src/main/java/com/institute/achievement/framework/search/controller/SearchController.java
    - achievement-web/src/api/search.ts
    - achievement-framework/src/test/java/com/institute/achievement/framework/search/SearchQuerySanitizerTest.java
    - achievement-framework/src/test/java/com/institute/achievement/framework/search/SearchServiceTest.java
  modified:
    - achievement-framework/pom.xml (added spring-boot-starter-test)
decisions:
  - "SearchQuerySanitizer is a static utility class (private constructor), not a Spring @Service — avoids unnecessary DI and enables clean OGNL invocation from MyBatis XML"
  - "PageResult already exists in achievement-common.util — reused instead of creating a new DTO"
  - "spring-boot-starter-test added to framework pom.xml as test-scope dependency (was missing)"
metrics:
  duration: ~25 min
  completed_date: "2026-06-17"
---

# Phase [03] Plan [03-03]: ngram Full-Text Search Backend Summary

**One-liner:** Implemented MySQL ngram FULLTEXT search backend with UNION ALL across 3 achievement tables (paper/patent/copyright), SearchQuerySanitizer for BOOLEAN MODE safety, D-16 classified data exclusion via role check, and keyword highlighting for frontend rendering.

## Tasks Completed

| # | Type | Description | Commit | Key Files |
|---|------|-------------|--------|-----------|
| 1 | create | Flyway V16 ngram FULLTEXT indexes + SearchQuerySanitizer | `510d93b` | V16__add_ngram_fulltext_indexes.sql, SearchQuerySanitizer.java |
| 2 | create | Search DTOs + SearchMapper + SearchMapper.xml (UNION ALL) | `400e35d` | SearchQueryDTO.java, SearchResultVO.java, SearchMapper.java, SearchMapper.xml |
| 3 | create | SearchService/Controller + highlight computation + frontend API | `5d55814` | SearchService.java, SearchServiceImpl.java, SearchController.java, api/search.ts |
| 4 | test | SearchQuerySanitizerTest + SearchServiceTest (32 tests) | `14871a3` | SearchQuerySanitizerTest.java, SearchServiceTest.java, pom.xml |

## Architecture

### Component Diagram

```
Browser (api/search.ts)
    |
    | GET /api/search?keyword=...&type=...&page=...
    v
SearchController
    |
    v
SearchServiceImpl
    ├── SecurityUtils.hasRole("CLASSIFIED_MANAGER") → D-16 exclusion
    ├── SecurityUtils.getCurrentDeptId() → deptId resolution
    ├── SearchMapper.countSearch() → total count
    ├── SearchMapper.search() → UNION ALL FULLTEXT results
    └── computeHighlights() → regex match positions
        |
        v
SearchMapper.xml (UNION ALL + MATCH...AGAINST IN BOOLEAN MODE)
    ├── Paper branch: MATCH(title, authors, abstract_text)
    ├── Patent branch: MATCH(patent_name, inventors)
    └── Copyright branch: MATCH(name, copyright_holder)
```

### Data Flow

1. Frontend sends GET `/api/search?keyword=机器学习&page=1&size=10`
2. SearchController builds `SearchQueryDTO` from request params
3. SearchServiceImpl resolves `deptId` from SecurityContext (if null)
4. D-16 check: non-CLASSIFIED_MANAGER → `forceExcludeClassified=true`
5. MyBatis `<bind>` calls `SearchQuerySanitizer.toBooleanModeQuery(keyword)` → `+机器学习`
6. `countSearch()` counts UNION ALL results for pagination total
7. `search()` executes UNION ALL with MATCH...AGAINST and filters
8. Service computes `highlightRanges` via regex on title/authors
9. Returns `PageResult<SearchResultVO>` wrapped in `Result<>`

## Deviations from Plan

### Auto-fixed Issues (Rule 1 - Bug)

**1. `<bind>` element placement in SearchMapper.xml**
- **Found during:** Task 2
- **Issue:** `<bind>` was placed at `<mapper>` level, but MyBatis 3.5.x requires `<bind>` inside each `<select>/<insert>/<update>/<delete>` block.
- **Fix:** Moved `<bind>` into both `search` and `countSearch` select blocks.
- **Files modified:** `SearchMapper.xml`
- **Commit:** `400e35d`

**2. countSearch subquery missing columns for outer WHERE filter**
- **Found during:** Task 2
- **Issue:** The `countSearch` subquery only selected `p.id`, but the outer WHERE clause referenced `is_classified`, `achievementType`, and `publish_year` columns.
- **Fix:** Added all 3 columns to each UNION branch in `countSearch`.
- **Files modified:** `SearchMapper.xml`
- **Commit:** `400e35d`

### Auto-fixed Issues (Rule 3 - Blocking)

**3. Missing spring-boot-starter-test dependency in framework pom.xml**
- **Found during:** Task 4
- **Issue:** Tests failed to compile with "cannot find symbol: @Test, @Mock" because the framework module did not declare `spring-boot-starter-test` as a test dependency.
- **Fix:** Added `spring-boot-starter-test` (scope: test) to `achievement-framework/pom.xml`.
- **Files modified:** `achievement-framework/pom.xml`
- **Commit:** `14871a3`

### Test Fixes (Rule 1 - Bug)

**4. Wrong character offsets in multipleMatches highlight test**
- **Found during:** Task 4
- **Issue:** "机器学习与机器视觉" has "机器" at positions (0,2) and (5,7), but test asserted (0,2) and (4,6).
- **Fix:** Corrected character offsets.
- **Files modified:** `SearchServiceTest.java`
- **Commit:** `14871a3`

**5. SQL injection sanitizer test assertion mismatch**
- **Found during:** Task 4
- **Issue:** Test expected `;` and `TABLE` to be removed, but SearchQuerySanitizer only strips BOOLEAN MODE operators, not general SQL constructs.
- **Fix:** Updated test to assert correct behavior: BOOLEAN operators stripped, multi-char SQL keywords preserved.
- **Files modified:** `SearchQuerySanitizerTest.java`
- **Commit:** `14871a3`

## Verification Results

| Criterion | Status | Evidence |
|-----------|--------|----------|
| `mvn compile -pl achievement-framework` passes | PASSED | Exit code 0 |
| `mvn test -pl achievement-framework -Dtest="SearchQuerySanitizerTest,SearchServiceTest"` all green | PASSED | 32/32 tests pass |
| V16 migration has 3 ALTER TABLE ADD FULLTEXT INDEX statements | PASSED | paper/patent/copyright indexes |
| SearchQuerySanitizer.sanitize("C++") returns "C*" | PASSED | + stripped, "C" gets "*" suffix |
| SearchQuerySanitizer.sanitize("机器学习") returns "机器学习" | PASSED | Chinese preserved as-is |
| SearchQuerySanitizer.toBooleanModeQuery("机器学习") returns "+机器学习" | PASSED | AND prefix added |
| SearchMapper.xml has UNION ALL with inline dept_id in each branch | PASSED | 3 UNION ALL branches with explicit dept_id filter |
| D-16 classified exclusion: non-manager gets forceExcludeClassified=true | PASSED | Tested via SearchServiceTest |
| Classification filter uses `<choose>` mapping 'NORMAL'→0, 'CLASSIFIED'→1 | PASSED | In SearchMapper.xml |
| Frontend api/search.ts exports search() with correct types | PASSED | SearchQueryDTO, SearchResultVO, PageResult types |

## Success Criteria

- [x] V16 Flyway migration creates ngram FULLTEXT indexes on 3 tables (no keywords column — matches actual schema)
- [x] SearchQuerySanitizer correctly escapes BOOLEAN MODE operators, handles single-char wildcards
- [x] SearchMapper performs UNION ALL across 3 tables with MATCH...AGAINST IN BOOLEAN MODE
- [x] Inline dept_id filter in each UNION branch (bypasses DataPermissionInterceptor pitfall)
- [x] Classification filter uses `<choose>` to map string to int (fixes BLOCKER 2 type mismatch)
- [x] D-16 classified exclusion: non-CLASSIFIED_MANAGER users automatically get `forceExcludeClassified=true`
- [x] Search results sorted by relevance score DESC with pagination (LIMIT/OFFSET)
- [x] SearchService resolves deptId from SecurityContext if not provided
- [x] computeHighlights() returns match positions for frontend highlighting
- [x] Frontend api/search.ts exports search() with correct types
- [x] All 32 tests pass (sanitizer + service including D-16 tests)

## Threat Surface Scan

Added `SearchMapper.xml` as a new SQL surface with inline dept_id filtering. The threat model T-3-03-01 (BOOLEAN MODE tampering) is mitigated by SearchQuerySanitizer, T-3-03-02 (cross-dept leakage) by inline dept_id, T-3-03-03 (classified disclosure) by D-16 role check. No new network endpoints beyond the documented `/api/search` GET.

## Self-Check

Created files verified:
- `achievement-module/achievement-system/src/main/resources/db/migration/V16__add_ngram_fulltext_indexes.sql` — FOUND
- `achievement-framework/src/main/java/com/institute/achievement/framework/search/util/SearchQuerySanitizer.java` — FOUND
- `achievement-framework/src/main/java/com/institute/achievement/framework/search/dto/SearchQueryDTO.java` — FOUND
- `achievement-framework/src/main/java/com/institute/achievement/framework/search/dto/SearchResultVO.java` — FOUND
- `achievement-framework/src/main/java/com/institute/achievement/framework/search/mapper/SearchMapper.java` — FOUND
- `achievement-framework/src/main/resources/mapper/SearchMapper.xml` — FOUND
- `achievement-framework/src/main/java/com/institute/achievement/framework/search/service/SearchService.java` — FOUND
- `achievement-framework/src/main/java/com/institute/achievement/framework/search/service/impl/SearchServiceImpl.java` — FOUND
- `achievement-framework/src/main/java/com/institute/achievement/framework/search/controller/SearchController.java` — FOUND
- `achievement-web/src/api/search.ts` — FOUND
- `achievement-framework/src/test/java/com/institute/achievement/framework/search/SearchQuerySanitizerTest.java` — FOUND
- `achievement-framework/src/test/java/com/institute/achievement/framework/search/SearchServiceTest.java` — FOUND

Commits verified:
- `510d93b` — FOUND
- `400e35d` — FOUND
- `5d55814` — FOUND
- `14871a3` — FOUND

## Self-Check: PASSED
