---
phase: 01
slug: achievement-registration-approval
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-06-16
---

# Phase 01 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework (Backend)** | JUnit 5 + Mockito |
| **Framework (Frontend)** | Vitest |
| **Config file** | `pom.xml` (Surefire plugin) / `vitest.config.ts` |
| **Quick run command** | `mvn test -pl achievement-module/achievement-paper -DskipITs` |
| **Full suite command** | `mvn verify -Pall-tests && npx vitest run` |
| **Estimated runtime** | ~180 seconds (backend) + ~60 seconds (frontend) |

---

## Sampling Rate

- **After every task commit:** Run `mvn test -pl <affected-module> -DskipITs` or `npx vitest run src/views/__tests__/<affected>.spec.ts`
- **After every plan wave:** Run `mvn verify -Pall-tests && npx vitest run`
- **Phase gate:** Full suite green before execution summary
- **Max feedback latency:** 240 seconds

---

## Per-Task Verification Map

### Plan 01-01 — Paper Registration & DOI Auto-Complete (Wave 1)

| Task | Requirement | Threat Ref | Test Type | Automated Command | File |
|------|-------------|------------|-----------|-------------------|------|
| Task 1: Backend paper module + DOI integration | REG-01, REG-04, API-01 | T-01-01~07 | unit + integration | `mvn test -Dtest=PaperServiceTest,DoiAutoFillServiceTest` | ❌ W0 |
| Task 2: Frontend paper registration + list + detail | REG-01, REG-06 | — | component | `npx vitest run src/views/__tests__/AchievementRegister.spec.ts` | ❌ W0 |
| Task 3: Frontend DOI auto-complete + preview dialog | REG-04 | T-01-07 | component | `npx vitest run src/views/__tests__/DoiAutoComplete.spec.ts` | ❌ W0 |

### Plan 01-02 — Patent & Software Copyright Registration (Wave 2)

| Task | Requirement | Threat Ref | Test Type | Automated Command | File |
|------|-------------|------------|-----------|-------------------|------|
| Task 1: Backend patent + copyright modules | REG-02, REG-03 | T-01-02-01~05 | unit | `mvn test -Dtest=PatentServiceTest,CopyrightServiceTest` | ❌ W0 |
| Task 2: Frontend patent/copyright forms + list extend | REG-02, REG-03 | — | component | `npx vitest run src/views/__tests__/AchievementRegister.spec.ts` | ❌ W0 |

### Plan 01-03 — 3-Step Approval Workflow & Notifications (Wave 2)

| Task | Requirement | Threat Ref | Test Type | Automated Command | File |
|------|-------------|------------|-----------|-------------------|------|
| Task 1: Approval state machine + audit logging | APPR-01, APPR-02 | T-01-03-01~04 | unit + integration | `mvn test -Dtest=ApprovalServiceTest#testFullWorkflow,ApprovalServiceTest#testAuditLog` | ❌ W0 |
| Task 2: Notification backend + polling | APPR-03 | — | integration | `mvn test -Dtest=NotificationServiceTest#testApprovalNotification` | ❌ W0 |
| Task 3: Frontend approval UI + notification center | APPR-01, APPR-03 | — | component | `npx vitest run src/views/__tests__/ApprovalDetail.spec.ts` | ❌ W0 |

### Plan 01-04 — Batch Import, Attachments & Permissions (Wave 3)

| Task | Requirement | Threat Ref | Test Type | Automated Command | File |
|------|-------------|------------|-----------|-------------------|------|
| Task 1: Batch import (EasyExcel) + template | REG-05 | T-01-04-01~04 | integration | `mvn test -Dtest=BatchImportServiceTest` | ❌ W0 |
| Task 2: Attachment permission + classified access | REG-07, REG-10 | T-01-04-05 | integration | `mvn test -Dtest=AttachmentServiceTest,ClassifiedAccessTest` | ❌ W0 |

### Plan 01-05 — Invalidation, Duplicate Detection & Classified Marking (Wave 4)

| Task | Requirement | Threat Ref | Test Type | Automated Command | File |
|------|-------------|------------|-----------|-------------------|------|
| Task 1: Invalidation service + frontend | REG-08 | — | unit | `mvn test -Dtest=InvalidateServiceTest` | ❌ W0 |
| Task 2: Duplicate detection (submit-time) | REG-09 | T-01-02-05 | unit | `mvn test -Dtest=AchievementServiceTest#testDuplicateDetection` | ❌ W0 |
| Task 3: Classified marking + access control UI | REG-10 | — | unit + component | `mvn test -Dtest=ClassifiedAccessTest` | ❌ W0 |

---

## Wave 0 Requirements

- [ ] `src/test/java/.../paper/PaperServiceTest.java` — covers REG-01
- [ ] `src/test/java/.../patent/PatentServiceTest.java` — covers REG-02
- [ ] `src/test/java/.../copyright/CopyrightServiceTest.java` — covers REG-03
- [ ] `src/test/java/.../integration/doi/DoiAutoFillServiceTest.java` — covers REG-04, API-01
- [ ] `src/test/java/.../batch/BatchImportServiceTest.java` — covers REG-05
- [ ] `src/test/java/.../attachment/AttachmentServiceTest.java` — covers REG-07
- [ ] `src/test/java/.../approval/ApprovalServiceTest.java` — covers APPR-01, APPR-02
- [ ] `src/test/java/.../notification/NotificationServiceTest.java` — covers APPR-03
- [ ] `src/test/java/.../security/ClassifiedAccessTest.java` — covers REG-10
- [ ] Frontend component tests for AchievementRegister, ApprovalDetail, NotificationCenter
- [ ] `pom.xml` — Surefire & Failsafe plugin configuration for test execution
- [ ] `vitest.config.ts` — Vitest configuration

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| DOI auto-complete visual interaction (preview dialog, loading state, fallback behavior) | REG-04 | Requires visual inspection of loading spinner, preview dialog, and toast messages | 1. Enter valid DOI in paper form<br>2. Verify loading spinner appears<br>3. Verify preview dialog shows matched fields<br>4. Click confirm — verify fields filled<br>5. Enter invalid DOI — verify warning toast |
| Approval left-right split layout + timeline rendering | APPR-01 | Requires visual inspection of layout proportions and timeline node rendering | 1. Navigate to approval detail page<br>2. Verify left 60% / right 40% split<br>3. Verify timeline shows all steps with correct icons<br>4. Test pass/reject with reason |
| Type switch confirmation + form reset | D-01, D-09 | Visual + behavioral interaction | 1. Fill paper form partially<br>2. Switch to patent type<br>3. Verify confirmation dialog appears<br>4. Confirm — verify form cleared and patent fields shown |
| Batch import error report download | REG-05 | File download interaction | 1. Upload Excel with 2 invalid rows<br>2. Verify result report shows correct counts<br>3. Click download error report — verify Excel file downloaded |
| 30-day notification cleanup | D-55 | Time-dependent | Acceptance: code review confirms scheduled task + partition drop strategy |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 240s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
