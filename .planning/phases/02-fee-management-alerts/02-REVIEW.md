---
phase: 02-fee-management-alerts
reviewed: 2026-06-16T14:30:00Z
depth: standard
files_reviewed: 27
files_reviewed_list:
  - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/controller/AlertRecordController.java
  - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/controller/FeeRecordController.java
  - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/dto/AlertQueryDTO.java
  - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/dto/AlertRecordVO.java
  - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/dto/FeeRecordDTO.java
  - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/dto/FeeRecordQueryDTO.java
  - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/dto/FeeRecordVO.java
  - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/entity/AlertRecord.java
  - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/entity/FeeRecord.java
  - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/enums/AlertLevelEnum.java
  - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/enums/EscalationLevel.java
  - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/enums/FeeStatusEnum.java
  - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/enums/FeeTypeEnum.java
  - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/mapper/AlertRecordMapper.java
  - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/mapper/FeeRecordMapper.java
  - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/scheduler/AlertEscalationTask.java
  - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/scheduler/AlertScanTask.java
  - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/AlertRecordService.java
  - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/FeeRecordService.java
  - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/FeeSlipNumberGenerator.java
  - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/impl/AlertRecordServiceImpl.java
  - achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/impl/FeeRecordServiceImpl.java
  - achievement-module/achievement-system/src/main/java/com/institute/achievement/module/system/mapper/SysUserMapper.java
  - achievement-module/achievement-system/src/main/java/com/institute/achievement/module/system/service/NotificationService.java
  - achievement-web/src/api/fee/alertRecord.ts
  - achievement-web/src/api/fee/feeRecord.ts
  - achievement-web/src/api/fee/index.ts
  - achievement-web/src/views/fee/FeeLedger.vue
  - achievement-web/src/views/fee/components/BatchPayDialog.vue
  - achievement-web/src/views/notification/NotificationCenter.vue
findings:
  critical: 3
  warning: 6
  info: 5
  total: 14
status: issues_found
---

# Phase 2: Code Review Report -- Fee Management & Alerts

**Reviewed:** 2026-06-16T14:30:00Z
**Depth:** standard
**Files Reviewed:** 27
**Status:** issues_found

## Summary

This review covers the fee management subsystem: fee record CRUD, batch payment workflow (slip generation + batch pay), 4-tier alert generation (BLUE/YELLOW/ORANGE/RED), escalation state machine, notification routing, and the corresponding Vue 3 frontend components.

Three blocker-level issues were identified: (1) the batch payment flow has a fundamental data-integrity problem where individually generated slip numbers are silently overwritten by a single batch slip number, (2) both scheduled tasks set their idempotency guard before acquiring the distributed lock, creating a 2-hour execution blackout if an instance crashes mid-task, and (3) partial updates in `FeeRecordServiceImpl.update()` unconditionally set `funding_source` to whatever the client sends, including null, which silently clears existing data.

Six warnings cover: dead or misleading code (`notifySubmitter`), inconsistency-risk state ordering (escalation level updated before notification delivery), missing pagination bounds, unreadable notification fallback for copyrights, mutable-list mutation crash on null deptId, and unused enum thresholds. Five informational items are also noted.

---

## Critical Issues

### CR-01: Batch payment overwrites individually generated slip numbers (data integrity loss)

**Files:**
- `achievement-web/src/views/fee/components/BatchPayDialog.vue:199`
- `achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/impl/FeeRecordServiceImpl.java:235-250`
- `achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/mapper/FeeRecordMapper.java:95-114`

**Issue:** The two-step batch payment flow has a contradiction between step 1 (`batchGenerateSlips`) and step 2 (`batchPay`).

Step 1 (`batchGenerateSlips`, `FeeRecordServiceImpl.java:198`) generates a unique slip number per fee record via `FeeSlipNumberGenerator.generateSlipNo()` and persists each individually via `updateSlipNo(id, slipNo, userId)` (FeeRecordMapper line 124-131). This correctly stores distinct slip numbers for each record.

Step 2 (`batchPay`, `FeeRecordServiceImpl.java:235`) calls `batchMarkAsPaid` (FeeRecordMapper line 95-114), which sets ALL selected records to a single `slipNo` value via the SQL `UPDATE ... SET slip_no = #{slipNo} ... WHERE id IN (...)`.

The frontend at BatchPayDialog.vue line 199 sends only the first slip number:

```typescript
slipNo: slipNumbers.value[0] || '',
```

This means that if 3 records had individual slips `FEE-20260616-001`, `FEE-20260616-002`, `FEE-20260616-003` generated in step 1, step 2 overwrites all three to `FEE-20260616-001`. The individually generated numbers for records 2 and 3 are permanently lost in the database, and the corresponding Redis counter sequence is wasted (the incremented keys are rolled back/never used).

**Fix:** The `batchMarkAsPaid` SQL must NOT overwrite `slip_no`, `slip_generated_time`, or `slip_generated_by` since those were already correctly set during `batchGenerateSlips`. Remove these columns from the UPDATE:

```sql
UPDATE fee_record SET
  status = 'paid',
  paid_date = #{paidDate},
  voucher_no = #{voucherNo},
  updated_time = NOW()
WHERE id IN
<foreach item='id' collection='ids' open='(' separator=',' close=')'>
  #{id}
</foreach>
  AND status = 'pending'
```

---

### CR-02: Idempotency guard set before distributed lock creates 2-hour execution blackout on crash

**Files:**
- `achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/scheduler/AlertScanTask.java:54-70`
- `achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/scheduler/AlertEscalationTask.java:56-70`

**Issue:** In both scheduled tasks, the idempotency Redis key (`fee:alert:last-run:YYYY-MM-DD`) is set with `setIfAbsent` (2-hour TTL) BEFORE the distributed lock is acquired (5-minute TTL):

```java
// Step 1: Idempotency guard (set first)
Boolean alreadyRun = redisTemplate.opsForValue().setIfAbsent(
    "fee:alert:last-run:" + today, "running", Duration.ofHours(2));
if (Boolean.FALSE.equals(alreadyRun)) {
    return;
}

// Step 2: Distributed lock (set second)
Boolean locked = redisTemplate.opsForValue().setIfAbsent(
    LOCK_KEY, "locked", Duration.ofSeconds(LOCK_TTL_SECONDS));
if (Boolean.FALSE.equals(locked)) {
    return;
}
```

If Instance A sets the idempotency key, acquires the lock, but then crashes (OOM, SIGKILL during deployment) before releasing the lock, Instance B on the next scheduled run will:
1. Check idempotency key -- it exists (TTL = 2 hours) -- skip
2. Never execute the actual work

Result: the daily scan/escalation won't execute for up to 2 hours. For `AlertScanTask` (alert generation), this could mean a day's alerts are entirely missed. For `AlertEscalationTask` (escalation), this delays escalation by a day.

**Fix:** Set the idempotency key AFTER successful work completion, not before. The distributed lock alone is sufficient for concurrency control:

```java
@Scheduled(cron = "0 0 4 * * ?")
public void scanAlerts() {
    String today = LocalDate.now().toString();

    // Acquire lock first
    Boolean locked = redisTemplate.opsForValue().setIfAbsent(
            LOCK_KEY, "locked", Duration.ofSeconds(LOCK_TTL_SECONDS));
    if (Boolean.FALSE.equals(locked)) {
        log.debug("Alert scan already running on another instance, skipping");
        return;
    }

    try {
        int alertCount = alertRecordService.scanAndGenerateAlerts();
        // Set idempotency key only after successful completion
        redisTemplate.opsForValue().set(
            "fee:alert:last-run:" + today, String.valueOf(alertCount),
            Duration.ofHours(2));
        log.info("Alert scan completed for {}: {} alerts generated", today, alertCount);
    } catch (Exception e) {
        log.error("Alert scan failed for date {}", today, e);
    } finally {
        redisTemplate.delete(LOCK_KEY);
    }
}
```

Apply the same pattern to `AlertEscalationTask.processEscalations()`.

---

### CR-03: Partial update unconditionally sets nullable field to null, erasing existing data

**File:** `achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/impl/FeeRecordServiceImpl.java:83-97`

**Issue:** The `update()` method builds an `UpdateWrapper` that unconditionally calls `.set("funding_source", dto.getFundingSource())` for every update request. In `FeeRecordDTO`, `fundingSource` is nullable (no `@NotNull` annotation). If a client sends an update payload without `fundingSource` (e.g., a PATCH-style partial update that only changes `status`), the `fundingSource` field defaults to `null`, and the SQL unconditionally sets `funding_source = NULL`, silently erasing the previously stored value.

```java
// Lines 83-97: These are ALWAYS set
UpdateWrapper<FeeRecord> uw = new UpdateWrapper<FeeRecord>()
    .eq("id", id)
    .set("amount", dto.getAmount())                     // @NotNull, always present
    .set("funding_source", dto.getFundingSource());      // NULLABLE -- nullifies column

// These fields have proper null guards:
if (dto.getPaidAmount() != null) { uw.set("paid_amount", dto.getPaidAmount()); }
if (dto.getVoucherNo() != null) { uw.set("voucher_no", dto.getVoucherNo()); }
if (dto.getStatus() != null) { uw.set("status", dto.getStatus()); }
```

The inconsistency is clear: `paidAmount`, `voucherNo`, and `status` are guarded against null, but `amount` and `fundingSource` are not. The `amount` field is protected by `@NotNull` on the DTO, but `fundingSource` has no such protection.

**Fix:** Add a null guard for `fundingSource`, consistent with the pattern used for the other optional fields. Also consider whether `amount` should follow the same pattern (even though it is currently `@NotNull`, future changes to the DTO could remove that constraint):

```java
// Whitelisted fields (T-02-01-02), only set non-null values
if (dto.getAmount() != null) {
    uw.set("amount", dto.getAmount());
}
if (dto.getFundingSource() != null) {
    uw.set("funding_source", dto.getFundingSource());
}
```

If the business requirement is that `fundingSource` should always be provided on update, add `@NotNull` to the DTO field and document the API contract accordingly.

---

## Warnings

### WR-01: Escalation level thresholds are defined but never used

**File:** `achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/enums/EscalationLevel.java:30-31, 39-43, 83-92`

**Issue:** Each `EscalationLevel` enum value defines a `threshold` Duration field (`Duration.ofDays(3)` for DEPT_HEAD, `Duration.ofDays(8)` for LEADERSHIP) that is accessible via getter, but is NEVER referenced in the `determineNextLevel()` method. Instead, `determineNextLevel` uses hardcoded magic numbers `72` and `192` (hours):

```java
public static EscalationLevel determineNextLevel(String currentLevelCode, long hoursSinceTrigger) {
    if (hoursSinceTrigger >= 192 && !LEADERSHIP.getCode().equals(currentLevelCode)) {
        return LEADERSHIP;
    }
    if (hoursSinceTrigger >= 72 && (NONE.getCode().equals(currentLevelCode)
            || FIRST_ALERT.getCode().equals(currentLevelCode))) {
        return DEPT_HEAD;
    }
    return null;
}
```

If the escalation thresholds ever change (e.g., from 3 days to 5 days for DEPT_HEAD), both the enum constructor argument AND the hardcoded constant in `determineNextLevel` must be updated. This creates a maintenance trap where the enum values are decorative but the real logic lives in disconnected magic numbers.

**Fix:** Derive threshold comparisons from the enum's `threshold` field:

```java
public static EscalationLevel determineNextLevel(String currentLevelCode, long hoursSinceTrigger) {
    if (hoursSinceTrigger >= LEADERSHIP.getThreshold().toHours()
            && !LEADERSHIP.getCode().equals(currentLevelCode)) {
        return LEADERSHIP;
    }
    if (hoursSinceTrigger >= DEPT_HEAD.getThreshold().toHours()
            && (NONE.getCode().equals(currentLevelCode)
                || FIRST_ALERT.getCode().equals(currentLevelCode))) {
        return DEPT_HEAD;
    }
    return null;
}
```

---

### WR-02: `notifySubmitter()` is a no-op -- provides false sense of functionality

**File:** `achievement-module/achievement-system/src/main/java/com/institute/achievement/module/system/service/NotificationService.java:194-199`

**Issue:** The `notifySubmitter` method is a stub that only logs and never sends any notification:

```java
public void notifySubmitter(Long achievementId, String type, String message) {
    log.info("Submitter notification queued: achievementId={}, type={}, message='{}'",
            achievementId, type, message);
}
```

The Javadoc says "For Phase 1, the caller provides the userId or this is done directly" but callers of this method will silently receive no notification. This is dead code that erodes trust in the notification API. If this method is ever called in a production flow, expected notifications will be silently lost.

**Fix:** Either (a) implement the method (load the achievement's `created_by` from DB, then call `send()`), (b) remove it entirely, or (c) annotate with `@Deprecated` and throw `UnsupportedOperationException` to fail fast:

```java
@Deprecated
public void notifySubmitter(Long achievementId, String type, String message) {
    throw new UnsupportedOperationException(
        "notifySubmitter is not implemented. Use send(userId, ...) directly.");
}
```

---

### WR-03: Escalation level updated in DB before notification delivery succeeds

**File:** `achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/impl/AlertRecordServiceImpl.java:248-307` (processEscalations) and `322-388` (processSingleEscalation)

**Issue:** In both escalation methods, the alert record's `escalationLevel` is updated in the database BEFORE notifications are sent:

```java
// Line 252-254: Update escalation level FIRST
alert.setEscalationLevel(targetLevel.getCode());
alert.setEscalatedAt(LocalDateTime.now());
alertRecordMapper.updateById(alert);

// Line 257+: Load fee record and send notifications SECOND
FeeRecord feeRecord = feeRecordMapper.selectById(alert.getFeeRecordId());
// ... build content, send notifications ...
```

If `notificationService.send()` throws an exception (e.g., Redis connection failure, DB constraint violation), the escalation level has already been committed. The alert record shows "escalated to DEPT_HEAD" but no notification reached the department head. The transaction rollback from the exception will revert the update...  Actually, since the method is `@Transactional`, a thrown exception DOES roll back the escalation level update. The issue is narrower: the catch blocks at lines 309-312 and the try-per-alert loop pattern means individual failures are caught and logged, preventing transaction rollback.

Specifically, the per-alert `try { ... } catch (Exception e) { log.error(...) }` at lines 309-312 catches the exception and prevents the enclosing `@Transactional` method from rolling back. So if the escalation level update succeeds but the notification send fails, the level is persisted but notifications are not sent. The alert record is in an inconsistent state.

**Fix:** Reorder the operations within the try block so notifications are sent first, then the escalation level is updated. If notification send fails, the exception propagates to the catch block, which continues the loop (the escalation level hasn't been updated yet, so the state remains consistent):

```java
for (AlertRecord alert : pendingAlerts) {
    try {
        // ... determine targetLevel ...
        if (targetLevel == null) continue;

        // Load fee record
        FeeRecord feeRecord = feeRecordMapper.selectById(alert.getFeeRecordId());
        if (feeRecord == null) continue;

        // SEND NOTIFICATIONS FIRST
        if (EscalationLevel.DEPT_HEAD.equals(targetLevel)) {
            // ... send DEPT_HEAD notifications ...
        } else if (EscalationLevel.LEADERSHIP.equals(targetLevel)) {
            // ... send LEADERSHIP notifications ...
        }

        // THEN update the alert record
        alert.setEscalationLevel(targetLevel.getCode());
        alert.setEscalatedAt(LocalDateTime.now());
        alertRecordMapper.updateById(alert);

    } catch (Exception e) {
        log.error("Failed to escalate alert id={}: {}", alert.getId(), e.getMessage());
    }
}
```

---

### WR-04: `List.of()` immutability causes `UnsupportedOperationException` on null deptId

**Files:**
- `achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/impl/AlertRecordServiceImpl.java:275,360`
- `achievement-module/achievement-system/src/main/java/com/institute/achievement/module/system/service/NotificationService.java:211-214`

**Issue:** `NotificationService.findUserIdsByDeptAndRole()` returns `List.of()` (an immutable empty list) when `deptId` is null:

```java
// NotificationService.java:212-214
if (deptId == null) {
    log.warn("findUserIdsByDeptAndRole called with null deptId");
    return List.of();  // Immutable!
}
```

Both `processEscalations` (line 275) and `processSingleEscalation` (line 360) call `userIds.addAll(adminIds)` on the result:

```java
List<Long> userIds = notificationService.findUserIdsByDeptAndRole(
        alert.getDeptId(), "ROLE_SECRETARY");
List<Long> adminIds = notificationService.findUserIdsByDeptAndRole(
        alert.getDeptId(), "ROLE_DEPT_ADMIN");
userIds.addAll(adminIds);  // CRASH if deptId was null -- List.of() is immutable
```

If an alert record has a null `deptId` (DB corruption, migration edge case), this will throw `UnsupportedOperationException`. While the exception is caught at lines 309-312, it results in a logged error and escalation silently skipped for that alert. This is a latent crash risk in a data-integrity edge case.

**Fix:** Either (a) ensure `deptId` is never null at the DB level (`NOT NULL` constraint on `alert_record.dept_id`), or (b) use `new ArrayList<>()` instead of `List.of()` in the sentinel return:

```java
// NotificationService.java:214 -- return mutable list
return new ArrayList<>();
```

Also add a DB-level NOT NULL constraint on `alert_record.dept_id` as defense-in-depth.

---

### WR-05: No upper bound on pagination `size` parameter

**Files:**
- `achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/controller/AlertRecordController.java:44-48`
- `achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/controller/FeeRecordController.java:102-111`

**Issue:** Both paginated endpoints accept `page` and `size` via `@RequestParam(defaultValue = "1")` / `@RequestParam(defaultValue = "20")` without any validation. A client could request:
- `page = 0` (MyBatis-Plus generates `LIMIT 20 OFFSET 0` which is harmless, but `page` semantics are 1-based per Javadoc)
- `size = 10000` (loads 10K+ records into memory -- OOM risk on large datasets)
- Negative values (unlikely to pass Spring type conversion, but no explicit guard)

The project's threat model mentions T-02-01-04 ("Paginated queries limit result set size"), but no upper bound is enforced at the controller or service level.

**Fix:** Add `@Min` / `@Max` validation annotations:

```java
@GetMapping("/page")
public Result<Page<AlertRecordVO>> pageAlertRecords(
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        ...
```

---

### WR-06: `resolveOwnerName` fallback produces unreadable notification content for copyrights

**File:** `achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/impl/AlertRecordServiceImpl.java:438-452`

**Issue:** For fee records with `ownerType = 'copyright'`, `resolveOwnerName` returns a raw technical identifier instead of a human-readable name:

```java
// For copyrights and fallback, return a generic identifier
return feeRecord.getOwnerType() + "#" + feeRecord.getOwnerId();
```

This directly affects user-facing notification content (line 128: `"费用预警 -- " + ownerName`), which would display as "费用预警 -- copyright#42" to recipients. The alert record paginated listing correctly resolves owner names via SQL JOIN, but the notification delivery path does not use that JOIN.

**Fix:** Inject `SoftwareCopyrightMapper` into `AlertRecordServiceImpl` (following the existing `PatentMapper` pattern) and resolve the copyright name analogously:

```java
private String resolveOwnerName(FeeRecord feeRecord) {
    if ("patent".equals(feeRecord.getOwnerType()) && feeRecord.getOwnerId() != null) {
        Patent patent = patentMapper.selectById(feeRecord.getOwnerId());
        if (patent != null && patent.getPatentName() != null) {
            return patent.getPatentName();
        }
    } else if ("copyright".equals(feeRecord.getOwnerType()) && feeRecord.getOwnerId() != null) {
        SoftwareCopyright copyright = copyrightMapper.selectById(feeRecord.getOwnerId());
        if (copyright != null && copyright.getName() != null) {
            return copyright.getName();
        }
    }
    return feeRecord.getOwnerType() + "#" + feeRecord.getOwnerId();
}
```

---

## Info

### IN-01: Edit dialog sends dummy `ownerType` and `ownerId` values

**File:** `achievement-web/src/views/fee/FeeLedger.vue:488-489`

The `saveEdit()` function sends `ownerType: ''` and `ownerId: 0` in the update payload:

```typescript
await feeApi.update(currentEditId.value, {
    feeType: editForm.value.feeType,
    amount: editForm.value.amount,
    ownerType: '',     // dummy value
    ownerId: 0,        // dummy value
    // ...
})
```

The backend ignores these fields in the `UpdateWrapper` (only whitelisted fields are set per T-02-01-02), so there is no functional impact. However, sending dummy values is misleading and fragile -- if the backend DTO validation changes, these dummy values could cause errors. Remove them.

---

### IN-02: Paginated single-record lookup pattern is needlessly complex

**File:** `achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/impl/AlertRecordServiceImpl.java:179-193`

The `getById()` method queries the full paginated JOIN view with `new Page<>(1, 1)` and then filters in Java:

```java
Page<AlertRecordVO> pageResult = alertRecordMapper.selectAlertPage(
        new Page<>(1, 1), query);
return pageResult.getRecords().stream()
        .filter(vo -> vo.getId().equals(id))
        .findFirst()
        .orElseThrow(() -> AchievementException.notFound("预警记录", id));
```

This runs a paginated query with no WHERE clause, potentially scanning the entire alert_record table (sorted by triggered_at DESC), then filters 1 result in-memory. As the table grows, this query becomes progressively less efficient. Consider adding a direct SQL lookup: `SELECT ... FROM alert_record WHERE ar.id = #{id}` with the same JOINs.

---

### IN-03: `FeeRecordVO.getById()` does not populate label fields

**File:** `achievement-module/achievement-fee/src/main/java/com/institute/achievement/fee/service/impl/FeeRecordServiceImpl.java:107-113`

The `getById()` method uses `toVO(record)` which maps entity fields directly to the VO. The `FeeRecordVO` has computed `getStatusLabel()` and `getFeeTypeLabel()` methods (defined at lines 56-68 of `FeeRecordVO.java`), but the frontend might expect these fields to be populated via bean serialization rather than calling getter methods. This depends on the serialization configuration (Jackson will call getters by default), so this may work correctly. If the frontend accesses `row.feeTypeLabel` and finds `null` when it expects a label, the fallback `scope.row.feeTypeLabel || scope.row.feeType` in `FeeLedger.vue` line 99 handles it gracefully.

---

### IN-04: `paidDate` not validated in BatchPayDialog before API call

**File:** `achievement-web/src/views/fee/components/BatchPayDialog.vue:185-208`

The `confirmPay()` function validates `voucherNo` for non-empty but does not validate `paidDate`:

```typescript
async function confirmPay() {
    if (!voucherNo.value.trim()) {
        ElMessage.warning('请输入缴费凭证号')
        return
    }
    // paidDate is never checked
```

While `paidDate` is initialized with `new Date().toISOString().slice(0, 10)` and reset by the watcher, if the date picker malfunctions or the user clears it, an empty string could be sent to the backend. Add a validation guard:

```typescript
if (!paidDate.value) {
    ElMessage.warning('请选择缴费日期')
    return
}
```

---

### IN-05: TypeScript `any` types in multiple frontend files

**Files:**
- `achievement-web/src/views/fee/FeeLedger.vue:266,268,269,380,385,411,417`
- `achievement-web/src/views/fee/components/BatchPayDialog.vue:172,194`
- `achievement-web/src/views/notification/NotificationCenter.vue:74,99`

Several variables and function parameters are typed as `any` rather than using the defined TypeScript interfaces (`FeeRecordVO`, `FeeRecordPageParams`, etc.). For example, `editForm` at line 268, `dateRange` at line 266, and the API response `res: any` at lines 380/385/FeeLedger.vue. These suppress type checking and bypass compile-time error detection. Replace `any` with the appropriate interfaces where possible.

---

_Reviewed: 2026-06-16T14:30:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
