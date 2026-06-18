---
phase: 3
plan: 03-04
subsystem: search-frontend
tags: [search, frontend, ui, navigation, filters, highlighting]
requires: [03-03]
affects: [layout, router, user-store, search-results]
tech-stack:
  added: []
  patterns:
    - "Global search box in nav bar (header-right, always visible)"
    - "Compact search results list with keyword highlighting"
    - "Range-based HighlightedText component (no v-html)"
    - "Classification filter gated by CLASSIFIED_MANAGER role (D-16)"
key-files:
  created:
    - achievement-web/src/views/search/SearchResults.vue
    - achievement-web/src/components/dashboard/HighlightedText.vue
    - achievement-web/src/__tests__/search/SearchResults.test.ts
  modified:
    - achievement-web/src/layout/index.vue
    - achievement-web/src/router/index.ts
    - achievement-web/src/store/user.ts
metrics:
  duration: "~45 minutes"
  tasks: 3
  commits: 4
  test_pass_rate: "6/6 search tests"
  build: "pass"
---

# Phase 3 Plan 04: Search Frontend Summary

Global search box in the navigation bar with compact search results page, filters by type/department/year/classification (managers only), keyword highlighting via HighlightedText component, and pagination.

## Key Decisions

- **Search trigger is Enter key only** (D-09): No real-time suggestions or typeahead; button-free search flow that matches search-engine submission style.
- **No focus expansion animation**: Kept at fixed 240px width per D-08 simplicity preference (always visible, no expand required).
- **Department list on userStore**: Added `deptList` ref to `userStore` for the department filter dropdown, initialized as empty array. The plan references it as existing, but it was not present — added as a missing-critical dependency (Rule 2).
- **Status labels mapped from common status codes**: DRAFT/PENDING/APPROVED/REJECTED/etc. mapped to Chinese labels with Element Plus tag type colors.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Dependency] Added deptList to userStore**
- **Found during:** Task 3-04-2
- **Issue:** The plan referenced `userStore.deptList || []` for the department filter dropdown, but the user store did not have a `deptList` property.
- **Fix:** Added `deptList` ref (`Array<{ id: number; deptName: string }>`) to the store state, return value, and resetState function.
- **Files modified:** `achievement-web/src/store/user.ts`

**2. [Rule 2 - Missing Dependency] Fixed vue-router mock in SearchResults test**
- **Found during:** Task 3-04-3
- **Issue:** The `vi.mock('vue-router')` replacement only supplied `useRoute` and `useRouter`, but `@/store/user.ts` imports `@/router` which imports `createRouter` from vue-router. The mock caused module resolution errors.
- **Fix:** Switched to `vi.mock(import('vue-router'), async (importOriginal) => { ... })` with `importOriginal` to preserve all original vue-router exports while overriding the specific functions needed.
- **Files modified:** `achievement-web/src/__tests__/search/SearchResults.test.ts`

### Pre-existing Test Failures

The existing `AchievementRegister.spec.ts` test has 8 pre-existing failures unrelated to this plan:
- `createRouter` missing in its vue-router mock (its mock only provides `useRoute`/`useRouter` but `@/store/user` needs `createRouter` through `@/router`)
- `lookupDoi` missing in its paper API mock

These failures exist when running the test file in isolation and are not caused by this plan's changes.

## Verification Results

- `npx vite build`: PASS (compiled successfully, SearchResults chunk generated)
- `npx vitest run src/__tests__/search/SearchResults.test.ts`: PASS (6/6 tests)

## Task Completion

| Task | Name | Commit | Key Files |
|------|------|--------|-----------|
| 3-04-1 | Add global search box to nav bar + /search route | d1f3211 | layout/index.vue, router/index.ts |
| 3-04-2 | Create search results page with filters, highlighting, pagination | 5f473e2 | SearchResults.vue, HighlightedText.vue, user.ts |
| 3-04-3 | Create SearchResults test file | a6a2510, c43fb13 | SearchResults.test.ts |

## Self-Check: PASSED

Files verified:
- `achievement-web/src/layout/index.vue` — Contains search box in header-right
- `achievement-web/src/router/index.ts` — Contains /search route with hidden: true
- `achievement-web/src/views/search/SearchResults.vue` — Created with filters, list, pagination
- `achievement-web/src/components/dashboard/HighlightedText.vue` — Created with range-based highlighting
- `achievement-web/src/store/user.ts` — Contains deptList ref
- `achievement-web/src/__tests__/search/SearchResults.test.ts` — Created with 6 passing tests

Commits verified: d1f3211, 5f473e2, a6a2510, c43fb13
Build: PASS
Tests: 6/6 PASS (search tests)
