---
phase: 01
plan: 05
subsystem: "Achievement Invalidation + Duplicate Detection + Classified Access Control"
type: execute
tags: [invalidation, duplicate-detection, classified-access, lifecycle-management]
dependency_graph:
  requires: [01-01, 01-02, 01-03, 01-04]
  provides: [InvalidationService, DuplicateCheckService, ClassifiedPermissionService]
  affects: [AchievementRegister, AchievementDetail, AchievementList, PaperService, PatentService, CopyrightService]
tech-stack:
  added: []
  patterns: [UpdateWrapper for status transitions, Optional for duplicate queries, mockito strict stubbing]
key-files:
  created:
    - achievement-module/achievement-system/src/main/java/.../system/service/InvalidationService.java
    - achievement-module/achievement-system/src/main/java/.../system/service/DuplicateCheckService.java
    - achievement-module/achievement-system/src/main/java/.../system/controller/InvalidationController.java
    - achievement-module/achievement-system/src/main/java/.../system/controller/DuplicateCheckController.java
    - achievement-module/achievement-system/src/main/java/.../system/entity/InvalidationRecord.java
    - achievement-module/achievement-system/src/main/java/.../system/mapper/InvalidationRecordMapper.java
    - achievement-module/achievement-system/src/main/java/.../system/dto/InvalidationDTO.java
    - achievement-module/achievement-system/src/main/java/.../system/dto/InvalidationVO.java
    - achievement-module/achievement-system/src/main/java/.../system/dto/DuplicateCheckResult.java
    - achievement-module/achievement-system/src/main/resources/db/migration/V10__create_invalidation_record_table.sql
    - achievement-web/src/components/achievement/DuplicateDialog.vue
    - achievement-web/src/components/achievement/ClassifiedTag.vue
    - achievement-web/src/api/achievement/invalidation.ts
    - achievement-module/achievement-system/src/test/java/.../system/InvalidationServiceTest.java
    - achievement-module/achievement-system/src/test/java/.../system/DuplicateCheckServiceTest.java
    - achievement-module/achievement-system/src/test/java/.../system/ClassifiedPermissionServiceTest.java
    - achievement-module/achievement-system/src/test/java/.../system/AchievementLifecycleIntegrationTest.java
  modified:
    - achievement-module/achievement-system/src/main/java/.../system/service/ClassifiedPermissionService.java
    - achievement-web/src/views/achievement/AchievementRegister.vue
    - achievement-web/src/views/achievement/AchievementDetail.vue
    - achievement-web/src/views/achievement/AchievementList.vue
decisions:
  - D-34: Direct invalidation — creator or dept secretary can invalidate archived achievements
  - D-35: After invalidation — only creator and admin can view (others hidden)
  - D-36: Invalidation is irreversible (cannot invalidate already-invalidated achievements)
  - D-45: Submit-time duplicate check (not real-time), enforced server-side
  - D-46: Duplicate dialog shows existing achievement info with view-existing and continue-submit options
  - D-47: Draft submissions skip duplicate check
metrics:
  duration: "~45 min"
  completed_date: "2026-06-16"
---

# Phase 01 Plan 05: Achievement Invalidation, Duplicate Detection, and Classified Access Summary

**One-liner:** Implemented achievement invalidation with reason logging (D-34~D-36), submit-time duplicate detection with preview dialog (D-45~D-47), and classified marking with visual tags and access control (REG-10).

## Tasks Executed

| Task | Name | Type | TDD | Commit(s) |
|------|------|------|-----|-----------|
| 1 | Backend — InvalidationService, DuplicateCheckService, ClassifiedPermissionService | auto | yes | `9e4a6ac` (RED), `6d8c7ad` (GREEN) |
| 2 | Frontend — DuplicateDialog, AchievementDetail invalidation, ClassifiedTag, Register duplicate check | auto | no | `c138840` |
| 3 | Integration test — full lifecycle, duplicate detection, classified access enforcement | auto | no | `8e6864e` |

## Implementation Details

### Task 1: Backend Services (TDD: RED/GREEN)

**RED commit** (`9e4a6ac`): Created InvalidationRecord entity/mapper/DTOs, DuplicateCheckResult DTO, V10 migration. Wrote 34 failing tests across 3 test classes.

**GREEN commit** (`6d8c7ad`): Implemented:

- **InvalidationService**: Transitions ARCHIVED -> INVALIDATED with full permission validation (D-34: creator or same-dept secretary), visibility control (D-35: only creator/admin see invalidated), and irreversibility (D-36). Audit-logged with InvalidationRecord + AuditLogService in same @Transactional (T-01-20).

- **DuplicateCheckService**: Unified submit-time duplicate detection. `findExistingByDoi()`, `findExistingByApplicationNo()`, `findExistingByRegistrationNo()` return Optional<Paper/Patent/Copyright>. `checkDuplicateForSubmit()` returns `DuplicateCheckResult` with existing achievement title, type, status, and submit time for frontend dialog display (D-46).

- **ClassifiedPermissionService**: Enhanced with `filterClassifiedAchievements()` (removes unauthorized classified items from list queries), `canUserViewAttachment()` (classified attachment access control), `getUserClassifiedRole()` (role info query).

- **InvalidationController**: POST /api/achievement/invalidate, GET /api/achievement/invalidation, GET /api/achievement/invalidations.

- **DuplicateCheckController**: GET /api/achievement/check-duplicate?type=paper&field=DOI.

### Task 2: Frontend Components

- **DuplicateDialog.vue** (UI-SPEC Section 6.9): Dialog shows existing achievement title, type, status, submit time with [查看已有成果] (navigates to existing detail) and [继续填写并提交] buttons.

- **ClassifiedTag.vue** (UI-SPEC Section 4.4): Renders 秘密 as orange (warning) tag and 机密 as red (danger) tag. Used in AchievementList, AchievementDetail, and form.

- **AchievementRegister.vue**: Submit flow now includes duplicate check step — validates form, calls checkDuplicate API, shows DuplicateDialog if found. Draft submissions skip check (D-47). DOI is optional; if empty, skip duplicate check for paper.

- **AchievementDetail.vue**: ARCHIVED status shows [作废] button with ElMessageBox.prompt containing reason textarea. INVALIDATED status shows warning banner and hides all action buttons (view-only mode). ClassifiedTag displayed next to title.

- **AchievementList.vue**: Added 密级 column with ClassifiedTag.

### Task 3: Integration Tests

**AchievementLifecycleIntegrationTest** (13 tests): Covers full lifecycle (create -> submit -> approve -> archive -> invalidate -> verify visibility), duplicate detection for all three unique fields, classified access rules, permission enforcement, and irreversibility.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Mockito updateById mismatch in test expectations**
- **Found during:** Task 1 GREEN phase test execution
- **Issue:** Tests called `verify(paperMapper).updateById()`, but implementation uses `paperMapper.update(null, new UpdateWrapper<>())` to set specific fields
- **Fix:** Updated test assertions to match implementation pattern: `verify(paperMapper).update(null, any(UpdateWrapper.class))`
- **Files modified:** `InvalidationServiceTest.java`
- **Commit:** `6d8c7ad`

**2. [Rule 3 - Blocking] Duplicate recordCaptor variable in test**
- **Found during:** Task 1 GREEN phase
- **Issue:** Duplicate `ArgumentCaptor<InvalidationRecord> recordCaptor` declaration caused compilation error
- **Fix:** Removed duplicate block
- **Files modified:** `InvalidationServiceTest.java`
- **Commit:** `6d8c7ad`

## TDD Gate Compliance

- [x] RED commit exists: `test(01-05): add failing tests for invalidation, duplicate check, and classified permission services` (9e4a6ac)
- [x] GREEN commit exists: `feat(01-05): implement invalidation backend, duplicate check service, and classified permission enhancements` (6d8c7ad)
- [ ] REFACTOR commit: Not needed — code quality targets met immediately

## Verification Results

- `mvn test -pl achievement-module/achievement-system -Dtest=InvalidationServiceTest` — **14/14 passed**
- `mvn test -pl achievement-module/achievement-system -Dtest=DuplicateCheckServiceTest` — **10/10 passed**
- `mvn test -pl achievement-module/achievement-system -Dtest=ClassifiedPermissionServiceTest` — **10/10 passed**
- `mvn test -pl achievement-module/achievement-system -Dtest=AchievementLifecycleIntegrationTest` — **13/13 passed**
- `mvn test -pl achievement-module/achievement-system -DskipITs` — **99/99 passed** (all system module tests)

## Requirement Coverage

| Requirement | Status | Notes |
|-------------|--------|-------|
| REG-08 | Implemented | Achievement invalidation with reason logging, visibility control, irreversibility |
| REG-09 | Implemented | Submit-time duplicate detection for DOI (paper), applicationNo (patent), registrationNo (copyright) |
| REG-10 | Enhanced | ClassifiedPermissionService with filterClassifiedAchievements, canUserViewAttachment, getUserClassifiedRole |

## Threat Model Coverage

| Threat | Disposition | Status |
|--------|-------------|--------|
| T-01-16: DoS via invalidation abuse | mitigate | Only creator and dept secretary can invalidate; audit log records every invalidation |
| T-01-17: Classified data disclosure | mitigate | ClassifiedPermissionService filters in page queries + isVisibleToUser in getById |
| T-01-18: Classified attachment download | mitigate | canUserViewAttachment checks classified achievement access before download |
| T-01-19: Duplicate detection bypass | mitigate | DuplicateCheckService called server-side (not just frontend) in PaperService.submitPaper |
| T-01-20: Invalidation repudiation | mitigate | InvalidationRecord + AuditLogService.logged in same @Transactional block |

## Commits

| Hash | Message |
|------|---------|
| 9e4a6ac | test(01-05): add failing tests for invalidation, duplicate check, and classified permission services |
| 6d8c7ad | feat(01-05): implement invalidation backend, duplicate check service, and classified permission enhancements |
| c138840 | feat(01-05): implement frontend for invalidation, duplicate detection, and classified marking |
| 8e6864e | test(01-05): add integration tests for full lifecycle, duplicate detection, and classified access |

**Duration:** ~45 minutes
