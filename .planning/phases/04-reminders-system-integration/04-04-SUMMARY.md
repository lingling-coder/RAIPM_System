---
phase: 04-reminders-system-integration
plan: 04
type: execute
wave: 2
status: complete
requirements: [API-02]
metrics:
  duration_minutes: 30
  completed: 2026-06-17
  tasks: 3
  files_created: 21
  files_modified: 3
key_decisions:
  - Use sys_config table (new) for SMTP settings instead of api_config table
  - Use Hutool AES for password encryption (no existing AES utility in project)
  - Use Thymeleaf SpringTemplateEngine for HTML email rendering
  - Email templates placed in achievement-reminder module (not shared resources module)
  - Dedicated EmailRetryConfig for Resilience4j with longer backoff (minutes vs seconds)
---

# Phase 4 Plan 4: SMTP Email Integration Summary

**One-liner:** Dynamic SMTP email infrastructure with AES-256 encrypted passwords, dedicated async thread pool, Resilience4j retry (3 attempts with exponential backoff), 6 Thymeleaf HTML email templates, and admin SMTP configuration UI with test-send feature.

## Completed Tasks

### Task 1: Email Async Config, Service, DTO, and Controller

**Files created:**
- `achievement-module/achievement-reminder/pom.xml` (modified) -- Added spring-boot-starter-mail, spring-boot-starter-thymeleaf, resilience4j-spring-boot3
- `achievement-common/src/main/java/com/institute/achievement/common/util/EncryptUtil.java` -- AES-256 encryption utility using Hutool
- `achievement-module/achievement-reminder/src/main/resources/db/migration/V17__create_sys_config_for_smtp.sql` -- sys_config table + SMTP defaults
- `achievement-module/achievement-reminder/src/main/java/com/institute/achievement/reminder/config/EmailAsyncConfig.java` -- Dedicated bounded thread pool (4/8/200, CallerRunsPolicy)
- `achievement-module/achievement-reminder/src/main/java/com/institute/achievement/reminder/config/EmailRetryConfig.java` -- Resilience4j retry config (3 attempts, 1min/5min/15min)
- `achievement-module/achievement-reminder/src/main/java/com/institute/achievement/reminder/dto/EmailConfigDTO.java` -- SMTP configuration DTO
- `achievement-module/achievement-reminder/src/main/java/com/institute/achievement/reminder/entity/SysConfig.java` -- sys_config entity
- `achievement-module/achievement-reminder/src/main/java/com/institute/achievement/reminder/mapper/SysConfigMapper.java` -- Key-value config mapper
- `achievement-module/achievement-reminder/src/main/java/com/institute/achievement/reminder/service/EmailService.java` -- Email service interface
- `achievement-module/achievement-reminder/src/main/java/com/institute/achievement/reminder/service/impl/EmailServiceImpl.java` -- Core implementation
- `achievement-module/achievement-reminder/src/main/java/com/institute/achievement/reminder/controller/EmailConfigController.java` -- REST API endpoints

### Task 2: 6 Thymeleaf Email Templates

**Files created (all in `achievement-module/achievement-reminder/src/main/resources/templates/mail/`):**
- `reminder-ProjectApplication.html`
- `reminder-AwardApplication.html`
- `reminder-PatentFee.html`
- `reminder-CopyrightMaintenance.html`
- `reminder-TransformationEval.html`
- `reminder-ClassifiedAudit.html`

All templates share identical table-based layout with inline CSS, `th:text` variable substitution (no `th:utext`), and Chinese font support.

### Task 3: SMTP Config Frontend Page

**Files created:**
- `achievement-web/src/api/reminder/email-config.ts` -- API functions (getConfig, saveConfig, testConnection)
- `achievement-web/src/views/system/email-config/index.vue` -- SMTP config page with form + test send

**Files modified:**
- `achievement-web/src/router/index.ts` -- Added `/system/email-config` route
- `achievement-web/src/layout/index.vue` -- Added menu item in systemMenuItemsFull

## Deviations from Plan

### [Rule 2 - Missing Critical Functionality] AES encryption utility

**Found during:** Task 1
**Issue:** The plan references "project's existing AES utility in achievement-common" but no such utility exists in the codebase. SMTP password must be AES-256 encrypted at rest per T-4-03.
**Fix:** Created `EncryptUtil.java` in `achievement-common/src/main/java/com/institute/achievement/common/util/` using Hutool's AES implementation (GCM mode, PKCS5Padding).
**Files modified:** `achievement-common/src/main/java/com/institute/achievement/common/util/EncryptUtil.java`

### [Rule 2 - Missing Critical Functionality] Resilience4j retry configuration

**Found during:** Task 1
**Issue:** The plan specifies `@Retry(name = "emailSend")` with 1min/5min/15min backoff, but the existing Resilience4jConfig has 1s/2s/4s default. Email needs much longer intervals.
**Fix:** Created dedicated `EmailRetryConfig.java` with RetryRegistry containing the email-specific retry configuration.
**Files modified:** `achievement-module/achievement-reminder/src/main/java/com/institute/achievement/reminder/config/EmailRetryConfig.java`

### [Path safety] Files written to correct worktree path

**Found during:** Commit phase
**Issue:** Initial file writes went to main repo path (`D:\科研成果与知识产权管理系统\...`) instead of worktree path (`D:\科研成果与知识产权管理系统\.claude\worktrees\agent-a96acf076cc113283\...`).
**Fix:** All files re-created at correct worktree path. Originals cleaned from main repo.
**Note:** This is a known worktree path safety issue (#3099) -- absolute paths must be derived from `git rev-parse --show-toplevel` inside the worktree.

### [Git blocked] Could not commit per-task

Git commands are blocked by the Claude Code sandbox. All files are written to the worktree working directory and staged via `git add`. Per-task commits need to be performed by the orchestrator or the next agent with git access.

## Auth Gates

None encountered.

## Known Stubs

None.

## Threat Flags

None -- all files are within declared scope.

## Threat Surface Scan

All new endpoints are under `ROLE_SYSTEM_ADMIN` protection (via `@PreAuthorize("hasRole('ROLE_SYSTEM_ADMIN')")`). SMTP password encryption follows T-4-03. All email templates use `th:text` (no `th:utext`) per T-4-06. Thread pool is bounded per T-4-05.

## Self-Check

**Task 1 verification:**
- [x] EmailAsyncConfig defines "emailTaskExecutor" bean (4 occurrences)
- [x] EmailServiceImpl uses JavaMailSenderImpl (13 occurrences)
- [x] EmailServiceImpl has sendEmail method (3 occurrences)
- [x] EmailServiceImpl has testConnection method (1 occurrence)
- [x] EmailServiceImpl has refreshConfig method (3 occurrences)
- [x] SysConfigMapper defines getValueByKey method
- [x] EncryptUtil defines encrypt/decrypt methods (15 occurrences)
- [x] V17 migration contains SMTP config entries

**Task 2 verification:**
- [x] 6 template files exist in templates/mail/ directory
- [x] All use xmlns:th namespace
- [x] All have th:text for variable substitution

**Task 3 verification:**
- [x] Router has email-config route (2 occurrences)
- [x] Layout has email-config menu item (1 occurrence)
- [x] API file exports testConnection function
- [x] View page has 发送测试邮件 UI text
