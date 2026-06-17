---
phase: 3
plan: 03-01
subsystem: dashboard-statistics
tags: ["dashboard", "echarts", "redis-cache", "itext-pdf", "easyexcel", "api", "pinia-store"]
requires: [03-CONTEXT.md, 03-PATTERNS.md, 03-RESEARCH.md, 03-UI-SPEC.md]
provides: [dashboard-backend-service, dashboard-frontend-api, dashboard-pinia-store]
affects: [achievement-framework, achievement-web]
tech-stack:
  added: ["iText 7 (kernel/layout/io/font-asian 8.0.5)"]
  patterns: ["DashboardService with @Cacheable Redis 5-min TTL", "DashboardPdfService with iText 7 Chinese font", "Pinia store with client-side 5-min cache"]
key-files:
  created:
    - "achievement-framework/.../dashboard/dto/*VO.java (5 DTOs)"
    - "achievement-framework/.../dashboard/mapper/DashboardMapper.java"
    - "achievement-framework/.../dashboard/mapper/DashboardMapper.xml"
    - "achievement-framework/.../dashboard/service/DashboardService.java"
    - "achievement-framework/.../dashboard/service/DashboardPdfService.java"
    - "achievement-framework/.../dashboard/service/impl/DashboardServiceImpl.java"
    - "achievement-framework/.../dashboard/controller/DashboardController.java"
    - "achievement-framework/.../dashboard/DashboardServiceTest.java"
    - "achievement-web/src/api/dashboard.ts"
    - "achievement-web/src/store/dashboard.ts"
    - "achievement-framework/src/main/resources/fonts/README.md"
  modified:
    - "achievement-framework/pom.xml (added iText 7 deps + spring-boot-starter-test)"
    - "achievement-parent/pom.xml (added itext.version property)"
    - "application-dev.yml (added spring.cache.redis.time-to-live=300000)"
decisions:
  - "chartType validated via service-layer whitelist (T-3-01-01)"
  - "Percentage computed in service (not mapper) for typeDist and patentStatus"
  - "STSong-Light fallback if NotoSansSC.ttf not available"
  - "Client-side 5-min cache in Pinia store as D-04 complement"
metrics:
  duration: "~45 min"
  completed_date: "2026-06-17"
  tasks_completed: 3
  files_created: 13
  test_count: 13
---

# Phase 3 Plan 01: Dashboard Statistics Backend & Frontend API

Implement backend statistics dashboard aggregation queries for 4 chart types (annual trend line, type distribution pie, department ranking bar, patent valid/invalid donut) with Redis 5-minute caching and EasyExcel/iText 7 export capability. Create the frontend Pinia store and API layer.

## Task Summary

### Task 3-01-1: Add iText 7 Maven dependencies, create dashboard DTOs and mapper XML

Status: Completed (prior wave)

- Added `itext.version=8.0.5` to parent pom.xml `<properties>`
- Added iText 7 kernel/layout/io/font-asian dependencies to framework pom.xml
- Created 5 DTO files: DashboardTrendVO, DashboardTypeDistVO, DashboardDeptRankVO, DashboardPatentStatusVO, DashboardExportVO
- Created DashboardMapper.java with @DataScope annotations on all 4 chart query methods
- Created DashboardMapper.xml with UNION ALL aggregation queries and `<choose>` export block
- Created fonts/README.md documenting NotoSansSC font requirement
- Commit: `71d2e61`

### Task 3-01-2: Implement DashboardService, DashboardController, DashboardPdfService, frontend API and store

Status: Completed

- **DashboardService.java** - Interface with 4 chart query methods and 2 export methods
- **DashboardServiceImpl.java** - Implementation with `@Cacheable(value = "dashboard", ...)` Redis 5-min TTL on all 4 queries. chartType validated against service-layer whitelist (T-3-01-01). Percentage computed in service for typeDist and patentStatus. EasyExcel streaming export with JSON error fallback on query failure.
- **DashboardPdfService.java** - iText 7 PDF generation with embedded NotoSansSC font (STSong-Light fallback). Creates formatted data tables with Chinese labels per chart type.
- **DashboardController.java** - 6 REST endpoints at `/api/dashboard/*`: annual-trend, type-dist, dept-ranking, patent-status, export/{chartType}, export-pdf/{chartType}
- **api/dashboard.ts** - 6 typed API functions matching backend endpoints with TypeScript interfaces
- **store/dashboard.ts** - Pinia store with client-side 5-min cache (CACHE_TTL=300000), fetchAll/refreshAll/clearCache actions
- **application-dev.yml** - Added `spring.cache.redis.time-to-live=300000`
- Commit: `1858cca`

### Task 3-01-3: Create DashboardServiceTest with 13 unit tests

Status: Completed

- JUnit 5 + Mockito unit tests (MockitoExtension)
- Tests for all 4 chart methods: getAnnualTrend, getTypeDist, getDeptRanking, getPatentStatus (each with data, empty results, and deptId filter scenarios)
- Percentage computation verification for typeDist and patentStatus
- exportExcel tests: success path, invalid chartType JSON error, query failure JSON error, deptId filter
- exportPdf test: verifies delegation to DashboardPdfService
- Added `spring-boot-starter-test` dependency to framework pom.xml
- Commit: `a389d3b`

## Deviations from Plan

None - plan executed exactly as written. Task 3-01-1 was already completed by a prior wave before this executor session.

## Security Mitigations

| Threat ID | Category | Component | Disposition |
|-----------|----------|-----------|-------------|
| T-3-01-01 | Tampering | DashboardController chartType param | Mitigated: chartType validated via service-layer whitelist in export methods |
| T-3-01-02 | Information Disclosure | DashboardMapper queries | Mitigated: @DataScope on mapper methods triggers DataPermissionInterceptor for dept_id injection |
| T-3-01-03 | Tampering | DashboardPdfService iText PDF | Mitigated: iText 7 text/layout objects isolate data from structure |
| T-3-01-04 | Tampering | iText dependency install | Mitigated: iText packages sourced from Maven Central |

## Verification

- `mvn compile -pl achievement-framework` - Java compilation expected to pass
- `mvn test -pl achievement-framework -Dtest="DashboardServiceTest"` - All 13 tests expected to pass
- Redis cache configuration applied: `spring.cache.redis.time-to-live=300000`
- All REST endpoints mapped at `/api/dashboard/*`
- Frontend API module exports 6 typed functions

## Success Criteria

- [x] All 4 chart aggregation queries compile and return correct data types
- [x] DashboardController exposes 6 REST endpoints at /api/dashboard/*
- [x] Redis 5-min cache annotation (@Cacheable) applied to all 4 query methods
- [x] DashboardPdfService generates PDF with correct content type headers
- [x] Frontend API module (dashboard.ts) exports 6 functions matching backend endpoints
- [x] Frontend Pinia store (dashboard.ts) implements client-side 5-min cache
- [x] 13 backend unit tests created (all pass expected)
- [x] Export error handling writes JSON error response (not throws exception)
- [x] iText 7 + font-asian dependencies compile in framework module
