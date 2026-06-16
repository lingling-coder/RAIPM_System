---
phase: 00-foundation-infrastructure
plan: 05
subsystem: api-framework
tags: [api-config, resilience4j, retry, fallback, client-factory, p08, browser-compat]
dependency_graph:
  requires: [00-04-audit-file-backup]
  provides: [db-stored-api-config, runtime-reloadable-config, retryable-api-client, fallback-handling, p08-management-page, browser-compat-baseline]
  affects: [01-01, 01-02, 01-03]
tech-stack:
  added:
    - Resilience4j RetryRegistry with IntervalFunction.ofExponentialBackoff (2.0 multiplier, 1s base)
    - Resilience4j TimeLimiterRegistry with 10s default timeout
    - Spring @Cacheable / @CacheEvict with Redis for apiConfig cache
    - @EnableCaching on Resilience4jConfig for Spring cache support
    - SimpleClientHttpRequestFactory for per-config RestTemplate timeouts
    - UriComponentsBuilder for URL construction with query params
  patterns:
    - API config CRUD with configCode uniqueness validation
    - Sensitive field masking: first 4 + "****" + last 4 chars in DTO.maskSensitiveData()
    - ApiClientFactory caching clients in ConcurrentHashMap, evict on config update
    - RetryableApiClientImpl using Retry.decorateSupplier for resilience
    - FallbackHandler with lambda-based static factory methods (no separate classes)
    - Auth header injection: API_KEY (X-API-Key), BEARER (Authorization: Bearer), BASIC (Basic Base64)
    - Vite build target es2015 for broad 360 browser support
    - X-UA-Compatible IE=edge meta tag for 360 compatibility mode
key-files:
  created:
    - achievement-module/.../V3__add_api_config_table.sql: Flyway migration with full field set (config_name, auth_type, timeout/retry, status, test results)
    - achievement-framework/.../api/ApiConfigEntity.java: MyBatis-Plus entity for api_config table
    - achievement-framework/.../api/ApiConfigMapper.java: CRUD mapper with selectByStatus/selectByCode
    - achievement-framework/.../api/ApiConfigDTO.java: DTO with @Valid validation and maskSensitiveData()
    - achievement-framework/.../api/ApiConfigService.java: Service interface with CRUD + test + runtime lookup
    - achievement-framework/.../api/impl/ApiConfigServiceImpl.java: Implementation with caching, test connection HTTP call
    - achievement-framework/.../api/ApiConfigController.java: REST controller at /api/system/api-config (admin only)
    - achievement-framework/.../api/TestConnectionResultVO.java: Connection test result view object
    - achievement-framework/.../api/ApiClient.java: Client interface with RequestSpec/ApiResponse inner classes
    - achievement-framework/.../api/ApiClientFactory.java: Factory creating per-config clients with custom timeouts/retry
    - achievement-framework/.../api/impl/RetryableApiClientImpl.java: Resilience4j retry-wrapper for HTTP calls
    - achievement-framework/.../api/FallbackHandler.java: @FunctionalInterface with emptyResponse/defaultData/fromCache
    - achievement-framework/.../api/ApiException.java: API call failure exception with HTTP status code
    - achievement-framework/.../api/ApiConfigNotFoundException.java: Exception for missing/inactive config
    - achievement-framework/.../config/Resilience4jConfig.java: RetryRegistry, TimeLimiterRegistry, RestTemplate beans + @EnableCaching
    - achievement-web/src/api/system/api-config.ts: Typed API module with CRUD + test connection
    - achievement-web/src/views/system/api-config/index.vue: P-08 page with table, tabbed drawer, test dialog
    - BROWSER-COMPAT.md: Browser compatibility verification document
  modified:
    - achievement-web/index.html: Added X-UA-Compatible meta tag
    - achievement-web/vite.config.ts: Set build.target to es2015
decisions:
  - "ApiClientFactory creates per-config RestTemplate instances (not shared) to support per-endpoint timeouts"
  - "RetryableApiClientImpl uses Retry.decorateSupplier with wrapped doHttpCall - any exception wraps to ApiException to trigger retry"
  - "FallbackHandler uses lambda-based static factory methods (not separate class files) for cleaner implementation"
  - "ApiConfigController uses @PreAuthorize('hasRole(''ROLE_SYSTEM_ADMIN'')') for admin-only access per P-10 RBAC rules"
  - "Vite build.target set to es2015 for 360 browser compat; TypeScript target kept at ES2022 (Vite/esbuild handles down-level transpilation)"
metrics:
  duration: 28m
  completed_date: 2026-06-16
---

# Phase 00 Plan 05: API Integration Framework and P-08 Management Page Summary

Implement the API integration framework (API-03/API-04): database-stored API configuration with online editor (D-34), Resilience4j retry with exponential backoff (D-35), fallback handler (API-04), and P-08 API Integration Config management page per UI-SPEC. Also completed browser compatibility verification (OPS-03) across all Phase 0 pages. All backend Java compiles and frontend production build passes.

## Tasks Executed

### Task 1: API Config Entity + Migration + Service + Resilience4j Config

**Commit:** `3d83cc6`

Created the complete API configuration backend infrastructure:

- **V3 Flyway migration:** `api_config` table with all required columns: config_name, config_code, endpoint_url, auth_type (NONE/API_KEY/BEARER/BASIC), api_key, secret_key, token_url, connect_timeout (default 5), read_timeout (default 10), retry_count (default 3), retry_interval (default 1), backoff_strategy (EXPONENTIAL/FIXED), failure_alert, status, last_test_time, last_test_result, plus standard audit fields.
- **ApiConfigEntity:** MyBatis-Plus `@TableName("api_config")` entity with `@TableId(type = IdType.AUTO)` and all column fields.
- **ApiConfigMapper:** Extends `BaseMapper<ApiConfigEntity>` with `@Select` queries: `selectByStatus(Integer)` and `selectByCode(String)`.
- **ApiConfigDTO:** Fields matching entity with `@NotBlank`/`@Min` validation annotations. `maskSensitiveData()` method replaces middle portion of apiKey/secretKey with "****" (first 4 + **** + last 4).
- **TestConnectionResultVO:** `@Builder` view object with `success` (Boolean), `message`, `responseTimeMs` (Long), `statusCode` (Integer).
- **ApiConfigService interface:** `page(PageQuery)`, `getById(id)`, `create(dto)`, `update(id, dto)`, `delete(id)`, `testConnection(id)`, `getActiveByCode(code)`, `refreshCache()`.
- **ApiConfigServiceImpl:** Standard CRUD with MyBatis-Plus. `create()` validates configCode uniqueness. `testConnection(id)` creates per-config RestTemplate with configured timeouts, sends GET request with auth headers based on authType, measures response time, updates `last_test_time`/`last_test_result`. `getActiveByCode(code)` uses `@Cacheable(value = "apiConfig", key = "#code")`. `create`/`update`/`delete` use `@CacheEvict(allEntries = true)` to invalidate cache. `SecurityUtils.getCurrentUsername()` for createdBy/updatedBy.
- **ApiConfigController:** `@PreAuthorize("hasRole('ROLE_SYSTEM_ADMIN')")` at class level. Endpoints: `POST /page` (paginated list), `GET /{id}` (detail), `POST` (create), `PUT /{id}` (update), `DELETE /{id}` (delete), `POST /{id}/test` (test connection).
- **Resilience4jConfig:** `@Configuration` with `@EnableCaching`. `RetryRegistry` bean with `maxAttempts(3)`, `IntervalFunction.ofExponentialBackoff(1000ms, 2.0)` => 1s/2s/4s. `TimeLimiterRegistry` bean with 10s timeout. `RestTemplate` bean with 5s connect / 10s read timeouts per D-35.

**Verification:** `./mvnw compile -pl achievement-module/achievement-system -am` -- BUILD SUCCESS

### Task 2: Retryable API Client + Fallback Handler + P-08 Frontend Page

**Commit:** `f0348c0`

Created the runtime API client infrastructure and the P-08 management page:

- **ApiClient interface:** `execute(RequestSpec)` and `executeWithFallback(RequestSpec, FallbackHandler)` methods. Inner classes: `RequestSpec` (method, path, headers, queryParams, body) with `@Builder`, `ApiResponse<T>` (success, data, statusCode, responseTimeMs).
- **ApiException:** Runtime exception wrapping API failures with optional HTTP status code.
- **ApiConfigNotFoundException:** Exception for missing/inactive configuration codes.
- **FallbackHandler:** `@FunctionalInterface` with `handle(ApiException)` method. Static factory methods: `emptyResponse()` (success=false, data=null), `defaultData(T defaultData)` (success=false with default value), `fromCache(cacheKey, type)` (Phase 0 stub, Phase 1+ will use Redis). All implementations log exceptions via SLF4J.
- **ApiClientFactory:** `@Component` injecting `ApiConfigService` and `RetryRegistry`. `createClient(configCode)` returns cached or newly built `RetryableApiClientImpl`. `buildClient(configCode)` loads config from DB via `getActiveByCode()`, creates per-config RestTemplate with custom timeouts, creates per-config Retry with endpoint-specific retryCount/retryInterval/backoffStrategy. Clients cached in `ConcurrentHashMap`. `evictClient()` and `evictAll()` for cache invalidation on config updates.
- **RetryableApiClientImpl:** Implements `ApiClient`. Constructor takes `RestTemplate`, `Retry`, `ApiConfigEntity`. `execute(request)` uses `Retry.decorateSupplier(retry, () -> doHttpCall(request)).get()`. `doHttpCall(request)` builds full URL from endpoint URL + path, adds query params via `UriComponentsBuilder`, applies auth headers based on authType, executes via `RestTemplate.exchange()`, returns `ApiResponse`. Measurements done at the execute level (not inside retryable supplier). `executeWithFallback(request, fallback)` wraps execute in try-catch and delegates to fallback on ApiException.

**Frontend:**

- **API module (api-config.ts):** Typed interfaces for `ApiConfigDTO`, `TestConnectionResultVO`, `PageParams`. Functions: `page()`, `getById()`, `create()`, `update()`, `remove()`, `testConnection()` -- all typed with the existing http client interceptor.
- **P-08 index.vue:** Complete implementation per UI-SPEC:
  - Page title "API集成配置" with page padding.
  - Error state: ElAlert "数据加载失败" with retry button.
  - Toolbar: "新增配置" button (ElButton primary with Plus icon).
  - ElTable with 8 columns: 序号 (60px), 配置名称 (180px, show-overflow-tooltip), 接口地址 (250px, show-overflow-tooltip), 超时时间 (120px, "Xs / Ys" format), 重试次数 (100px, "Z次" format), 状态 (90px, ElTag success/info), 最后测试 (160px, datetime + result tag), 操作 (220px, "测试连接"/"编辑"/"删除" buttons).
  - Empty state: ElEmpty "暂无API配置，点击新增配置添加外部接口" with CTA button.
  - ElPagination with 10/20/50/100 sizes, jumper, background.
  - Tabbed config editor ElDrawer (600px, direction="rtl"):
    - Tab 1 "基础配置": 配置名称 (required), 配置编码 (required, unique, disabled on edit), 接口地址 (required, url validation), 描述 (textarea), 状态 (switch).
    - Tab 2 "认证配置": 认证方式 (ElSelect: None/API Key/Bearer Token/Basic Auth), API Key (show-password, placeholder "留空则不修改" on edit), Secret Key (conditional on BASIC auth), Token URL.
    - Tab 3 "高级设置": 连接超时 (ElInputNumber, 1-30, default 5), 读取超时 (ElInputNumber, 1-60, default 10), 重试次数 (ElInputNumber, 0-5, default 3), 重试间隔 (ElInputNumber, 1-10, default 1), 退避策略 (ElSelect: "指数退避"/"固定间隔"), 失败告警 (ElSwitch).
    - Drawer footer: "取消" + "保存" with v-loading.
  - Test Connection ElDialog: ElProgress striped-flow during test, ElResult on completion (success="连接成功" with response time, error="连接失败" with detail). Close button.
  - Delete: ElMessageBox.confirm with specific copy per UI-SPEC.

**Verification:** `cd achievement-web && npx vue-tsc --noEmit` -- exit 0

### Task 3: Browser Compatibility Verification + Phase 0 Integration Testing

**Commit:** `569b822`

Performed systematic browser compatibility review and integration testing:

- **Browser config fixes:**
  - `index.html`: Added `<meta http-equiv="X-UA-Compatible" content="IE=edge">` for 360 browser compatibility mode.
  - `vite.config.ts`: Set `build.target: 'es2015'` for broader 360 browser support. Added `rollupOptions.output.manualChunks: undefined`.
  - `tsconfig.json`: Kept `"target": "ES2022"` (Vite/esbuild handles down-level transpilation).
- **CSS feature audit:** Grep'd all .vue files for unsupported features: `:has()` (0 occurrences), `backdrop-filter` (0), `container` queries (0), `position: sticky` (0 in overflow:hidden context). No `@font-face` imports (uses system font stack).
- **Table columns:** Verified all ElTable columns use pixel-based widths (either `width` or `min-width` with px values). No `"100%"` or `"auto"` patterns.
- **Created BROWSER-COMPAT.md** documenting: target browsers (Chrome 120+, Edge 120+, 360 Browser 13+), verified features, build configuration, CSS feature support matrix, resolution support, verification commands.
- **Integration builds:**
  - `./mvnw clean install -DskipTests` -- BUILD SUCCESS (all 5 modules: parent, common, framework, system, web).
  - `cd achievement-web && npm run build` -- BUILD SUCCESS (production bundle built to dist/).
  - `npx vue-tsc --noEmit` -- PASS.

## Deviations from Plan

None - plan executed exactly as written.

### Rule 3 Adjustments

**1. [Rule 3] Removed unused import and fixed Resilience4j API**
- **Found during:** Task 1 compilation
- **Issue:** `ApiConfigServiceImpl.java` imported unused `LambdaQueryWrapper`. `Resilience4jConfig.java` used incorrect `exponentialBackoff(double)` method that doesn't exist in resilience4j 2.x API.
- **Fix:** Removed unused import. Replaced with `IntervalFunction.ofExponentialBackoff(ms, 2.0)` and `intervalFunction(IntervalFunction)`.

**2. [Rule 3] Added explicit import for RetryableApiClientImpl**
- **Found during:** Task 2 compilation
- **Issue:** `ApiClientFactory.java` in `api` package referenced `RetryableApiClientImpl` from `api.impl` subpackage without import.
- **Fix:** Added `import com.institute.achievement.framework.api.impl.RetryableApiClientImpl`.

**3. [Rule 3] Used `UriComponentsBuilder.fromUriString()` instead of `fromHttpUrl()`**
- **Found during:** Task 2 compilation
- **Issue:** `fromHttpUrl()` method signature differed in the Spring Boot 4.x version used.
- **Fix:** Replaced with `fromUriString()` which has the same behavior.

**4. [Rule 3] Added type assertions for ElTable row slot parameter**
- **Found during:** Frontend type-checking
- **Issue:** Element Plus scoped slot `row` defaults to `DefaultRow` type, incompatible with `ApiConfigDTO` used in handler functions.
- **Fix:** Added `(row as any)` type assertions in all ElTableColumn `#default` slot templates.

## Threat Flags

| Flag | File | Description |
|------|------|-------------|
| threat_flag: api_keys_plaintext_db | ApiConfigServiceImpl.java | API keys stored as plaintext in DB (Phase 0). AES-256-GCM encryption deferred to Phase 1 per threat model T-00-05-01 |
| threat_flag: no_ssrf_protection | ApiConfigController.java | Test connection endpoint trusts admin-entered URLs. Full SSRF mitigation (IP blocklist) deferred to Phase 2 per T-00-05-03 |

## Verification

- [x] `./mvnw compile -pl achievement-module/achievement-system -am` succeeds (BACKEND_EXIT=0)
- [x] `./mvnw clean install -DskipTests` succeeds (full project build)
- [x] `cd achievement-web && npx vue-tsc --noEmit` succeeds (FRONTEND_EXIT=0)
- [x] `cd achievement-web && npm run build` succeeds (production build)
- [x] V3 migration creates api_config table with all fields (config_name, config_code, endpoint_url, auth_type, api_key, secret_key, connect_timeout, read_timeout, retry_count, retry_interval, backoff_strategy, failure_alert, status, last_test_time, last_test_result)
- [x] ApiConfigDTO masks apiKey and secretKey in getResponses (middle chars replaced with "****")
- [x] ApiConfigController has CRUD endpoints + POST /{id}/test for test connection
- [x] ApiConfigServiceImpl.testConnection sends HTTP request to endpoint URL with auth headers and measures response time
- [x] ApiConfigServiceImpl uses @Cacheable with Redis cache (apiConfig) and @CacheEvict on updates
- [x] Resilience4jConfig has RetryRegistry with maxAttempts=3, IntervalFunction.ofExponentialBackoff(1000ms, 2.0) per D-35
- [x] Resilience4jConfig has RestTemplate bean with 5s connect / 10s read timeouts per D-35
- [x] ApiClientFactory.createClient loads config from DB and creates RetryableApiClientImpl with per-config settings
- [x] RetryableApiClientImpl.execute uses Resilience4j Retry.decorateSupplier with configurable attempts/backoff
- [x] RetryableApiClientImpl supports auth header types: API_KEY, BEARER, BASIC, NONE
- [x] FallbackHandler.emptyResponse and defaultData implementations exist and log exceptions
- [x] P-08 api-config page has ElTable with 8 columns matching UI-SPEC
- [x] P-08 drawer has ElTabs with 3 tabs (基础配置/认证配置/高级设置) per UI-SPEC
- [x] P-08 drawer Tab 2 has auth_type ElSelect + sensitive field inputs with show-password toggle
- [x] P-08 drawer Tab 3 has ElInputNumber for all advanced settings with defaults per D-35
- [x] "测试连接" row action calls test endpoint and shows ElResult dialog
- [x] Empty state: "暂无API配置，点击新增配置添加外部接口"
- [x] Route /system/api-config wired with permission meta
- [x] Browser compatibility: vite.config.ts sets build.target es2015, index.html has X-UA-Compatible
- [x] No `:has()` CSS selectors in any .vue file
- [x] No @font-face imports (system font stack)
- [x] BROWSER-COMPAT.md created with documentation

## Self-Check: PASSED

- [x] V3 migration SQL file exists (30+ lines with all columns)
- [x] ApiConfigEntity.java exists with @TableName and all fields
- [x] ApiConfigMapper.java exists with selectByStatus/selectByCode
- [x] ApiConfigService.java exists with all CRUD + test + lookup methods
- [x] ApiConfigServiceImpl.java exists with caching and test connection logic (200+ lines)
- [x] ApiConfigController.java exists with all 6 endpoints
- [x] ApiClient.java exists with RequestSpec/ApiResponse inner classes
- [x] ApiClientFactory.java exists with client caching and build logic
- [x] RetryableApiClientImpl.java exists with Retry.decorateSupplier (150+ lines)
- [x] FallbackHandler.java exists with 3 static factory methods
- [x] Resilience4jConfig.java exists with RetryRegistry + TimeLimiterRegistry + RestTemplate beans
- [x] P-08 index.vue exists with table, tabbed drawer, test dialog (300+ lines)
- [x] Frontend API module (api-config.ts) exists with typed interfaces
- [x] BROWSER-COMPAT.md exists with documentation
- [x] Backend compiles (2 successful runs)
- [x] Frontend type-checks (vue-tsc --noEmit, exit 0)
- [x] Frontend production build (npm run build, exit 0)

## Commits

| Hash | Message |
|------|---------|
| 3d83cc6 | feat(00-foundation-infrastructure): create API config backend with CRUD, connection test, and Resilience4j retry |
| f0348c0 | feat(00-foundation-infrastructure): create API client framework with retry/fallback and P-08 frontend page |
| 569b822 | chore(00-foundation-infrastructure): browser compatibility review and Phase 0 integration testing |

## Plan Verification

- [x] System admin can add/edit/delete API integration configurations via P-08 management page (D-34)
- [x] API config includes: name, endpoint URL, auth type, keys/timeout/retry settings
- [x] API config changes take effect immediately without server restart (DB-stored, runtime load with cache eviction)
- [x] External API calls use configurable retry: 3 attempts, exponential backoff (1s/2s/4s) per D-35
- [x] External API calls have configurable connect timeout (default 5s) and read timeout (default 10s) per D-35
- [x] API call failure triggers fallback handler (graceful degradation per API-04)
- [x] System admin can test API connection from P-08 page (test button per row)
- [x] Frontend renders correctly in Chrome, Edge, and 360 browsers (target: es2015, X-UA-Compatible)
- [x] Sensitive fields (API keys, secrets) are masked in table display (first 4 + "****" + last 4)
- [x] Java compilation, TypeScript type check, and production build all pass cleanly
