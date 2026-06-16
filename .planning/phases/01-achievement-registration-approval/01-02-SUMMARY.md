---
phase: 01
plan: 02
subsystem: "achievement-patent, achievement-copyright, achievement-web"
tags: ["patent", "copyright", "registration", "duplicate-detection", "templates"]
dependency_graph:
  requires: ["01-01-paper-registration"]
  provides: ["Patent CRUD API", "Copyright CRUD API", "PatentForm.vue", "CopyrightForm.vue", "Duplicate detection (inline)"]
  affects: ["03-approval-workflow", "04-batch-import", "05-approval-workflow"]
tech-stack:
  added: ["Patent entity with MyBatis-Plus", "Copyright entity with MyBatis-Plus", "PatentForm component", "CopyrightForm component"]
  patterns: ["Constructor injection (no @Autowired)", "Mockito + JUnit 5 tests", "Inline duplicate check (same as PaperService)", "Dynamic component registration"]
key-files:
  created:
    - "achievement-module/achievement-patent/"
    - "achievement-module/achievement-copyright/"
    - "achievement-web/src/components/achievement/PatentForm.vue"
    - "achievement-web/src/components/achievement/CopyrightForm.vue"
    - "achievement-web/src/api/achievement/patent.ts"
    - "achievement-web/src/api/achievement/copyright.ts"
  modified:
    - "pom.xml (parent + module aggregator)"
    - "achievement-module/achievement-system/pom.xml"
    - "achievement-web/src/views/achievement/AchievementRegister.vue"
    - "achievement-web/src/views/achievement/AchievementList.vue"
    - "achievement-web/src/views/achievement/AchievementDetail.vue"
    - "achievement-web/src/stores/achievement.ts"
    - "achievement-web/src/views/__tests__/AchievementRegister.spec.ts"
decisions:
  - "DuplicateCheckService removed — each service does inline duplicate check (avoids circular dependency between achievement-system and achievement-patent/achievement-copyright)"
  - "Copyright entity uses softwareVersion field name instead of version to avoid conflict with MyBatis-Plus @Version optimistic locking field"
  - "Frontend AchievementDetail detects achievement type from API response data shape (presence of title/patentName/name fields)"
metrics:
  duration: "~42 min"
  completed_date: "2026-06-16"
---

# Phase 01 Plan 02: Patent and Software Copyright Registration Summary

## One-liner
Extended the unified registration page with patent (10 REG-02 fields) and software copyright (6 REG-03 fields) support, including backend CRUD services with inline duplicate detection on applicationNo/registrationNo and frontend form components with validation.

## Deliverables

### Backend — Patent Module (achievement-patent)
- **Patent entity**: All REG-02 fields (patentName, inventors, applicationNo, authorizationNo, applicationDate, authorizationDate, patentType, country, nextFeeDate, legalStatus) plus common lifecycle/classification/ownership fields
- **PatentMapper**: MyBatis-Plus BaseMapper with UNIQUE index on application_no (DB-level duplicate prevention)
- **PatentDTO**: Jakarta validation (@NotBlank on patentName, inventors, applicationNo, patentType, country, legalStatus; @NotNull on applicationDate)
- **PatentVO**: MapStruct-style VO with computed statusLabel
- **PatentService**: Full CRUD, submit with inline duplicate check (D-45), draft save/load, pagination, ownership verification (T-01-04)
- **PatentController**: REST endpoints at /api/patents/* (create, update, submit, draft, get, page)
- **Migration V5**: Create patent table with UNIQUE index on application_no

### Backend — Copyright Module (achievement-copyright)
- **Copyright entity**: All REG-03 fields (name, copyrightHolder, registrationNo, registrationDate, softwareVersion, softwareCategory) plus common fields
- **CopyrightMapper**: MyBatis-Plus BaseMapper with UNIQUE index on registration_no
- **CopyrightDTO**: Jakarta validation (@NotBlank on name, copyrightHolder, registrationNo, softwareVersion, softwareCategory; @NotNull on registrationDate)
- **CopyrightVO**: VO with computed statusLabel
- **CopyrightService**: Full CRUD, submit with inline duplicate check, draft save/load, pagination, ownership verification
- **CopyrightController**: REST endpoints at /api/copyrights/*
- **Migration V6**: Create software_copyright table with UNIQUE index on registration_no

### Frontend
- **PatentForm.vue**: Config-driven form with el-input, el-select, el-date-picker for all REG-02 fields; patentType (发明/实用新型/外观设计), country (中国/美国/欧洲/日本/韩国/PCT/其他), legalStatus (授权/实审/公开/驳回/撤回/终止/无效) select options; required validation on 7 fields
- **CopyrightForm.vue**: Config-driven form with el-input, el-select, el-date-picker for all REG-03 fields; softwareCategory options (操作系统/数据库/中间件/应用软件/嵌入式软件/其他); required validation on all 6 fields
- **AchievementRegister.vue**: Added PatentForm and CopyrightForm to formComponentMap; submit/draft dispatch routes to correct API based on activeType
- **AchievementList.vue**: Dispatches to correct API (paper/patent/copyright) based on type filter; displays correct type label per row
- **AchievementDetail.vue**: Detects achievement type from API response shape; conditionally renders paper/patent/copyright field sets in el-descriptions
- **Store (achievement.ts)**: Supports PatentFormDTO and CopyrightFormDTO types; resetForm creates correct empty form per type
- **API layers**: patentApi and copyrightApi with submit, createDraft, saveDraft, getById, getPage

### Tests

| Test Suite | Tests | Status |
|-----------|-------|--------|
| PatentServiceTest | 7 | PASS |
| CopyrightServiceTest | 5 | PASS |
| Frontend component tests (vitest) | 13 | PASS |
| **Total** | **25** | **ALL PASS** |

## Deviations from Plan

### Rule 2 — Circular dependency avoided
**Issue:** The plan specified a `DuplicateCheckService` in achievement-system that would inject PatentMapper and CopyrightMapper. This creates a circular dependency because PatentService needs DuplicateCheckService from system, while system needs PatentMapper from patent.

**Fix:** Removed DuplicateCheckService. Each service performs duplicate checks inline using its own mapper (same pattern as PaperService.checkDuplicateDoi). This is cleaner, avoids the circular dependency, and follows the existing architecture pattern.

**Files affected:**
- Removed: `achievement-module/achievement-system/src/main/java/.../DuplicateCheckService.java`

### Rule 2 — Field name conflict resolution
**Issue:** The Copyright entity had two fields named `version` — one for the software version string (REG-03) and one for the MyBatis-Plus @Version optimistic locking integer.

**Fix:** Renamed the software version field to `softwareVersion` (maps to DB column `software_version`). The optimistic lock field remains `version`. Updated DTO, VO, service, and migration SQL to use the new field name.

## TDD Gate Compliance

Gate sequence verified:
1. `test(01-02): add failing tests for patent and copyright service modules` — RED gate (commit f45eb33)
2. `feat(01-02): implement patent and copyright backend modules` — GREEN gate (commit 57f2595)
3. `feat(01-02): add frontend patent and copyright forms with full integration` — GREEN gate (commit ea0e3a8)

RED phase confirmed: stubs threw UnsupportedOperationException, all tests failed.
GREEN phase confirmed: full implementations, all 12 backend tests passed.

## Commits

| # | Hash | Message |
|---|------|---------|
| 1 | f45eb33 | test(01-02): add failing tests for patent and copyright service modules |
| 2 | 57f2595 | feat(01-02): implement patent and copyright backend modules |
| 3 | ea0e3a8 | feat(01-02): add frontend patent and copyright forms with full integration |

## Self-Check: PASSED
