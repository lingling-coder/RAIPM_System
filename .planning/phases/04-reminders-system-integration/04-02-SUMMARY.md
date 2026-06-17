---
phase: 04-reminders-system-integration
plan: 02
subsystem: achievement-reminder
type: execute
tags: [reminder, task-generation, scheduler, notification]
dependency_graph:
  requires:
    - "04-01 (ReminderConfig entities, mappers, services, enums, template util)"
    - "Phase 2 (FeePlanGenerationTask scheduler pattern, AlertRecordServiceImpl patterns)"
    - "achievement-system (NotificationService, SysUserMapper)"
  provides:
    - "ReminderTaskService with batch generation, user-facing query/mutate"
    - "ReminderGenerationTask (3 AM daily scheduler with Redis lock + idempotency)"
    - "ReminderTaskController (6 endpoints for user task management)"
    - "generateTasks() → ReminderConfigService CRUD (via config reads)"
    - "generateTasks() → NotificationService.send() (in-app notification delivery)"
    - "confirmReceipt() → frontend REMINDER tab confirm button"
  affects:
    - "notification table (new REMINDER type notifications)"
    - "Redis keys (notify:unread:{userId} via NotificationService)"
tech-stack:
  added:
    - "None (all within existing achievement-reminder module)"
  patterns:
    - "Scheduler + Redis lock + idempotency key (FeePlanGenerationTask pattern)"
    - "Batch generation with dedup check (AlertRecordServiceImpl pattern)"
    - "Ownership-verified confirmReceipt and dismiss"
    - "Dual-assignment routing: userId + roleCode per D-05"
key-files:
  created:
    - "achievement-module/achievement-reminder/src/main/java/com/institute/achievement/reminder/dto/ReminderTaskDTO.java"
    - "achievement-module/achievement-reminder/src/main/java/com/institute/achievement/reminder/dto/ReminderTaskVO.java"
    - "achievement-module/achievement-reminder/src/main/java/com/institute/achievement/reminder/service/ReminderTaskService.java"
    - "achievement-module/achievement-reminder/src/main/java/com/institute/achievement/reminder/service/impl/ReminderTaskServiceImpl.java"
    - "achievement-module/achievement-reminder/src/main/java/com/institute/achievement/reminder/scheduler/ReminderGenerationTask.java"
    - "achievement-module/achievement-reminder/src/main/java/com/institute/achievement/reminder/controller/ReminderTaskController.java"
  modified: []
decisions: []
metrics:
  duration: "~20 min"
  completed_date: "2026-06-17"
---

# Phase 04 Plan 02: Reminder Task Generation Engine Summary

Implements the core engine that scans enabled reminder configs at 3 AM daily and batch-generates reminder_task records with deadline calculation, dual-assignment routing (userId + roleCode per D-05), template variable substitution, and in-app notification delivery to responsible users. Also implements user-facing task management endpoints for listing, confirming receipt, dismissing, and querying high-urgency tasks for the global popup feature.

## Changes

### Task 1: ReminderTask DTOs, Service Interface, and Implementation

**Created:**
- `ReminderTaskDTO.java` — Query DTO with configId, userId, typeCode, urgency, status, startDate/endDate range filters.
- `ReminderTaskVO.java` — View object with all entity fields plus resolved typeCode/typeName, userName, deptName, configTitle, urgencyLabel, and recalculated daysUntilDeadline. Includes `recalcDaysUntilDeadline()` and `resolveUrgencyLabel()`/`resolveTypeName()` helper methods.
- `ReminderTaskService.java` — Interface with 8 methods: `generateTasks`, `listByUser`, `listByConfigId`, `getById`, `confirmReceipt`, `dismissTask`, `getHighUrgencyUnconfirmed`, `getUnconfirmedCount`.
- `ReminderTaskServiceImpl.java` — Full implementation with:
  - **`generateTasks(today)`**: Queries enabled configs via `ReminderConfigMapper.findEnabledConfigs()`. For each config, computes deadline (explicit or advanceDays-based), resolves target users via D-05 dual assignment (personal userId + roleCode via `findUserIdsByDeptAndRole`), deduplicates by user ID, checks UNIQUE KEY (configId + userId + deadline) before insert, builds variable map from config fields, substitutes templates using `ReminderTemplateUtil`, inserts `ReminderTask` records, and sends in-app notifications via `NotificationService.send(userId, "REMINDER", ...)`.
  - **`confirmReceipt(taskId, userId)`**: Validates ownership (`task.getUserId().equals(userId)`), sets `confirmedFlag=1` and `confirmedTime=now`. No-op if already confirmed.
  - **`dismissTask(taskId, userId)`**: Validates ownership, logs the dismiss action. No DB state change (frontend-only tracking).
  - **`listByUser(userId, urgency, page, size)`**: Paginated query with optional urgency filter, ordered by deadline ASC with recalculated `daysUntilDeadline`.
  - **`getHighUrgencyUnconfirmed(userId)`**: Uses existing `ReminderTaskMapper.findUnconfirmedHighUrgency()`.
  - **`getUnconfirmedCount(userId)`**: Counts WHERE userId + confirmedFlag=0 + deadline >= today.
  - Deduplication: Pre-insertion `selectCount` check against (configId, userId, deadline) per T-4-02.
  - `@Autowired(required=false)` for optional EmailService with null guard.

### Task 2: ReminderGenerationTask Daily Scheduler

**Created:**
- `ReminderGenerationTask.java` — Scheduled task following exact `FeePlanGenerationTask.java` pattern (D-02):
  - `@Scheduled(cron = "0 0 3 * * ?")` — daily at 3 AM
  - Redis idempotency key: `"reminder:gen:last-run:{today}"` with 2h TTL
  - Redis distributed lock: `"spring:lock:reminder-gen"` with 300s TTL
  - On exception: deletes idempotency key so task retries next day (WR-04 pattern)
  - `finally`: always releases lock key
  - Logs task count and duration on completion
  - Full Javadoc documenting cron timing, idempotency mechanism, and threat model alignment (T-4-02, T-4-04)

### Task 3: ReminderTaskController

**Created:**
- `ReminderTaskController.java` — REST controller at `/api/reminder/tasks` with 6 endpoints:
  1. **GET `/page`** — Paginated listing with optional urgency filter, derives userId from `SecurityUtils.getCurrentUserId()`
  2. **GET `/{id}`** — Single task detail with ownership verification (throws `AchievementException.notAuthorized`)
  3. **PUT `/{id}/confirm`** — Formal read receipt (D-17), calls `confirmReceipt()` with service-layer ownership check
  4. **POST `/{id}/dismiss`** — Dismisses task for frontend tracking, calls `dismissTask()` with ownership check
  5. **GET `/high-urgency-unconfirmed`** — Returns list for high-urgency global popup (D-20)
  6. **GET `/unconfirmed-count`** — Returns count for REMINDER tab badge
  - All endpoints return `Result.success()` wrapper
  - All derive userId from `SecurityUtils.getCurrentUserId()`
  - No `@PreAuthorize` — users can only see their own tasks via service-layer filtering

## Deviations from Plan

None — plan executed exactly as written.

## Verification

### Automated Checks

| Check | Result |
|-------|--------|
| `ReminderTaskServiceImpl` contains `generateTasks` | PASS |
| `ReminderTaskServiceImpl` contains `confirmReceipt` | PASS |
| `ReminderTaskServiceImpl` contains `getHighUrgencyUnconfirmed` | PASS |
| `ReminderGenerationTask` contains `@Scheduled(cron = "0 0 3 * * ?")` | PASS |
| `ReminderGenerationTask` contains `reminder:gen:last-run` | PASS |
| `ReminderTaskController` contains `/api/reminder/tasks` | PASS |
| `ReminderTaskController` contains `high-urgency-unconfirmed` | PASS |
| `ReminderTaskController` contains `confirm` | PASS |

### Acceptance Criteria

- [x] `ReminderTaskServiceImpl.generateTasks()` queries enabled configs, resolves users (personal + role), deduplicates, inserts tasks, sends in-app notifications
- [x] `NotificationService.send()` called with `type="REMINDER"` for each generated task
- [x] `confirmReceipt()` validates ownership, sets `confirmedFlag=1` and `confirmedTime`
- [x] `listByUser()` supports urgency filter, orders by deadline ASC, recalculates `daysUntilDeadline`
- [x] `getHighUrgencyUnconfirmed()` returns unconfirmed HIGH urgency tasks with future deadlines
- [x] `generateTasks()` handles both personal (userId) and role-based (roleCode) assignment per D-05
- [x] `generateTasks()` is `@Transactional`
- [x] EmailService injection uses `@Autowired(required=false)` with null guard
- [x] `@Scheduled(cron = "0 0 3 * * ?")` on main method
- [x] Idempotency check using Redis key `"reminder:gen:last-run:{today}"` with 2h TTL
- [x] Distributed lock using `"spring:lock:reminder-gen"` with 300s TTL
- [x] On exception: idempotency key deleted so task retries next day
- [x] finally: lock key always deleted
- [x] Controller has 6 endpoints: page, getById, confirm, dismiss, high-urgency-unconfirmed, unconfirmed-count
- [x] All endpoints derive userId from `SecurityUtils.getCurrentUserId()`
- [x] All return `Result.success()` wrapper
- [x] Ownership checks in `confirmReceipt`, `dismissTask`, `getById`

## Known Stubs

None.

## Threat Flags

None — all created surface is within the planned threat model.

## Self-Check: PASSED
