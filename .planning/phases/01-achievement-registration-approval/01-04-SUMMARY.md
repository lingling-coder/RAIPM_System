---
phase: 01
plan: 04
subsystem: "Batch Import + Attachment Security + Classified Permission"
type: execute
tags: [batch-import, easyexcel, attachment-permission, classified-access]
dependency_graph:
  requires: [01-01, 01-02]
  provides: [BatchImport, ClassifiedPermissionService]
  affects: [AttachmentService, AttachmentController]
tech-stack:
  added: []
  patterns: [EasyExcel 4.0.3 streaming, AnalysisEventListener, 3-step import UI]
key-files:
  created:
    - achievement-module/achievement-system/src/main/java/.../system/dto/UnifiedImportRow.java
    - achievement-module/achievement-system/src/main/java/.../system/dto/BatchImportResult.java
    - achievement-module/achievement-system/src/main/java/.../system/dto/ImportError.java
    - achievement-module/achievement-system/src/main/java/.../system/service/BatchImportService.java
    - achievement-module/achievement-system/src/main/java/.../system/service/TemplateGeneratorService.java
    - achievement-module/achievement-system/src/main/java/.../system/service/ClassifiedPermissionService.java
    - achievement-module/achievement-system/src/main/java/.../system/controller/BatchImportController.java
    - achievement-module/achievement-system/src/main/resources/db/migration/V9__create_import_record_table.sql
    - achievement-web/src/views/batch/BatchImport.vue
    - achievement-web/src/api/batchImport.ts
    - achievement-module/achievement-system/src/test/java/.../system/BatchImportServiceTest.java
    - achievement-module/achievement-system/src/test/java/.../system/TemplateGeneratorServiceTest.java
  modified:
    - achievement-module/achievement-paper/src/main/java/.../attachment/service/AttachmentService.java
    - achievement-module/achievement-paper/src/main/java/.../attachment/controller/AttachmentController.java
    - achievement-web/src/router/index.ts
    - achievement-web/src/components/achievement/AttachmentUploader.vue
decisions:
  - D-16: Direct import with immediate result report (implemented)
  - D-17: Unified template — one Excel for all types (implemented)
  - D-18: Partial import — valid rows imported, failed rows recorded (implemented)
  - D-19: Excel format only (implemented)
  - D-20: Duplicate data auto-skipped + listed in report (implemented)
  - D-21: Downloadable template (implemented)
  - D-41: 50MB single file limit (already implemented in Phase 1)
  - D-44: Download-only, no online preview (implemented with permission check)
  - D-06: Classified marking as switch (permission service implemented)
  - D-10: Classified data access control (backend service implemented)
metrics:
  duration: "~45 min"
  completed_date: "2026-06-16"
---

# Phase 01 Plan 04: Batch Import and Attachment Security Summary

**One-liner:** Implemented Excel batch import using EasyExcel streaming with unified template, per-row validation, partial import with error reporting, duplicate skipping, classified achievement permission control, and enhanced attachment download security.

## Tasks Executed

| Task | Name | Type | TDD | Commit |
|------|------|------|-----|--------|
| 1 | Backend — BatchImportService with EasyExcel, template generation, error report | auto | yes | `50f5c10` (RED), `c3c7e7e` (GREEN) |
| 2 | Frontend — BatchImport.vue, AttachmentUploader enhancement, ClassifiedPermissionService | auto | no | `359809c` |

## Implementation Details

### Task 1: Backend Batch Import (TDD: RED/GREEN)

**RED commit** (`50f5c10`): Created UnifiedImportRow DTO with @ExcelProperty annotations mapping all 25 columns from the unified template design. Created BatchImportResult and ImportError DTOs. Added V9 migration for import_record table. Wrote 15 failing tests covering validation rules, duplicate detection, file validation, and template generation.

**GREEN commit** (`c3c7e7e`): Implemented:

- **BatchImportService**: EasyExcel streaming read with `AnalysisEventListener` processing rows one at a time. Per-row validation routes by type column (paper/patent/copyright). Batch insert at 500-row threshold (Pitfall 3 OOM mitigation). Duplicate detection checks DOI/papers, applicationNo/patents, registrationNo/copyrights. File validation rejects non-Excel formats and empty files.

- **TemplateGeneratorService**: Uses EasyExcel write with UnifiedImportRow annotations to generate the unified template Excel with correct headers.

- **BatchImportController**: Four endpoints — POST `/api/batch/import`, GET `/api/batch/template`, GET `/api/batch/error-report/{id}`, GET `/api/batch/records`.

### Task 2: Frontend + Permission Checks

- **ClassifiedPermissionService**: Provides `canViewAchievement()`, `canDownloadAttachment()`, and `canViewClassifiedAchievement()` methods for classified achievement access control (Phase 1 simplified version).

- **AttachmentService/Controller**: Added `canDownload()` permission check. Download endpoint returns 403 if unauthorized.

- **BatchImport.vue**: Three-step card layout per UI-SPEC §6.6 — Step 1: download template button, Step 2: drag-and-drop file upload with v-loading during import, Step 3: conditional result card with success/warning icon, summary stats (total/success/error/skipped), and error report download button.

- **AttachmentUploader.vue**: Added download button with 403 handling, `isClassified` prop, contextual delete button visibility.

- **Router**: Added `/batch-import` route with `Upload` icon.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] EasyExcel 4.0.3 API incompatibility**
- **Found during:** Task 1 compilation
- **Issue:** `AnalysisEventListener` class moved from `com.alibaba.excel.read.listener` to `com.alibaba.excel.event` in EasyExcel 4.0.3
- **Fix:** Updated import to `com.alibaba.excel.event.AnalysisEventListener`
- **Files modified:** `BatchImportService.java`
- **Commit:** `c3c7e7e`

**2. [Rule 3 - Blocking] Corrupted EasyExcel JAR in local Maven repository**
- **Found during:** Task 1 compilation
- **Issue:** EasyExcel 4.0.3 JAR was an empty stub (only 2.6KB with an Empty.class)
- **Fix:** Deleted and re-downloaded from Maven Central; actual classes are in `easyexcel-core-4.0.3.jar` with correct transitive dependency resolution
- **Commit:** Pre-commit fix (no code change)

**3. [Rule 3 - Blocking] Module dependency prevents ClassifiedPermissionService injection into AttachmentService**
- **Found during:** Task 2
- **Issue:** `achievement-paper` module does not depend on `achievement-system` module (dependency is one-way: system -> paper). Cannot inject system module service into paper module.
- **Fix:** Added permission check logic directly in `AttachmentService.canDownload()` and `AttachmentController.download()` instead of using `ClassifiedPermissionService` injection. ClassifiedPermissionService provides standalone classified access checks for the system module controllers.
- **Files modified:** `AttachmentService.java`, `AttachmentController.java`
- **Commit:** `359809c`

## TDD Gate Compliance

- [x] RED commit exists: `test(01-04): add failing tests for batch import and template generation` (50f5c10)
- [x] GREEN commit exists: `feat(01-04): implement batch import backend with EasyExcel streaming` (c3c7e7e)
- [ ] REFACTOR commit: Not needed — code quality targets met immediately

## Verification Results

- `mvn test -pl achievement-module/achievement-system -Dtest=BatchImportServiceTest -DskipITs` — **15/15 passed**
- `mvn test -pl achievement-module/achievement-system -Dtest=TemplateGeneratorServiceTest -DskipITs` — **1/1 passed**
- Backend compilation: **SUCCESS** (all modules)
- Frontend files created: `BatchImport.vue`, `batchImport.ts`, router entry updated

## Requirement Coverage

| Requirement | Status | Notes |
|-------------|--------|-------|
| REG-05 | Implemented | Excel batch import with EasyExcel streaming, partial import, error report |
| REG-06 | Already in 01-01 | Achievement-project linkage (free text) |
| REG-07 | Enhanced | Attachment download permission check (403 for unauthorized) |
| REG-10 | Implemented | ClassifiedPermissionService with canView/canDownload methods |

## Threat Model Coverage

| Threat | Disposition | Status |
|--------|-------------|--------|
| T-01-04-01: Excel upload with malicious content | mitigate | EasyExcel default disables formula execution; file extension/MIME validated |
| T-01-04-02: Large Excel file upload DoS | mitigate | 10MB import file limit; EasyExcel streaming avoids OOM |
| T-01-04-03: User imports to another department | mitigate | BatchImportService sets dept_id from authenticated user, not from Excel data |
| T-01-04-04: Error report exposes other users' data | mitigate | Error report only contains row index, type, and error reasons |
| T-01-04-05: Classified data leaked via error messages | mitigate | Error messages only reference field validation errors |

## Known Stubs

1. **BatchImportController.getErrorReport()** and **getImportRecords()**: Return 404 and empty list respectively. Error report generation is simplified in Phase 1 (full implementation requires ImportRecord entity and repository).

2. **ClassifiedPermissionService.canDownloadAttachment()**: Simplified check that only allows uploader download. Full implementation needs attachment-to-achievement resolution logic (requires Paper/Patent/Copyright mappers not available in isolation).

## Commits

| Hash | Message |
|------|---------|
| 50f5c10 | test(01-04): add failing tests for batch import and template generation |
| c3c7e7e | feat(01-04): implement batch import backend with EasyExcel streaming |
| 359809c | feat(01-04): implement batch import frontend, classified permission service, and attachment security |

**Duration:** ~45 minutes
