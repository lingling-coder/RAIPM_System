---
phase: 04-reminders-system-integration
plan: 05
subsystem: achievement-reminder
type: execute
wave: 3
status: complete
requirements: [RMD-05, RMD-06]
metrics:
  duration_minutes: 28
  tasks: 3
  files_created: 5
  files_modified: 2
key_decisions:
  - escalationTime field added to ReminderTask entity and V18 migration (not in original V16 schema)
  - ReminderEscalationTask at 5 AM (after generation at 3 AM) following AlertEscalationTask pattern
  - CSS pointer-events: none on overlay for non-blocking modal (RESEARCH.md Pitfall 6 workaround)
  - One task at a time in popup (show first, dismiss to advance to next)
  - In-memory dismissedIds Set for session-level one-time dismiss (D-21)
  - Route change watcher triggers fetchHighUrgencyTasks for login/page-switch auto-show
---

# Phase 4 Plan 5: Escalation State Machine and High-Urgency Popup Summary

**One-liner:** Deadline-relative escalation scheduler (HIGH: -7d, MEDIUM: -3d, LOW: none) with 3-tier NONE→DEPT_HEAD→LEADERSHIP chain, 5-day DEPT_HEAD→LEADERSHIP threshold, and non-blocking high-urgency modal popup with one-time dismiss tracking.

## Completed Tasks

### Task 1: Escalation logic in ReminderTaskService

**Implementation:**
- Added `processEscalations()` to ReminderTaskService interface and implementation
- 3-tier escalation: NONE→DEPT_HEAD (notifies ROLE_SECRETARY)→LEADERSHIP (notifies ROLE_LEADER)
- Urgency-dependent thresholds: HIGH at 7 days before deadline, MEDIUM at 3 days, LOW never escalates
- DEPT_HEAD→LEADERSHIP after 5 days without confirmation (DEPT_HEAD_TO_LEADERSHIP_DAYS)
- Added `escalationTime` (LocalDateTime) field to ReminderTask entity and ReminderTaskVO
- Partial failure tolerance: per-task try/catch, logging for failed notifications
- Transactional annotation on processEscalations() for atomicity

### Task 2: ReminderEscalationTask scheduler and V18 migration

**Files created:**
- `achievement-module/achievement-reminder/src/main/java/com/institute/achievement/reminder/scheduler/ReminderEscalationTask.java`
- `achievement-module/achievement-reminder/src/main/resources/db/migration/V18__add_escalation_time_to_reminder_task.sql`

**Pattern:** Follows exact AlertEscalationTask pattern from Phase 2:
- @Scheduled(cron = "0 0 5 * * ?") — 5 AM daily
- Idempotency key: `reminder:escalation:last-run:{today}` with 2h TTL
- Distributed lock: `spring:lock:reminder-escalation` with 300s TTL
- On exception: delete idempotency key for retry next day
- finally: always release lock

### Task 3: High-urgency popup, store, and layout integration

**Files created:**
- `achievement-web/src/stores/reminder.ts` — Pinia store with highUrgencyTasks, dismissedIds, fetch/dismiss actions
- `achievement-web/src/components/reminder/ReminderUrgencyPopUp.vue` — Non-blocking el-dialog (480px, 15vh top)

**Files modified:**
- `achievement-web/src/layout/index.vue` — Imported and mounted ReminderUrgencyPopUp below router-view

**Key features:**
- CSS pointer-events: none on overlay enables page interaction behind dialog (non-blocking per D-20)
- Auto-shows on login (onMounted) and route change (watcher) for unconfirmed HIGH tasks
- One task at a time in popup; "我知道了" dismisses via backend API + local dismissedIds Set
- One-time dismiss per session per task (D-21) — dismissed tasks still visible in notification center

## Verification

- [x] Backend compiles: all Java files created with correct package structure
- [x] ProcessEscalations handles all 3 urgency levels
- [x] Escalation chain follows NONE→DEPT_HEAD→LEADERSHIP with deadline-relative timing
- [x] ReminderEscalationTask matches AlertEscalationTask pattern (lock, idempotency, error handling)
- [x] V18 migration ALTER TABLE adds escalation_time column
- [x] Store created with proper Pinia composition API pattern
- [x] Popup component with CSS pointer-events workaround for non-blocking behavior
- [x] Layout imports and mounts ReminderUrgencyPopUp

> **⚠ Human verification required:** The non-blocking modal CSS pointer-events technique should be visually verified across Chrome, Edge, and 360 browser to ensure the dialog overlay does not block page interaction. See PLAN.md Task 3 how-to-verify steps 1-9.
