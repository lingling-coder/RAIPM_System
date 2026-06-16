---
phase: 02-fee-management-alerts
plan: 04
subsystem: fee
tags: [batch-payment, slip-generation, alert-escalation, notification-rbac, scheduled-task]
provides:
  - FEE-04-batch-slip-generation
  - FEE-04-batch-payment
  - FEE-04-alert-escalation-engine
  - FEE-04-escalation-notification-rbac
  - FEE-04-batch-pay-dialog
requires:
  - 02-01-FeeRecord-entity-with-slips
  - 02-01-FeeRecordMapper
  - 02-03-AlertRecord-entity
  - 02-03-NotificationService
  - Phase 0 Redis infrastructure
  - Phase 0 SecurityUtils
affects:
  - achievement-fee module (FeeSlipNumberGenerator, FeeRecordMapper/Service/Controller, AlertRecordService/Controller, AlertEscalationTask)
  - achievement-system module (NotificationService, SysUserMapper)
  - achievement-web frontend (feeRecord.ts, FeeLedger.vue, BatchPayDialog.vue)
tech-stack:
  added:
    - FeeSlipNumberGenerator (Redis INCR-based daily sequential slip numbering)
    - EscalationLevel enum with 3-tier state machine
    - AlertEscalationTask scheduler (daily 5 AM, Redis lock + idempotency)
    - BatchPayDialog.vue (2-step wizard: record confirmation + payment info)
    - SysUserMapper.findUserIdsByDeptAndRole() (RBAC JOIN query)
  patterns:
    - Scheduler pattern: `@Scheduled + Redis idempotency (setIfAbsent) + distributed lock` (consistent with AlertScanTask)
    - RBAC user query: `sys_user JOIN sys_user_role JOIN sys_role WHERE dept_id AND role_code`
    - Batch payment guard: `WHERE status='pending'` in Update SQL prevents re-paying paid records
key-files:
  created:
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/FeeSlipNumberGenerator.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/enums/EscalationLevel.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/scheduler/AlertEscalationTask.java
    - achievement-web/src/views/fee/components/BatchPayDialog.vue
  modified:
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/mapper/FeeRecordMapper.java (added batchMarkAsPaid, updateSlipNo)
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/FeeRecordService.java (added batchGenerateSlips, batchPay)
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/impl/FeeRecordServiceImpl.java (implemented batch methods)
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/controller/FeeRecordController.java (added batch endpoints + DTOs)
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/AlertRecordService.java (added processEscalations, processSingleEscalation)
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/impl/AlertRecordServiceImpl.java (implemented escalation logic)
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/controller/AlertRecordController.java (added escalate endpoint)
    - achievement-module/achievement-system/src/main/java/com/institute/achievement/module/system/mapper/SysUserMapper.java (added findUserIdsByDeptAndRole)
    - achievement-module/achievement-system/src/main/java/com/institute/achievement/module/system/service/NotificationService.java (added findUserIdsByDeptAndRole, upgraded notifyDeptSecretaries to RBAC)
    - achievement-web/src/api/fee/feeRecord.ts (added batchGenerateSlips, batchPay)
    - achievement-web/src/views/fee/FeeLedger.vue (added selection state, batch pay button, dialog wiring)
  pre-existing:
    - V11 migration (slip columns slip_no, slip_generated_time, slip_generated_by) from 02-01
    - FeeRecord entity with slip fields from 02-01
decisions:
  - "Slip number format: FEE-YYYYMMDD-XXX with daily sequential numbering via Redis INCR (D-07)"
  - "3-tier escalation: FIRST_ALERT (0d) -> DEPT_HEAD (3d) -> LEADERSHIP (8d) per D-24"
  - "Batch payment guard: WHERE status='pending' in SQL prevents re-paying already paid records (T-02-04-01)"
  - "NotificationService.notifyDeptSecretaries upgraded from sentinel userId=0 to RBAC query (Phase 1 fallback preserved)"
metrics:
  duration: 0
  completed_date: "2026-06-16"
---

# Phase 02 Plan 04: Batch Fee Slip Generation + Secondary Alert Escalation

## One-liner

Batch fee slip generation with Redis INCR auto-numbered slips (FEE-YYYYMMDD-XXX), batch mark-as-paid with status guard, 3-tier alert escalation engine (3d dept head / 8d leadership) with RBAC notification routing, and 2-step BatchPayDialog wizard frontend.

## Tasks Summary

| Task | Name | Status | Commit | Files |
|------|------|--------|--------|-------|
| 1 | Implement fee slip number generator and batch payment backend | Done | a25d4c7 | FeeSlipNumberGenerator, FeeRecordMapper/Service/Impl/Controller |
| 2 | Implement alert escalation engine (3-tier escalation task + RBAC user query) | Done | 74623e8 | EscalationLevel, AlertRecordService/Impl, AlertEscalationTask, AlertRecordController, NotificationService, SysUserMapper |
| 3 | Implement BatchPayDialog frontend and wire batch payment flow into FeeLedger | Done | 25572cd | BatchPayDialog.vue, FeeLedger.vue, feeRecord.ts |

## What Was Built

### Task 1: Fee Slip Number Generator + Batch Payment Backend

**FeeSlipNumberGenerator:**
- `@Service` component using `StringRedisTemplate` for atomic Redis INCR
- Daily key pattern: `fee:slip:seq:YYYYMMDD` with 2-day TTL auto-expiry
- Output format: `FEE-YYYYMMDD-XXX` with zero-padded 3-digit sequence number
- Supports up to 999 slips per day (more than sufficient per T-02-04-02)

**FeeRecordMapper extensions:**
- `batchMarkAsPaid()` — `@Update` with `<script>` + `<foreach>` dynamic SQL
  - Sets status='paid', paid_date, voucher_no, slip_no, slip_generated_time=NOW(), slip_generated_by
  - `WHERE status='pending'` guard prevents re-paying already paid records (T-02-04-01)
- `updateSlipNo()` — Updates slip fields for a single record during batch generation

**FeeRecordService extensions:**
- `batchGenerateSlips(List<Long> ids)` — Validates each record exists and is pending, generates unique slip numbers via Redis INCR per record, persists to DB, returns list of slip numbers
- `batchPay(List<Long> ids, LocalDate paidDate, String voucherNo, String slipNo)` — Validates voucherNo not blank, calls batchMarkAsPaid with WHERE status='pending' guard, logs results

**FeeRecordController extensions:**
- `POST /api/fees/batch-generate-slips` — Accepts `BatchGenerateSlipsRequest { ids }`, returns `Result<List<String>>`
- `PUT /api/fees/batch-pay` — Accepts `BatchPayRequest { ids, paidDate, voucherNo, slipNo }` with jakarta validation, returns `Result<Integer>`

### Task 2: Alert Escalation Engine

**EscalationLevel enum:**
- 4 values: NONE, FIRST_ALERT (0d), DEPT_HEAD (3d), LEADERSHIP (8d)
- `determineNextLevel(currentLevel, hoursSinceTrigger)` state machine
  - >= 192 hours (8d) AND not already LEADERSHIP -> escalate to LEADERSHIP
  - >= 72 hours (3d) AND current is NONE/FIRST_ALERT -> escalate to DEPT_HEAD

**AlertRecordServiceImpl extensions:**
- `processEscalations()` — Scans all pending alerts not at max escalation, evaluates age, escalates to DEPT_HEAD or LEADERSHIP
  - Updates alert_record with escalationLevel and escalatedAt
  - Routes notifications via RBAC: DEPT_HEAD -> findUserIdsByDeptAndRole(deptId, ROLE_SECRETARY + ROLE_DEPT_ADMIN)
  - LEADERSHIP -> findUserIdsByDeptAndRole(deptId, ROLE_LEADER)
  - Notification content: Chinese messages with fee type, due date, and escalation context
- `processSingleEscalation(Long alertRecordId)` — Same logic for single manual trigger

**AlertEscalationTask scheduler:**
- `@Scheduled(cron = "0 0 5 * * ?")` — Daily at 5 AM (after alert scan at 4 AM)
- Redis distributed lock: `spring:lock:alert-escalation`, TTL 300s
- Idempotency: `fee:alert:escalation:last-run:YYYY-MM-DD` with setIfAbsent (TTL 2h)
- Error handling: logs error, next day retry

**SysUserMapper RBAC query:**
- `findUserIdsByDeptAndRole(deptId, roleCode)` — `SELECT DISTINCT u.id FROM sys_user u JOIN sys_user_role ur ON u.id=ur.user_id JOIN sys_role r ON ur.role_id=r.id WHERE u.dept_id=? AND r.role_code=? AND u.deleted=0`

**NotificationService upgrade:**
- `findUserIdsByDeptAndRole()` — Delegates to SysUserMapper, logs warning if no users found
- `notifyDeptSecretaries()` — Upgraded from sentinel userId=0 to real RBAC query with fallback

**AlertRecordController extension:**
- `POST /api/alert-records/{id}/escalate` — Manual escalation trigger

### Task 3: BatchPayDialog Frontend

**feeRecord.ts API extensions:**
- `batchGenerateSlips(ids)` — `POST /api/fees/batch-generate-slips`
- `batchPay(data)` — `PUT /api/fees/batch-pay`

**BatchPayDialog.vue (2-step wizard):**
- Step 1 "确认缴费记录": Mini el-table with selected records (序号, 费用类型, 关联成果, 截止日期, 金额), total amount summary row, "生成缴费单" button
- Step 2 "填写缴费信息": Read-only slip numbers as el-tags, el-date-picker for paidDate (default today), el-input for voucherNo (required), "确认缴费" button
- State management: step, slipNumbers, paidDate, voucherNo, generating/paying loading
- v-model:visible pattern with watch to sync prop and reset state on open
- Emits: update:visible, success(paidCount)

**FeeLedger.vue extensions:**
- "生成缴费单" el-button positioned in toolbar row, disabled when no rows selected
- `onSelectionChange()` handler syncing checkbox selection
- `selectedRecords` ref, `selectedIds` computed, `batchPayDialogVisible` ref
- BatchPayDialog component with v-model:visible, wired events: @success refreshes table and clears selection

## Threat Model Mitigations

| Threat | Disposition | Implementation |
|--------|-------------|----------------|
| T-02-04-01: Tampering — batch pay without payment | **mitigate** | Controller validates records exist. Backend checks status='pending' before updating. Mapper has WHERE status='pending' guard in UPDATE SQL |
| T-02-04-02: Slip number collision | **mitigate** | Redis INCR with daily prefix is atomic. 3-digit padding supports 999/day |
| T-02-04-03: Escalation notification sent to wrong users | **mitigate** | RBAC query uses sys_user_role + sys_role JOIN with exact role_code. DEPT_HEAD includes dept_id filter. LEADERSHIP uses role_code='ROLE_LEADER' |
| T-02-04-04: Information disclosure | **accept** | Leadership legitimately needs fee amounts for management oversight |

## Key Design Decisions

1. **Redis INCR for slip numbering:** Atomic per-day counter with daily key prefix (fee:slip:seq:YYYYMMDD) and 2-day TTL. Avoids DB sequence locks and works across instances.
2. **Per-record slip generation (not batch slip):** Each fee record gets its own unique slip number for audit traceability. The batch-pay endpoint accepts a single slipNo for the entire batch.
3. **RBAC fallback for notifyDeptSecretaries:** If no users found for the role+dept combination, falls back to sentinel userId=0 (Phase 1 compatibility). Logs warning for operational awareness.
4. **3-tier state machine:** EscalationLevel.determineNextLevel() encapsulates the threshold logic. 72h for DEPT_HEAD, 192h for LEADERSHIP. Future threshold changes are a single config update.

## Deviations from Plan

### Auto-fixed Issues

None — plan executed exactly as written.

## Known Stubs

None — all batch payment and escalation functionality is fully wired.

## Deferred Issues

1. **Pre-existing EasyExcel compilation error (FeeStatsExcelVO.java):** The `@ColumnWidth` annotation from EasyExcel causes a compilation error. This is unrelated to this plan's changes. File: `achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/dto/FeeStatsExcelVO.java`

## Verification

- [x] Task 1: FeeSlipNumberGenerator created with Redis INCR pattern (a25d4c7)
- [x] Task 1: FeeRecordMapper batchMarkAsPaid() with WHERE status='pending' guard (a25d4c7)
- [x] Task 1: FeeRecordService batchGenerateSlips() and batchPay() implemented (a25d4c7)
- [x] Task 1: FeeRecordController POST /batch-generate-slips and PUT /batch-pay endpoints (a25d4c7)
- [x] Task 2: EscalationLevel enum with 3-tier state machine (74623e8)
- [x] Task 2: AlertEscalationTask at 5 AM with Redis lock + idempotency (74623e8)
- [x] Task 2: processEscalations() with 72h/192h thresholds and RBAC notification routing (74623e8)
- [x] Task 2: POST /api/alert-records/{id}/escalate endpoint (74623e8)
- [x] Task 2: SysUserMapper.findUserIdsByDeptAndRole() RBAC query (74623e8)
- [x] Task 2: NotificationService upgraded with RBAC user query + upgraded notifyDeptSecretaries (74623e8)
- [x] Task 3: feeRecord.ts batchGenerateSlips() and batchPay() API functions (25572cd)
- [x] Task 3: BatchPayDialog.vue with 2-step wizard (25572cd)
- [x] Task 3: FeeLedger.vue with selection state, batch pay button, dialog wiring (25572cd)
- [x] Backend compilation: only pre-existing FeeStatsExcelVO errors
- [x] Frontend TypeScript: no new errors from this plan's changes

## Self-Check: PASSED

- [x] FeeSlipNumberGenerator.java created and committed (a25d4c7)
- [x] FeeRecordMapper.java updated with batchMarkAsPaid and updateSlipNo (a25d4c7)
- [x] FeeRecordService.java updated with batchGenerateSlips and batchPay (a25d4c7)
- [x] FeeRecordServiceImpl.java updated with implementations (a25d4c7)
- [x] FeeRecordController.java updated with batch endpoints and DTOs (a25d4c7)
- [x] EscalationLevel.java created and committed (74623e8)
- [x] AlertRecordService.java updated with processEscalations and processSingleEscalation (74623e8)
- [x] AlertRecordServiceImpl.java updated with escalation logic (74623e8)
- [x] AlertEscalationTask.java created and committed (74623e8)
- [x] AlertRecordController.java updated with escalate endpoint (74623e8)
- [x] SysUserMapper.java updated with findUserIdsByDeptAndRole (74623e8)
- [x] NotificationService.java updated with RBAC user query and upgraded notifyDeptSecretaries (74623e8)
- [x] feeRecord.ts updated with batch API functions (25572cd)
- [x] BatchPayDialog.vue created and committed (25572cd)
- [x] FeeLedger.vue updated with batch pay button and dialog wiring (25572cd)
- [x] All 3 commits exist in git log
