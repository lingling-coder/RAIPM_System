---
phase: 04-reminders-system-integration
plan: 03
type: execute
subsystem: achievement-web (frontend)
tags: [reminders, notification-center, read-receipt, frontend]
dependency_graph:
  requires: [04-02]
  provides: [04-04, 04-05]
  affects: [notification-center]
tech-stack:
  added: []
  patterns:
    - "NotificationCenter.vue: add REMINDER tab-pane with reminder item rendering"
    - "reminder-task.ts: frontend API module following api-config.ts pattern"
    - "ReminderConfirmButton.vue: read-receipt confirm component with loading state"
key-files:
  created:
    - achievement-web/src/api/reminder/reminder-task.ts
    - achievement-web/src/components/reminder/ReminderConfirmButton.vue
  modified:
    - achievement-web/src/views/notification/NotificationCenter.vue
decisions:
  - "Icon mapping: TrophyBase -> Medal, DocumentCopy -> CopyDocument, DataAnalysis -> DataBoard (compatibility with @element-plus/icons-vue 2.3.2)"
  - "handleReminderClick marks as read but does NOT auto-confirm (D-17)"
  - "handleConfirmed() updates item state in-place without reloading the list"
metrics:
  duration: ~15 min
  completed: "2026-06-17"
---

# Phase 04 Plan 03: REMINDER Tab and Read Receipt — Summary

One-liner: Add REMINDER tab to notification center with deadline countdown, urgency badges, and ReminderConfirmButton read receipt workflow.

## Changes Made

### Task 1: Create frontend API module for reminder tasks

Created `achievement-web/src/api/reminder/reminder-task.ts` with:

- `ReminderTaskVO` interface matching the backend VO
- `PageParams` interface with optional `urgency` filter
- `page()` — GET `/api/reminder/tasks/page`
- `getById()` — GET `/api/reminder/tasks/{id}`
- `confirmReceipt()` — PUT `/api/reminder/tasks/{id}/confirm`
- `dismissTask()` — POST `/api/reminder/tasks/{id}/dismiss`
- `getHighUrgencyUnconfirmed()` — GET `/api/reminder/tasks/high-urgency-unconfirmed`
- `getUnconfirmedCount()` — GET `/api/reminder/tasks/unconfirmed-count`

Follows the exact pattern of `api/system/api-config.ts`.

### Task 2: Create ReminderConfirmButton read receipt component

Created `achievement-web/src/components/reminder/ReminderConfirmButton.vue` with:

- `el-button type="primary" size="small"` displaying "确认收到" for unconfirmed items
- `el-tag type="success"` displaying "已确认" for confirmed items
- `v-loading` via `:loading` prop to prevent double-click during API call
- Calls `reminderApi.confirmReceipt(props.taskId)` on click
- Emits `confirmed` event with `taskId` on success
- Shows `ElMessage.success('确认成功')` / `ElMessage.error('确认失败，请重试')`

### Task 3: Add REMINDER tab to NotificationCenter.vue

Modified `achievement-web/src/views/notification/NotificationCenter.vue`:

- **Tab**: Added `el-tab-pane name="REMINDER"` after ALERT tab with `Bell` icon and "申报提醒" label
- **Reminder list**: Separate rendering block via `v-if="activeTab === 'REMINDER'"` with reminder items showing:
  - Type-colored icons using `getTypeColor()` (6 type code mappings with specific hex colors)
  - Deadline countdown with color coding per UI-SPEC (>14d gray #606266, 7-14d orange #e6a23c, <7d red #f56c6c)
  - Urgency tags: HIGH=danger, MEDIUM=warning, LOW=primary with Chinese labels
  - `ReminderConfirmButton` for unconfirmed items; "已确认" tag for confirmed items
- **Helpers**: `urgencyTagType()`, `urgencyLabel()`, `getCountdownColor()`, `getCountdownText()`, `getTypeColor()`
- **Empty state**: "暂无申报提醒" for REMINDER tab with `BellFilled` icon
- **Pagination**: Uses `reminderTotal` for total when REMINDER tab active
- **handleConfirmed()**: Updates item state in-place (sets `confirmedFlag = 1`) without full list reload
- **Icons**: `BellFilled`, `Medal`, `WalletFilled`, `CopyDocument`, `DataBoard`, `Lock` from `@element-plus/icons-vue`

## Deviations from Plan

None — plan executed exactly as written.

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Duplicate v-if on template elements**
- **Found during:** Task 3
- **Issue:** Notification and reminder list divs each had two `v-if` directives, which is invalid in Vue 3
- **Fix:** Combined conditions into single `v-if` using `&&` operator
- **Files modified:** `NotificationCenter.vue`
- **Commit:** Included in `df1f1bc`

## Verification

- [x] `reminder-task.ts` exports `confirmReceipt`, `getHighUrgencyUnconfirmed`, and contains `/api/reminder/tasks` references
- [x] `ReminderConfirmButton.vue` contains `confirmReceipt` and "确认收到" strings
- [x] `NotificationCenter.vue` contains `REMINDER`, `reminderApi`, `ReminderConfirmButton`, and "暂无申报提醒"
- [x] All changes committed individually with proper conventional commit format

## Self-Check

All key files created/modified and committed. No missing files or uncommitted changes.

## Commits

- `5b13422` feat(04-03): create reminder task frontend API module
- `1ceebbc` feat(04-03): create ReminderConfirmButton read receipt component
- `df1f1bc` feat(04-03): add REMINDER tab to NotificationCenter with reminder items
