---
phase: 02-fee-management-alerts
plan: 01
type: execute
wave: 1
subsystem: fee
tags: [fee-ledger, module-structure, crud, pagination, frontend]
dependency_graph:
  requires: [Phase 1 plans (Patent/Copyright entities, NotificationService), Phase 0 (SecurityUtils, Result, MyBatis-Plus, Flyway)]
  provides: [FeeRecord entity, fee_record table, FeeRecord CRUD, /fee frontend route, FeeLedger, FeeDetail]
  affects: [02-02 (fee plan), 02-03 (alert engine), 02-04 (batch slip), 02-05 (fee stats)]
tech-stack:
  added: [MyBatis-Plus @Select JOIN query with <script> tag, Arc polymorphic pattern for fee<->achievement]
  patterns: [@EventListener for domain events across modules, Controller → Service → Mapper with UpdateWrapper for whitelisted field updates]
key-files:
  created:
    - achievement-module/achievement-fee/pom.xml
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/entity/FeeRecord.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/enums/FeeTypeEnum.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/enums/FeeStatusEnum.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/dto/FeeRecordDTO.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/dto/FeeRecordVO.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/dto/FeeRecordQueryDTO.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/mapper/FeeRecordMapper.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/FeeRecordService.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/impl/FeeRecordServiceImpl.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/controller/FeeRecordController.java
    - achievement-module/achievement-fee/src/main/resources/db/migration/V11__create_fee_record_table.sql
    - achievement-common/src/main/java/com/institute/achievement/common/event/AchievementArchivedEvent.java
    - achievement-web/src/router/index.ts (modified)
    - achievement-web/src/api/fee/feeRecord.ts
    - achievement-web/src/api/fee/index.ts
    - achievement-web/src/views/fee/FeeManagement.vue
    - achievement-web/src/views/fee/FeeLedger.vue
    - achievement-web/src/views/fee/FeeDetail.vue
    - achievement-web/src/views/fee/FeePlan.vue
    - achievement-web/src/views/fee/FeeStats.vue
  modified:
    - pom.xml (parent — added achievement-fee module + dependency)
    - achievement-module/pom.xml (module aggregator — added achievement-fee)
  deleted: []
decisions:
  - "AchievementArchivedEvent carries nextFeeDate field so fee module can auto-generate first fee without compile dependency on patent module"
  - "FeeMapper uses @Select with <script> XML-style annotations for JOIN query (no XML mapper files exist in project)"
  - "UpdateWrapper for whitelisted field updates (per Phase 01-03 decision, avoids LambdaUpdateWrapper serialization issues)"
metrics:
  duration: "~45 min"
  completed_date: "2026-06-16"
---

# Phase 2 Plan 01: Fee Ledger Foundation — Module Structure + Fee Record CRUD

Created the `achievement-fee` sub-module with complete backend CRUD, paginated filtered listing with JOIN-based owner name resolution, and the frontend fee ledger views under the /fee route. This is the foundation for all subsequent Phase 2 plans (fee plan engine, alert engine, batch slip, fee statistics).

## Tasks Completed

### Task 1: Create achievement-fee module structure with fee_record database migration

- Added `<module>achievement-fee</module>` to `achievement-module/pom.xml` and parent `pom.xml`
- Created `achievement-fee/pom.xml` following the patent module pattern
- Created `FeeTypeEnum` (ANNUAL_FEE, REGISTRATION_FEE, MAINTENANCE_FEE, OTHER) and `FeeStatusEnum` (PENDING, PAID, PAUSED)
- Created `FeeRecord` entity with all D-13 fields including Arc polymorphic owner_type/owner_id
- Created `V11__create_fee_record_table.sql` migration with all columns, 5 indexes (idx_owner, idx_status, idx_due_date, idx_dept_id, idx_funding_source), and unique constraint on (owner_type, owner_id, fee_type, due_date)
- **Commit:** `3d56db8`

### Task 2: Implement FeeRecord backend (Mapper, Service, Controller) with CRUD and paginated filtered listing

- Created `AchievementArchivedEvent` in achievement-common with ownerType, ownerId, achievementStatus, and nextFeeDate fields
- Created `FeeRecordDTO` with jakarta.validation constraints
- Created `FeeRecordVO` with computed fields (statusLabel, feeTypeLabel, alertLevel) and ownerInfo map
- Created `FeeRecordQueryDTO` for multi-dimensional filtering
- Created `FeeRecordMapper` with `@Select` annotation using `<script>` XML-style syntax for JOIN-based paginated query
  - JOINs with patent (when owner_type='patent') and copyright (when owner_type='copyright') tables for owner name resolution
  - Supports filters: status, feeType, fundingSource, keyword, dueDate range, ownerType, deptId
- Created `FeeRecordService` interface and `FeeRecordServiceImpl` with:
  - **T-02-01-01:** creator/updater injected from SecurityUtils
  - **T-02-01-02:** UpdateWrapper whitelist (only amount, fundingSource, paidAmount, voucherNo, status editable)
  - **T-02-01-03:** dept_id data isolation injection
  - **T-02-01-05:** delete restricted to creator + paused status
  - `FirstFeeGenerationListener` — `@EventListener` that auto-creates first fee record when patent transitions to ARCHIVED status
- Created `FeeRecordController` at `/api/fees` with POST, PUT, GET, DELETE, GET /page endpoints
- **Commit:** `3575716`

### Task 3: Implement frontend fee ledger views

- Updated `router/index.ts` with `/fee` top-level route (redirect to `/fee/ledger`) and 3 sub-tabs + hidden detail route
- Created `feeRecord.ts` API service with FeeRecordVO interface and all CRUD functions
- Created `FeeManagement.vue` wrapper component
- Created `FeeLedger.vue` with:
  - Quick filter tags (全部/即将逾期/已逾期/本月需缴费) that trigger filtered API calls
  - Filter row with feeType, status, fundingSource selects, date range picker, keyword input
  - Collapsible advanced filter (ownerType select) with ArrowUp/ArrowDown toggle
  - el-table with 9 data columns (费用类型, 关联成果, 截止日期, 金额, 实缴金额, 缴费日期, 经费来源, 状态, 操作) + selection checkboxes
  - Color-coded due dates (red=overdue, orange=7 days, yellow=15 days)
  - Edit dialog with editable amount/fundingSource/voucherNo/status but locked dueDate (D-17)
  - Delete button disabled for pending/paid records, enabled for paused
- Created `FeeDetail.vue` with 4 tabs: 基本信息 (label-value descriptions), 缴费历史 (placeholder), 附件 (AttachmentUploader), 操作日志 (placeholder)
- Created `FeePlan.vue` and `FeeStats.vue` placeholders
- **Commit:** `42ae671`

## Deviations from Plan

### Rule 2 - Enhancement: AchievementArchivedEvent extended with nextFeeDate field

- **Found during:** Task 2
- **Issue:** The plan states the event should carry minimal fields (ownerType, ownerId, achievementStatus) but the listener needs the patent's nextFeeDate to auto-generate the first fee. Since the fee module has no compile dependency on the patent module, it cannot query the patent entity directly.
- **Fix:** Added `nextFeeDate` (LocalDate) field to `AchievementArchivedEvent`. This allows the patent service to pass the nextFeeDate when publishing the event.
- **Files modified:** `achievement-common/src/main/java/com/institute/achievement/common/event/AchievementArchivedEvent.java`
- **Commit:** `3575716`

### Pre-existing Issue: vue-tsc type definition error

- **Found during:** Task 3 verification
- **Issue:** `npx vue-tsc --noEmit` reports `error TS2688: Cannot find type definition file for 'element-plus/global'`. This is a pre-existing configuration issue in the project (confirmed by running on base commit before changes).
- **Fix:** None needed — not caused by this plan's changes.

## Auth Gates

None.

## Verification

- `mvn compile -pl achievement-module/achievement-fee -am` — PASSED (Tasks 1 and 2)
- `npx vue-tsc --noEmit` — Pre-existing element-plus type error (not caused by this plan)
- V11 migration SQL creates fee_record table with all columns, 5 indexes, and unique constraint — PASSED
- Backend: FeeRecordController at `/api/fees` with full CRUD + paginated filtered listing with JOIN-based ownerName — PASSED
- Frontend: FeeLedger with quick filters, advanced filters, paginated el-table, edit dialog with locked dueDate, delete restricted to paused — PASSED

## Self-Check: PASSED

All plan files exist and compile. 3 commits recorded and verified.
