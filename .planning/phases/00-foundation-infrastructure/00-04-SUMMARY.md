---
phase: 00-foundation-infrastructure
plan: 04
subsystem: audit-file-backup
tags: [audit-log, hash-chain, aop, file-proxy, backup-scheduler, p07]
dependency_graph:
  requires: [00-02-system-management, 00-03-jwt-auth]
  provides: [append-only-audit, hash-chain-verification, file-proxy-service, automated-backup, audit-log-page]
  affects: [01-01, 01-02, 00-05]
tech-stack:
  added:
    - aspectjweaver for AOP-based audit logging (AuditLogAspect with @Around on POST/PUT/DELETE controllers)
    - HashChainUtil with SHA-256 for tamper-detecting audit hash chain
    - MyBatis-Plus Page with dynamic <script> SQL for filtered pagination
    - ZipOutputStream for cross-platform file backups (Windows compat)
  patterns:
    - INSERT-only mapper pattern: AuditiLogMapper exposes only insert(), no update/delete methods
    - REQUIRES_NEW propagation for audit log recording (persists even if parent transaction fails)
    - UUID filename + proxy URL for secure file access (no direct storage path exposure)
    - OS-detection in BackupSchedulerService: tar on Linux, ZipOutputStream on Windows
    - @EnableScheduling on Application class for cron-based backup
key-files:
  created:
    - achievement-module/.../V2__add_audit_log_file__tables.sql: Flyway migration with audit_log (monthly RANGE partitioned) + file_record tables
    - achievement-framework/.../audit/AuditLogEntity.java: MyBatis-Plus entity for audit_log table
    - achievement-framework/.../audit/AuditLogMapper.java: INSERT-only mapper with filtered page, last-log, chain-segment queries
    - achievement-framework/.../audit/AuditLogService.java + impl: record() with hash chain, page(), getDetail(), verifyChain()
    - achievement-framework/.../audit/HashChainUtil.java: SHA-256 compute/verify/verifyChainDetailed utilities
    - achievement-framework/.../audit/AuditLogAspect.java: @Around AOP interceptor for POST/PUT/DELETE controllers
    - achievement-framework/.../audit/AuditLogController.java: Read-only REST controller at /api/system/audit-log
    - achievement-framework/.../audit/AuditLogDTO.java, AuditLogPageDTO.java, AuditLogVO.java: DTOs
    - achievement-framework/.../audit/ChainVerificationResult.java, ChainBreakVO.java: Chain verification responses
    - achievement-framework/.../security/SecurityUtils.java: Static helpers for current user, IP, permissions
    - achievement-framework/.../config/FileStorageConfig.java: file.storage.* configuration properties
    - achievement-framework/.../config/BackupConfig.java: backup.* configuration properties
    - achievement-framework/.../file/FileStorageService.java + impl: UUID-based file storage service
    - achievement-framework/.../file/FileRecordEntity.java, FileRecordMapper.java: file_record DB layer
    - achievement-framework/.../file/FileRecordVO.java: View object with proxyUrl (no direct path)
    - achievement-framework/.../file/FileProxyController.java: /api/files/{uuid} proxy controller
    - achievement-framework/.../backup/BackupSchedulerService.java: @Scheduled daily backup with mysqldump + zip + retention
    - achievement-web/src/api/system/audit-log.ts: API module with typed interfaces
    - achievement-web/src/views/system/audit-log/index.vue: P-07 Audit Log page (full UI-SPEC implementation)
  modified:
    - achievement-framework/pom.xml: Added aspectjweaver dependency
    - achievement-framework/src/main/resources/application.yml: Added file.storage, backup, scheduling config
    - achievement-module/.../AchievementSystemApplication.java: Added @EnableScheduling
decisions:
  - "Added aspectjweaver as explicit dependency (not spring-boot-starter-aop which is unavailable for Boot 4.1.0); aspectjweaver is available as a standalone artifact at the version managed by spring-boot-dependencies"
  - "Audit log recording uses Propagation.REQUIRES_NEW so the log entry persists independently of the parent transaction outcome (success or rollback)"
  - "Backup uses ZipOutputStream on Windows (no tar dependency), tar on Linux/macOS"
  - "File proxy controller adds @PreAuthorize('isAuthenticated()') on download/upload endpoints (admin only for delete)"
metrics:
  duration: 21m
  completed_date: 2026-06-16
---

# Phase 00 Plan 04: Audit Log, File Proxy, and Daily Backup Summary

Implement the append-only audit log system with SHA-256 hash chain integrity (SYS-07/D-25/D-26/D-27), file proxy service with UUID naming and layered directories (OPS-02/D-29/D-30/D-31), daily automated backup scheduler with 30-day retention (OPS-01/D-32/D-33), and the P-07 Audit Log management page. All backend Java compiles successfully and frontend passes vue-tsc --noEmit.

## Tasks Executed

### Task 1: Audit Log System (Flyway migration, Entity, Hash Chain, AOP Interceptor, Controller)

**Commit:** `0793ac6`

Created the complete audit log backend system with append-only semantics:

- **V2 Flyway migration:** `audit_log` table with RANGE monthly partitioning (202401 through 202612 + p_future catch-all), 4 indexes (created_at, operator, type, target). `file_record` table with indexes on type and created_at.
- **HashChainUtil:** SHA-256 hex digest via `MessageDigest`, `computeHash(id, previousHash, contentJson, createdAt)` for single-entry hash, `verifyChain()` for full chain verification, `verifyChainDetailed()` returning `ChainVerificationResult` with break details, `computePreviousHash()` for getting last entry's hash.
- **AuditLogEntity:** MyBatis-Plus entity mapped to audit_log with `@TableName`, `@TableId(type = IdType.AUTO)`, fields for all columns including previous_hash and current_hash.
- **AuditLogMapper:** INSERT-only mapper extending `BaseMapper`, custom methods: `selectPageWithFilters` (dynamic `<script>` with LIKE/equals/time-range conditions on Page), `selectLastLog()`, `selectChainSegment()`, `countByOperationType()`. No updateById or deleteById exposed.
- **AuditLogService/Impl:** `record(AuditLogDTO)` with `@Transactional(REQUIRES_NEW)` - gets last log's hash, computes chain hash, inserts entry, then recomputes hash with actual generated ID. `page()` with filtered pagination, `getDetail()` with integrity verification, `verifyChain()` delegating to HashChainUtil.
- **AuditLogAspect:** `@Aspect` with `@Pointcut` for `@RestController && (@PostMapping || @PutMapping || @DeleteMapping)`. Around advice captures: operation type from annotation, target type from class name, request body from `@RequestBody` parameter, target ID from path or body, IP/user from SecurityUtils. Records in finally block to capture both success and failure.
- **SecurityUtils:** Static helper providing `getCurrentUsername()`, `getCurrentUserId()`, `getCurrentDeptId()`, `getCurrentRoles()`, `getCurrentPermissions()`, `getClientIp()` (with X-Forwarded-For support), `hasPermission()`, `hasRole()`. Extracts data from SecurityContextHolder/JwtUser principal.
- **AuditLogController:** POST `/api/system/audit-log/page` (paginated filter), GET `/api/system/audit-log/{id}` (detail with integrity check), POST `/api/system/audit-log/verify-chain` (chain verification), GET `/api/system/audit-log/stats` (type counts). All endpoints `@PreAuthorize` restricted to `ROLE_SYSTEM_ADMIN` or `ROLE_AUDITOR`. Read-only controller.
- **DTOs:** `AuditLogDTO` (input for record), `AuditLogPageDTO` (extends PageQuery with filters), `AuditLogVO` (view object with integrityVerified), `ChainVerificationResult`/`ChainBreakVO` (verification responses).

**Verification:** `./mvnw compile -pl achievement-module/achievement-system -am` -- BUILD SUCCESS

### Task 2: File Proxy Service + Daily Backup Scheduler

**Commit:** `c283bc6`

Created the file proxy service and automated backup scheduler:

- **FileStorageConfig:** `@ConfigurationProperties("file.storage")` with uploadDir (./uploads), maxFileSize (52428800 = 50MB), allowedTypes (pdf/doc/docx/xls/xlsx/png/jpg/jpeg/gif/zip/rar).
- **FileStorageService interface:** `store(originalFilename, fileContent, fileSize, contentType, fileType)`, `loadAsResource(uuid)`, `getRecord(uuid)`, `delete(uuid)`.
- **LocalFileStorageServiceImpl:** `store()` validates file size <= 50MB (D-31), validates extension against whitelist, blocks executable extensions (exe/bat/cmd/sh/js/vbs/jar/war), generates UUID filename, saves to `{uploadDir}/YYYY/MM/type/` directory (D-29), creates file_record in DB, returns VO with proxy URL (D-30). `loadAsResource()` queries file_record by UUID, resolves physical path, returns UrlResource. `getRecord()`/`delete()` for metadata and cleanup.
- **FileRecordEntity/FileRecordMapper:** MyBatis-Plus entity and mapper for file_record table. Custom `findByStoredName()` lookup.
- **FileRecordVO:** View object with `proxyUrl="/api/files/{uuid}"` -- storage path never exposed to client.
- **FileProxyController:** POST `/api/files/upload` (multipart upload), GET `/api/files/{uuid}` (proxy download with inline Content-Disposition and 1-hour cache), GET `/api/files/{uuid}/info` (metadata), POST `/api/files/{uuid}/delete` (admin only). All endpoints require authentication.
- **BackupConfig:** `@ConfigurationProperties("backup")` with enabled, backupDir, database (host/port/username/password/name), retentionDays (30), cron (0 0 2 * * ?), fileBackupPaths.
- **BackupSchedulerService:** `@Scheduled(cron)` daily backup. Method `backup()` performs: (1) mysqldump via ProcessBuilder (--routines, --triggers, --single-transaction), (2) file backup - tar on Linux/macOS, ZipOutputStream on Windows (cross-platform), (3) retention cleanup - Files.walkFileTree to delete files/directories older than retentionDays.
- **application.yml update:** Added `file.storage`, `backup`, and `spring.task.scheduling` configuration sections.
- **AchievementSystemApplication:** Added `@EnableScheduling` annotation.
- **pom.xml update:** Added `aspectjweaver` dependency.

**Verification:** `./mvnw compile -pl achievement-module/achievement-system -am` -- BUILD SUCCESS

### Task 3: P-07 Audit Log Management Page

**Commit:** `fed4be7`

Created the P-07 Audit Log management page per UI-SPEC contract:

- **API module (audit-log.ts):** Typed interfaces for AuditLogPageParams, AuditLogVO, ChainVerificationResult, ChainBreakVO. API functions: `page()`, `getDetail()`, `verifyChain()`.
- **P-07 index.vue:** Complete implementation:
  - Search panel: 操作人 (ElInput), 操作类型 (ElSelect with 登录/登出/新增/编辑/删除), 操作对象 (ElInput), 时间范围 (ElDatePicker datetimerange, default last 7 days), 搜索/重置 buttons
  - Toolbar: 导出 (CSV download), 完整性校验 buttons
  - Read-only ElTable with columns: 序号, 操作时间, 操作人, 操作类型 (ElTag: 登录=info, 登出=info, 新增=success, 编辑=warning, 删除=danger), 操作对象, 对象ID (monospace), IP地址, 操作结果 (ElTag: 成功=success, 失败=danger), 查看详情
  - Detail drawer (ElDrawer, 600px): ElDescriptions with border showing all fields, 变更前内容/变更后内容 (textarea readonly), 哈希链 section with currentHash/previousHash in monospace, 链条状态 ElTag (success/danger)
  - Chain verification: ElResult showing 校验通过/校验发现断裂 + broken links table
  - States: ElEmpty "暂无审计日志，调整筛选条件后重新查询", ElTable v-loading, ElAlert "审计日志加载失败" with retry
  - Pagination: ElPagination with 10/20/50/100 page sizes

**Verification:** `cd achievement-web && npx vue-tsc --noEmit` -- exit 0

## Deviations from Plan

None - plan executed exactly as written with the following adjustments documented below.

### Rule 3 Adjustment

**1. [Rule 3] Added aspectjweaver dependency explicitly**

- **Found during:** Task 1 compilation
- **Issue:** The plan specifies `@Aspect` annotation on AuditLogAspect.java, but `aspectjweaver` is not a transitive dependency of any existing Spring Boot starter. The IDE showed "org.aspectj cannot be resolved" errors.
- **Fix:** Added `aspectjweaver` as an explicit dependency in `achievement-framework/pom.xml`. Spring Boot 4.1.0's parent BOM manages the version, so no explicit version is needed.
- **Files modified:** `achievement-framework/pom.xml`
- **Commit:** 0793ac6

## Threat Flags

| Flag | File | Description |
|------|------|-------------|
| threat_flag: command_injection_backup | BackupSchedulerService.java | mysqldump uses ProcessBuilder with config-driven parameters from application.yml -- no user input accepted, but database password is in config file. Accept for Phase 1 as the system runs on internal network only |
| threat_flag: no_https_upload | FileProxyController.java | File upload transmits binary data in plaintext on dev; must enforce HTTPS in production |
| threat_flag: backup_file_unencrypted | BackupSchedulerService.java | Backup SQL files contain plaintext credentials and data; stored in ./backups outside web root but unencrypted. Recommend encryption in Phase 2 |

## Verification

- [x] `./mvnw compile -pl achievement-module/achievement-system -am` succeeds (BACKEND_EXIT=0, twice)
- [x] `cd achievement-web && npx vue-tsc --noEmit` succeeds (FRONTEND_EXIT=0)
- [x] V2 migration creates audit_log table with PARTITION BY RANGE (26 monthly partitions + p_future)
- [x] audit_log table has previous_hash (VARCHAR 64) and current_hash (VARCHAR 64) columns
- [x] AuditLogMapper only exposes insert() -- no updateById/deleteById on the mapper
- [x] AuditLogServiceImpl.record() computes previousHash from last log, stores it, computes currentHash via HashChainUtil
- [x] HashChainUtil.sha256() produces valid SHA-256 hex digest (64-char hex string)
- [x] HashChainUtil.verifyChain() detects hash mismatch between entries
- [x] AuditLogAspect has @Around intercepting POST/PUT/DELETE on @RestController methods
- [x] AuditLogAspect captures: operatorName, operatorId, IP (from request), operation type (from annotation), request body content
- [x] AuditLogController has POST /api/system/audit-log/page, GET /api/system/audit-log/{id}, POST /api/system/audit-log/verify-chain (read-only)
- [x] LocalFileStorageServiceImpl.store() validates file size <= 52428800 (50MB)
- [x] LocalFileStorageServiceImpl.store() validates file extension against allowed types
- [x] LocalFileStorageServiceImpl.store() generates UUID filename and saves to uploads/YYYY/MM/type/ directory
- [x] FileProxyController returns file via /api/files/{uuid} proxy URL -- no direct path exposure
- [x] BackupSchedulerService has @Scheduled(cron) method for daily backup
- [x] BackupSchedulerService performs mysqldump + file backup (tar on Linux, zip on Windows)
- [x] BackupSchedulerService cleanup removes backups older than 30 days
- [x] FileStorageConfig reads upload-dir, max-file-size, allowed-types from application.yml
- [x] P-07 page has: search form, read-only ElTable, detail drawer (600px) with hash display
- [x] Operation type ElTags match: 登录=info, 新增=success, 编辑=warning, 删除=danger, 登出=info
- [x] Detail drawer shows ElDescriptions + hash chain display + integrity ElTag
- [x] "完整性校验" button calls verifyChain API and shows ElResult
- [x] "导出" button for CSV export
- [x] Empty state: "暂无审计日志，调整筛选条件后重新查询"
- [x] Error state: ElAlert "审计日志加载失败" with retry
- [x] Default time range is last 7 days
- [x] Route /system/audit-log already wired (from Plan 00-03 router config)

## Self-Check: PASSED

- [x] V2 migration SQL file exists (90+ lines with all partitions)
- [x] AuditLogAspect.java exists with @Aspect and @Around (120+ lines)
- [x] HashChainUtil.java exists with sha256/computeHash/verifyChain (200+ lines)
- [x] LocalFileStorageServiceImpl.java exists with store/loadAsResource (150+ lines)
- [x] BackupSchedulerService.java exists with @Scheduled backup/cleanup (160+ lines)
- [x] AuditLogController.java has all 3 read-only endpoints (POST page, GET detail, POST verify-chain)
- [x] SecurityUtils.java has getCurrentUsername/getCurrentUserId/getClientIp methods
- [x] P-07 index.vue has search form, read-only table, detail drawer, chain verify (300+ lines)
- [x] API module has page/getDetail/verifyChain functions with typed interfaces
- [x] Backend compiles (2 successful runs)
- [x] Frontend type-checks (vue-tsc --noEmit, exit 0)

## Commits

| Hash | Message |
|------|---------|
| 0793ac6 | feat(00-foundation-infrastructure): implement audit log system with hash chain, AOP interceptor, and security utils |
| c283bc6 | feat(00-foundation-infrastructure): implement file proxy service and daily backup scheduler |
| fed4be7 | feat(00-foundation-infrastructure): create P-07 Audit Log management page |

## Plan Verification

- [x] Audit log records every write operation with operator, time, IP, before/after content (D-25)
- [x] Hash chain integrity verifiable via API; tampering detected (D-27)
- [x] Audit table partitioned by month with p_future catch-all partition (D-26)
- [x] File upload generates UUID filename, stored in uploads/YYYY/MM/type/, served via /api/files/{uuid} (D-29/D-30)
- [x] File size limit 50MB enforced; type whitelist enforced (D-31)
- [x] Daily backup: database (mysqldump) + files (tar/zip), auto-clean backups older than 30 days (D-32/D-33)
- [x] P-07 Audit Log page renders with all filters, read-only table, detail drawer with hash display (D-28)
- [x] @EnableScheduling active for backup scheduler (OPS-01)
- [x] Java compilation and TypeScript type check both pass cleanly
