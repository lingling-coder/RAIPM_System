---
phase: 01
plan: 01
subsystem: "achievement-paper, achievement-integration, achievement-web"
tags: ["paper", "doi", "registration", "draft", "attachment", "migration"]
dependency_graph:
  requires: ["00-foundation-infrastructure (Phase 0)"]
  provides: ["Paper CRUD API", "DOI auto-complete service", "AchievementRegister UI", "AchievementList UI", "AchievementDetail UI", "Attachment service"]
  affects: ["02-patent-registration", "03-copyright-registration", "04-batch-import", "05-approval-workflow"]
tech-stack:
  added: ["Paper entity with MyBatis-Plus", "CrossrefClient (HttpClient)", "OpenAlexClient (HttpClient)", "DoiAutoFillService", "Pinia store (useAchievementStore)"]
  patterns: ["Constructor injection (no @Autowired)", "Mockito + JUnit 5 tests", "Vitest frontend tests", "Exclusive Arc for polymorphism", "new LambdaQueryWrapper<>() syntax"]
key-files:
  created: ["achievement-module/achievement-paper/", "achievement-module/achievement-integration/"]
  modified: ["pom.xml (parent + module aggregator)", "achievement-web/src/router/index.ts", ".gitignore"]
decisions: []
metrics:
  duration: "~45 min"
  completed_date: "2026-06-16"
---

# Phase 01 Plan 01: Paper Registration Implementation Summary

## One-liner
Delivered end-to-end paper registration with all REG-01 metadata fields, DOI auto-complete from Crossref/OpenAlex with priority-ordered fallback, draft save/load, attachment upload with 50MB limit, and achievement list/detail views.

## Deliverables

### Backend Modules
1. **achievement-module/achievement-paper** — Paper entity, mapper, service (CRUD + submit + draft), controller (REST endpoints), DOI controller
2. **achievement-module/achievement-integration** — DOI auto-fill service with multi-source priority (Crossref + OpenAlex), API gateway with retry, fallback handler

### Common (achievement-common)
- AchievementTypeEnum (PAPER/PATENT/COPYRIGHT)
- AchievementStatusEnum (7-state lifecycle)
- AchievementConstant (field lengths, file limits)
- AchievementException (domain exception with error codes)

### Database Migrations (Flyway)
- V1: paper table with all REG-01 fields, indexes, optimistic locking
- V2: attachment table with Exclusive Arc pattern
- V3: draft support index placeholder
- V4: DOI source configuration table

### Attachment Module
- Attachment entity with soft-delete, Exclusive Arc polymorphism
- File upload with 50MB limit, MIME/extension validation, UUID stored names
- Download streaming with proper headers
- Soft-delete by uploader only

### Frontend
- **AchievementRegister.vue**: Unified registration page with type selector, dynamic form, classified marking, project linkage, attachment upload, submit/save draft
- **PaperForm.vue**: All 13 paper fields with config-driven layout, proper validation
- **DoiAutoComplete.vue**: On-blur DOI lookup with inline loading spinner
- **DoiPreviewDialog.vue**: Matched fields preview with confirm/cancel
- **AttachmentUploader.vue**: Drag-and-drop, 50MB limit, type validation, progress bar
- **AchievementList.vue**: Three-tab layout (active/draft/invalidated), filters, paginated table
- **AchievementDetail.vue**: Basic info tab, attachments tab, action bar
- **Pinia store**: useAchievementStore for form state management
- **Router**: /achievement/register, /achievement/list, /achievement/detail/:id

### Configuration
- WebMvcConfig: CORS for development
- .gitignore: auto-generated d.ts files excluded

## Test Results

| Test Suite | Tests | Status |
|-----------|-------|--------|
| PaperServiceTest | 6 | PASS |
| PaperServiceIntegrationTest | 3 | PASS |
| DoiAutoFillServiceTest | 4 | PASS |
| AchievementRegister spec (vitest) | 9 | PASS |
| **Total** | **22** | **ALL PASS** |

## Deviations from Plan

### Rule 2 — Missing DOI controller endpoint
The plan specified GET `/api/doi/lookup` but the initial PaperController was mapped to `/api/papers`. A separate DoiController was created at the correct `/api/doi/lookup` path.

### Rule 3 — Module dependency fix
Achievement-paper needed a dependency on achievement-integration for DoiController (uses DoiAutoFillService). Added to pom.xml.

### Rule 1 — Syntax fix
`Wrappers.lambdaQuery<Paper>()` was parsed by javac as comparison operators on older compiler resolution. Changed to `new LambdaQueryWrapper<Paper>()`.

### TDD Compliance
RED/GREEN gate sequence verified:
1. `test(01-01): add failing tests for paper service and DOI auto-fill service` — RED
2. `feat(01-01): implement paper backend with DOI auto-complete integration` — GREEN
3. `test(01-01): add frontend component test for achievement registration` — RED
4. `feat(01-01): implement frontend for paper registration with DOI auto-complete` — GREEN

## Commits

| # | Hash | Message |
|---|------|---------|
| 1 | 4d84106 | test(01-01): add failing tests for paper service and DOI auto-fill service |
| 2 | 791243a | feat(01-01): implement paper backend with DOI auto-complete integration |
| 3 | 01126b6 | test(01-01): add frontend component test for achievement registration |
| 4 | 0c58e7d | feat(01-01): implement frontend for paper registration with DOI auto-complete |
| 5 | d1a95fd | feat(01-01): wire database migrations, attachment module, and integration tests |

## Self-Check: PASSED
