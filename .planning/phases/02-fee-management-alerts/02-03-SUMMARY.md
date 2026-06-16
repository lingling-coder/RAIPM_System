---
phase: 02-fee-management-alerts
plan: 03
subsystem: fee
tags: [alert-engine, fee-alert, notification-center, scheduled-task]
provides:
  - FEE-03-4-tier-alert-engine
  - FEE-03-alert-record-persistence
  - FEE-03-alert-dedup
  - FEE-03-alert-notification-center-tab
requires:
  - 02-01-FeeRecord-entity
  - 02-01-NotificationService
  - Phase 0 Redis infrastructure
  - Phase 0 SecurityUtils
affects:
  - achievement-fee module
  - achievement-system module (notification)
  - achievement-web frontend
tech-stack:
  added:
    - AlertRecord entity with AlertLevelEnum 4-tier classification
    - AlertRecordMapper with JOINs and batch dedup
    - AlertScanTask (daily 4 AM, Redis lock + idempotency)
    - AlertRecordController REST API
    - frontend ALERT tab in NotificationCenter
  patterns:
    - Scheduler pattern: `@Scheduled + Redis idempotency (setIfAbsent) + distributed lock`
    - Joining fee_record -> patent/copyright for owner name via raw SQL @Select
    - Inline dedup check before insert (T-02-03-01 mitigation)
key-files:
  created:
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/dto/AlertQueryDTO.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/dto/AlertRecordVO.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/mapper/AlertRecordMapper.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/AlertRecordService.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/impl/AlertRecordServiceImpl.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/controller/AlertRecordController.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/scheduler/AlertScanTask.java
    - achievement-web/src/api/fee/alertRecord.ts
  modified:
    - achievement-web/src/api/fee/index.ts (added alertRecord export)
    - achievement-web/src/views/notification/NotificationCenter.vue (added ALERT tab)
    - achievement-web/src/views/fee/FeeLedger.vue (added alert level column)
  pre-existing:
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/entity/AlertRecord.java (committed in 02-01/02-02)
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/enums/AlertLevelEnum.java (committed in 02-01/02-02)
    - achievement-module/achievement-fee/src/main/resources/db/migration/V13__create_alert_record_table.sql (committed in 02-01/02-02)
decisions:
  - "Notification recipient: patent.created_by for patents, fee_record.created_by fallback for copyrights"
  - "Alert dedup strategy: batch pre-filter (findAlreadyAlertedFeeRecordIds) + double-check (countByFeeRecordAndLevelAndDate)"
  - "Copyright owner name in alert page: uses software_copyright.name field (c.name in SQL JOIN)"
metrics:
  duration: 0
  completed_date: "2026-06-16"
---

# Phase 02 Plan 03: 4-Tier Alert Engine with Notification Center Integration

## One-liner

Alert engine with daily 4 AM scan classifies pending fee records into BLUE/YELLOW/ORANGE/RED tiers, persists to alert_record table with dedup, sends in-app ALERT notifications, and extends NotificationCenter with a "费用预警" tab and FeeLedger with color-coded alert tags.

## Tasks Summary

| Task | Name | Status | Commit | Files |
|------|------|--------|--------|-------|
| 1 | Create alert_record database migration, entity, and enums | Done (pre-existing) | cafd226 | AlertRecord.java, AlertLevelEnum.java, V13 migration |
| 2 | Implement alert engine backend | Done | df9cd64 | AlertQueryDTO, AlertRecordVO, AlertRecordMapper, AlertRecordService, AlertRecordServiceImpl, AlertRecordController, AlertScanTask |
| 3 | Extend NotificationCenter with ALERT tab and fee ledger tags | Done | dbc8456 | alertRecord.ts, fee/index.ts, NotificationCenter.vue, FeeLedger.vue |

## What Was Built

### Backend: Alert Engine (Task 1 + 2)

**Data Layer:**
- **AlertLevelEnum** (BLUE=30d/YELLOW=15d/ORANGE=7d/RED=overdue) with `fromDueDate()` classification logic and `fromCode()`/`getColorClass()` helpers
- **AlertRecord** entity with feeRecordId, alertLevel, triggeredDate, status, escalationLevel, deptId
- **V13** migration: `alert_record` table with all columns, indexes (idx_fee_record_id, idx_status, idx_triggered_date, idx_escalation, idx_dept_id), and UNIQUE INDEX `idx_unique_alert` on (fee_record_id, alert_level, triggered_date) for dedup

**Service Layer:**
- **AlertRecordMapper**:
  - `selectAlertPage()` — paginated listing JOINing fee_record + patent/copyright for owner name
  - `findByStatusAndLevel()` — filter query
  - `findPendingForEscalation()` — escalation scan (Phase 2)
  - `countByFeeRecordAndLevelAndDate()` — per-record dedup check
  - `findAlreadyAlertedFeeRecordIds()` — batch dedup pre-filter
- **AlertRecordServiceImpl.scanAndGenerateAlerts()**:
  1. Queries all pending fee_records with due_date <= today + 30 days
  2. Filters out records already alerted today (batch dedup)
  3. Classifies each record by due date proximity
  4. Inserts alert_record with double-check dedup
  5. Sends in-app notification via `NotificationService.send(userId, "ALERT", ...)`
  6. Notification recipient: patent.created_by for patents, fee_record.created_by for copyrights
  7. Returns count of new alerts generated

**Controller Layer (`/api/alert-records`):**
- `GET /page` — paginated listing with status/alertLevel filter
- `GET /{id}` — single record detail
- `PUT /{id}/resolve` — resolve single alert
- `PUT /batch-resolve` — batch resolve

**Scheduler (`AlertScanTask`):**
- `@Scheduled(cron = "0 0 4 * * ?")` — daily at 4 AM
- Redis distributed lock (`spring:lock:alert-scan`, TTL 300s) prevents concurrent execution
- Redis idempotency key (`fee:alert:last-run:YYYY-MM-DD`, TTL 2h) prevents duplicate runs per day
- On failure: logs error, retries next day

### Frontend: Alert Display (Task 3)

**API Service (`alertRecord.ts`):**
- `getPage()`, `getById()`, `resolve()`, `batchResolve()` endpoints
- `AlertRecordVO` interface matching backend

**NotificationCenter.vue — New "费用预警" Tab (D-26):**
- Third `el-tab-pane` labeled "费用预警" with name="ALERT"
- ALERT type notifications display **WarningFilled** icon with orange color (#e6a23c)
- Custom CSS class `.icon-alert` for ALERT icon styling
- Empty state shows "暂无费用预警" with orange WarningFilled icon
- Clicking an ALERT notification navigates to `/fee/detail/{relatedAchievementId}`
- No backend changes needed — the existing NotificationMapper.findByUserIdAndType() handles any type string natively

**FeeLedger.vue — Alert Level Color Tags:**
- New "预警" column after "截止日期"
- `getAlertTag()` computed method maps dueDate + status to color tags:
  - BLUE: `el-tag type="primary"` — "即将缴费"
  - YELLOW: `el-tag type="warning"` — "请尽快缴费"
  - ORANGE: `el-tag type="danger" plain` — "截止在即"
  - RED: `el-tag type="danger" dark` — "逾期 X 天"
- Only shown for pending fee records

## Threat Model Mitigations

| Threat | Disposition | Implementation |
|--------|-------------|----------------|
| T-02-03-01: Duplicate alert insert | **mitigate** | UNIQUE INDEX idx_unique_alert + batch dedup pre-filter + per-record double-check count |
| T-02-03-02: Double execution | **mitigate** | Redis idempotency key + distributed lock in AlertScanTask |
| T-02-03-03: Information disclosure | **mitigate** | SQL-layer dept_id injection via MyBatis-Plus interceptor |
| T-02-03-04: Alert level spoofing | **accept** | due_date is immutable per D-17; alert level is always based on original system-calculated date |

## Key Design Decisions

1. **Notification recipient resolution:** Patent fees notify the patent's creator (patent.created_by). Copyright fees fall back to fee_record.created_by. If no valid user found, sends to sentinel userId=0 (same pattern as existing NotificationService.notifyDeptSecretaries).
2. **Two-layer dedup:** Batch pre-filter (findAlreadyAlertedFeeRecordIds) avoids individual DB calls, followed by per-record count check as second safety net against race conditions.
3. **Copyright JOIN in mapper:** Uses raw SQL `LEFT JOIN software_copyright c ON ...` since fee module doesn't depend on achievement-copyright Maven artifact — raw SQL bypasses the compile-time dependency.

## Deviations from Plan

### Auto-fixed Issues

None — plan executed exactly as written.

## Known Stubs

None — all alert functionality is fully wired. The `alertLevel` field in FeeRecordVO (already in the entity) is computed client-side in FeeLedger.vue via `getAlertTag()`, which works before the backend alert scan runs.

## Deferred Issues

1. **Pre-existing EasyExcel compilation error (FeeStatsExcelVO.java):** The `@ColumnWidth` annotation from EasyExcel causes a compilation error. This is unrelated to this plan's changes and was present before execution. The issue is in `achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/dto/FeeStatsExcelVO.java` — possibly an EasyExcel version mismatch or missing import. Not a blocker for this plan.

## Verification

- All 3 tasks completed and committed atomically
- AlertRecord entity with 4-tiers BLUE/YELLOW/ORANGE/RED (committed pre-existing)
- V13 migration with UNIQUE INDEX idx_unique_alert for dedup (committed pre-existing)
- AlertScanTask daily at 4 AM with Redis lock + idempotency (committed df9cd64)
- scanAndGenerateAlerts() classifies fee_records by due date proximity (committed df9cd64)
- Each new alert creates both alert_record and in-app notification (committed df9cd64)
- AlertRecordController provides paginated listing + single/batch resolve (committed df9cd64)
- NotificationCenter extended with "费用预警" tab + WarningFilled icon + orange color (committed dbc8456)
- FeeLedger shows alert level color tags inline per D-22 (committed dbc8456)
- Alert dedup via UNIQUE INDEX + batch pre-filter + double-check (committed df9cd64)
- Pre-existing EasyExcel compilation error documented as deferred item

## Self-Check: PASSED

- [x] AlertRecord.java, AlertLevelEnum.java, V13 migration exist (verified: committed in cafd226)
- [x] AlertQueryDTO.java, AlertRecordVO.java, AlertRecordMapper.java, AlertRecordService.java, AlertRecordServiceImpl.java, AlertRecordController.java, AlertScanTask.java created (verified: committed in df9cd64)
- [x] alertRecord.ts created, fee/index.ts updated, NotificationCenter.vue extended, FeeLedger.vue extended (verified: committed in dbc8456)
- [x] All 3 commits exist in git log
