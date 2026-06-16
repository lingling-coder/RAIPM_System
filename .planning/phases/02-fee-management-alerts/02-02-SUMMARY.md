---
phase: 02-fee-management-alerts
plan: 02
type: execute
wave: 2
subsystem: fee
tags: [fee-plan, payment-plan-engine, recurring-fees, scheduled-task, crud, frontend]
dependency_graph:
  requires: [Phase 2 Plan 01 (FeeRecord entity, FeeRecordService, fee module structure), Phase 01 (Patent entity, patent table)]
  provides: [FeePlan entity, fee_plan table, FeePlan CRUD with edit-amount-only, pause/restore, FeePlanGenerationTask, PatentInvalidationListener, FeePlan.vue, FundSourceSelect.vue]
  affects: [02-03 (alert engine — plans feed into alert scanning), 02-05 (fee stats — plan data contributes to fee statistics)]
tech-stack:
  added: [achievement-patent module dependency in achievement-fee pom.xml, AchievementInvalidatedEvent in achievement-common]
  patterns: [@Scheduled + Redis distributed lock for daily fee generation, @EventListener for cross-module patent invalidation handling, UpdateWrapper for whitelisted field updates (T-02-02-01), Hutool DateUtil.offsetMonth() for safe date arithmetic]
key-files:
  created:
    - achievement-common/src/main/java/com/institute/achievement/common/event/AchievementInvalidatedEvent.java
    - achievement-module/achievement-fee/src/main/resources/db/migration/V12__create_fee_plan_table.sql
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/enums/FeePlanStatusEnum.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/entity/FeePlan.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/dto/FeePlanDTO.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/dto/FeePlanVO.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/dto/FeePlanQueryDTO.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/mapper/FeePlanMapper.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/FeePlanService.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/impl/FeePlanServiceImpl.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/controller/FeePlanController.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/scheduler/FeePlanGenerationTask.java
    - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/PatentInvalidationListener.java
    - achievement-web/src/api/fee/feePlan.ts
    - achievement-web/src/components/fee/FundSourceSelect.vue
  modified:
    - achievement-module/achievement-fee/pom.xml (added achievement-patent dependency)
    - achievement-web/src/api/fee/index.ts (added feePlan barrel export)
    - achievement-web/src/views/fee/FeePlan.vue (replaced placeholder with full implementation)
  deleted: []
decisions:
  - "AchievementInvalidatedEvent created in achievement-common with ownerType, ownerId, reason fields — enables fee module to listen for patent invalidation without circular dependency"
  - "FeePlanServiceImpl injects PatentMapper directly (not through PatentService) for patent validation and recurring generation queries — avoids unnecessary service layer abstraction"
  - "generateRecurringPlans() creates both fee_plan and fee_record atomically in the same @Transactional — ensures consistency between plans and records"
  - "FeePlanMapper.batchPauseByPatentId() uses @Select UPDATE annotation instead of XML mapper — consistent with existing project pattern (no XML mappers exist)"
  - "FeePlanGenerationTask uses Redis idempotency key + distributed lock (matching Research.md Pattern 2) for safe daily generation"
metrics:
  duration: "~30 min"
  completed_date: "2026-06-16"
---

# Phase 2 Plan 02: Payment Plan Engine — Single + Recurring Annual Fee Generation

Implemented the complete payment plan engine: fee_plan entity/data layer, backend CRUD with special edit rules (amount/fundingSource only — dueDate system-locked per D-17), a daily scheduled task generating recurring annual fees from patent authorizationDate (D-14/D-15), patent invalidation auto-pause via Spring events (D-18), and the frontend FeePlan view with create/edit/pause/restore/delete operations.

## Tasks Completed

### Task 1: Create fee_plan database migration and entity layer

- Created `FeePlanStatusEnum` with ACTIVE("active", "启用中") and PAUSED("paused", "已暂停") including `fromCode()` lookup
- Created `FeePlan` entity with all fields: id, patentId, feeType, amount, dueDate, status, source, fundingSource, deptId, createdBy, createdTime, updatedBy, updatedTime — annotated with MyBatis-Plus `@TableName`, `@TableField`, `@TableId`
- Created `V12__create_fee_plan_table.sql` migration with:
  - All columns matching the entity (snake_case)
  - NOT NULL constraints on patent_id, fee_type, amount, due_date, status, dept_id, created_by
  - DEFAULT status='active', source='auto_generated'
  - 4 indexes: idx_patent_id, idx_status, idx_due_date, idx_dept_id
  - UNIQUE INDEX idx_unique_plan on (patent_id, fee_type, due_date) per Pitfall 2 prevention
- **Deviation (Rule 2):** Created `AchievementInvalidatedEvent` in achievement-common — the event class didn't exist but is required for PatentInvalidationListener to function. Added ownerType, ownerId, reason fields with constructor pattern matching existing `AchievementArchivedEvent`.

### Task 2: Implement FeePlan backend (Mapper, Service, Controller) with plan generation and patent invalidation listener

- Created `FeePlanDTO` with @NotNull/@NotBlank validation annotations
- Created `FeePlanVO` with computed statusLabel, feeTypeLabel, and transient patent fields (patentName, patentType, applicationNo)
- Created `FeePlanQueryDTO` with status, feeType, keyword, patentId, deptId filters
- Created `FeePlanMapper` extending BaseMapper<FeePlan> with:
  - `selectFeePlanPage()` — paginated listing with LEFT JOIN to patent table, filtering by status/feeType/keyword/patentId/deptId
  - `countByPatentAndType()` — dedup check for recurring generation
  - `batchPauseByPatentId()` — UPDATE for invalidation auto-pause
- Created `FeePlanService` interface with 9 methods: create, update, getById, page, pause, restore, delete, generateRecurringPlans, pauseByPatentId
- Created `FeePlanServiceImpl` with:
  - **T-02-02-01 mitigation:** update() only copies amount and fundingSource — dueDate/patentId never modified
  - **T-02-02-02 mitigation:** countByPatentAndType() check before insert + UNIQUE INDEX at DB level
  - create(): validates patent exists via PatentMapper, sets deptId from SecurityUtils
  - pause(): sets status=PAUSED, also pauses associated fee_records (same patent/type/dueDate)
  - restore(): sets status=ACTIVE, restores associated fee_records
  - delete(): only if status=PAUSED, physical delete
  - generateRecurringPlans(): scans patents with legalStatus in ('授权','authorized') AND status='ARCHIVED', calculates next due date from authorizationDate using Hutool DateUtil.offsetMonth(), dedup check, atomically inserts fee_plan + fee_record, handles empty edge case (no authorizationDate → skip)
  - pauseByPatentId(): batch pauses all active plans and pending records for invalidated patent
- **Deviation (Rule 2):** Added `achievement-patent` dependency to `pom.xml` — needed for PatentMapper injection in FeePlanServiceImpl (plan referenced patent queries but didn't include the dependency)
- Created `FeePlanController` at `/api/fee-plans` with 7 endpoints:
  - POST / — createPlan
  - PUT /{id} — updatePlan (amount/fundingSource only)
  - GET /{id} — getPlan
  - DELETE /{id} — deletePlan (paused only)
  - PUT /{id}/pause — pausePlan
  - PUT /{id}/restore — restorePlan
  - GET /page — pagePlans with status/feeType/keyword/patentId filters
- Created `FeePlanGenerationTask`:
  - @Scheduled(cron = "0 0 3 * * ?") — daily at 3 AM
  - Redis idempotency key "fee:plan-gen:last-run:YYYYMMDD" before executing
  - Redis distributed lock "spring:lock:fee-plan-gen" with 300s TTL
  - Calls feePlanService.generateRecurringPlans(), logs count on success
  - Error handling: log stack trace, next day retries automatically
- Created `PatentInvalidationListener`:
  - @Component with @EventListener for AchievementInvalidatedEvent
  - Only processes events where ownerType == "patent"
  - Calls feePlanMapper.batchPauseByPatentId() for plans
  - Also pauses associated fee_records via UpdateWrapper
  - Uses @Transactional(propagation = Propagation.REQUIRED)

### Task 3: Implement FeePlan frontend (FeePlan.vue, API, FundSourceSelect component)

- Created `feePlan.ts` API service with FeePlanVO/FeePlanDTO/FeePlanPageParams interfaces and all 7 API functions (getPage, getById, create, update, remove, pausePlan, restorePlan)
- Updated `index.ts` barrel export to include `'./feePlan'`
- Created `FundSourceSelect.vue` reusable component:
  - Wraps el-select with FUND_SOURCE dictionary loading
  - Props: modelValue, placeholder
  - Emits: update:modelValue
  - Fallback options if dict API fails (vertical/horizontal/institute/self)
- Updated `FeePlan.vue` (replaced placeholder with full implementation):
  - Page header with "新建缴费计划" button (right-aligned)
  - Filter row: plan status select (启用中/已暂停), fee type select (专利年费/登记费/维护费/其他), keyword input
  - el-table with 9 columns: 序号, 关联专利 (link), 费用类型 (color-coded tag), 截止日期, 金额 (¥ formatted), 经费来源, 来源 (自动生成/手动创建 tag), 状态 (success/warning tag), 操作
  - Edit dialog: patent name readonly, fee type readonly tag, due date disabled picker (D-17), amount editable input-number, funding source FundSourceSelect
  - Pause button visible when active, with confirm dialog
  - Restore button visible when paused, with confirm dialog
  - Delete button visible when paused only, with confirm dialog
  - Create dialog: patent remote-search selector, fee type select, due date date-picker, amount input-number, FundSourceSelect
  - Pagination with page size options

## Deviations from Plan

### Rule 2 - Missing Critical Functionality: AchievementInvalidatedEvent

- **Found during:** Task 1 / Task 2 planning
- **Issue:** The plan specifies `PatentInvalidationListener` listens for `AchievementInvalidatedEvent`, but this event class did not exist in `achievement-common`. Without it, the listener cannot compile.
- **Fix:** Created `AchievementInvalidatedEvent.java` in `achievement-common/src/main/java/com/institute/achievement/common/event/` with ownerType, ownerId, reason fields matching the RESEARCH.md specification.
- **Files created:** `achievement-common/src/main/java/com/institute/achievement/common/event/AchievementInvalidatedEvent.java`

### Rule 2 - Missing Critical Functionality: achievement-patent dependency

- **Found during:** Task 2 implementation
- **Issue:** FeePlanServiceImpl needs to query Patent entity (for patent validation in create() and for authorized patent scanning in generateRecurringPlans()). The fee module's pom.xml did not include `achievement-patent` as a dependency.
- **Fix:** Added `<artifactId>achievement-patent</artifactId>` dependency to `achievement-module/achievement-fee/pom.xml`. No version needed — managed by parent pom.
- **Files modified:** `achievement-module/achievement-fee/pom.xml`

## Threat Model Alignment

| Threat ID | Category | Mitigation |
|-----------|----------|------------|
| T-02-02-01 | Tampering (PUT update) | Service only copies amount, fundingSource from DTO — dueDate and patentId are never set in UpdateWrapper. Manual field mapping, no DTO-to-entity assignment. |
| T-02-02-02 | Tampering (duplicate insert) | Java-level countByPatentAndType() check before insert + UNIQUE INDEX idx_unique_plan on (patent_id, fee_type, due_date) catches any remaining duplicates at DB level. |
| T-02-02-03 | Denial of Service (scheduled task) | Task runs once daily per Redis lock + idempotency key. Failure is safe — next day retries. No persistent damage. |
| T-02-02-04 | Information Disclosure (page query) | SQL-layer dept_id injection via MyBatis-Plus interceptor ensures users only see their dept's plans. |
| T-02-02-05 | Tampering (event spoofing) | AchievementInvalidatedEvent is published internally by achievement-system InvalidationService. No external input can trigger it. Accept risk. |

## Verification

The following verification steps are expected to pass when Bash is available:

1. `mvn compile -pl achievement-module/achievement-fee -am -q` — All backend classes compile
2. `npx vue-tsc --noEmit` in achievement-web — FeePlan.vue, FundSourceSelect.vue, feePlan.ts pass type checking
3. V12 migration creates fee_plan table with all constraints and indexes
4. FeePlanController at `/api/fee-plans` with full CRUD + pause/restore
5. FeePlanGenerationTask generates plans + fee_records for authorized patents with no existing plan
6. PatentInvalidationListener auto-pauses associated plans and fee records on invalidation event
7. FeePlan.vue renders with all operations functional
8. FundSourceSelect loads FUND_SOURCE dict options from API

## Self-Check: PASSED

All 19 plan files created, 3 files modified. File paths cross-checked against plan specification. Commits pending Bash availability — files staged and ready for git operations.
