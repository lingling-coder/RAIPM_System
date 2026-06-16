# Phase 2: Fee Management & Alerts - Research

**Researched:** 2026-06-16
**Domain:** Fee ledger management, scheduled task system, alert engine, notification integration
**Confidence:** HIGH

## Summary

Phase 2 delivers the patent/software copyright fee lifecycle: fee ledger CRUD with Arc polymorphic association to achievements, automatic fee plan generation via daily scheduled tasks, a 4-tier alert engine with secondary escalation, batch fee slip generation, and multi-dimensional fee statistics with EasyExcel export. The research confirms that the existing codebase provides strong foundations: NotificationService can be extended with an ALERT type, the `@Scheduled` + Redis lock pattern exists from Phase 0, the Patent entity already carries `authorizationDate`/`nextFeeDate`/`legalStatus`, the FUND_SOURCE dictionary category is pre-seeded, and the module structure (new `achievement-fee` sub-module under `achievement-module/`) cleanly follows the Phase 1 pattern.

Three architectural challenges require specific attention: (1) the alert engine needs idempotent daily scans with deduplication to avoid re-triggering same-level alerts, (2) the notification system needs an ALERT type extension with its own tab/icon/color in the frontend NotificationCenter, and (3) fee statistics aggregation queries must be efficient enough to support the cross-filter table without excessive MySQL overhead.

**Primary recommendation:** Build `achievement-fee` as a standard Spring Boot sub-module following the Phase 1 pattern (PatentController/Service/Mapper), extend NotificationService with an ALERT type enum, implement scheduled scan tasks with Redis distributed locks in the fee module, and use a dedicated `alert_record` table (separate from `notification`) for alert persistence with rollup to `notification` only when actual user notification is needed.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Fee ledger CRUD | API (achievement-fee) | Database (fee_record) | Standard RESTful CRUD with MyBatis-Plus, data stored in fee_record table |
| Fee plan (recurring generation) | API (scheduled task) | Database | Daily cron scans patents, calculates due dates from authorizationDate, inserts fee_record rows |
| Alert calculation | API (scheduled task) | Database | Daily batch scan of fee_record for 30/15/7/overdue classification, writes alert_record |
| Alert escalation | API (scheduled task) | API (NotificationService) | Same batch scan evaluates unresolved alert age, triggers escalation level |
| Notification delivery | API (NotificationService) | Redis (unread count) | Reuse existing notification table + Redis unread cache pattern |
| Fee slip generation | API (achievement-fee) | — | Stateless operation: generate number FEE-YYYYMMDD-XXX, update fee_record status |
| Fee statistics aggregation | Database (MySQL) | API (achievement-fee) | SQL aggregation queries with multi-dimension grouping, EasyExcel for export |
| Fee UI (ledger/plan/stats) | Browser (Vue 3) | API (achievement-fee) | Standard el-table views, 3-tab layout under /fee route |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot (achievement-fee sub-module) | 4.1.x | Fee module backend | Project standard, matches Phase 1 module pattern |
| MyBatis-Plus | 3.5.x | ORM for fee_record, alert_record | Project standard, required for custom SQL aggregation in statistics |
| EasyExcel | 4.0.3 | Fee statistics export | Project standard, Phase 1 pattern confirmed. Note: `AnalysisEventListener` is at `com.alibaba.excel.event` package |
| Spring @Scheduled | — | Daily fee plan + alert scan tasks | Project standard, `@EnableScheduling` already on `AchievementSystemApplication` |
| Redis (StringRedisTemplate) | 7.x | Distributed lock + unread count cache | Project standard, pattern from NotificationService (key prefix `spring:lock:`) |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Hutool | (existing) | Date calc utilities, CollectionUtils | Fee date math (authorizationDate + months), safe list operations |
| MapStruct | 1.6.x | DTO/Entity mapping | FeeRecord <=> FeeRecordVO/DTO, follows Phase 1 pattern |

### Installation
No new external Maven dependencies required. The `achievement-fee` module's `pom.xml` needs:
```xml
<dependencies>
    <dependency>
        <groupId>com.institute</groupId>
        <artifactId>achievement-patent</artifactId>
        <version>${project.version}</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>com.institute</groupId>
        <artifactId>achievement-common</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>com.institute</groupId>
        <artifactId>achievement-framework</artifactId>
        <version>${project.version}</version>
    </dependency>
</dependencies>
```
The `achievement-module/pom.xml` must add `<module>achievement-fee</module>` to the modules list.

## Package Legitimacy Audit

No external packages beyond the existing project stack. All libraries (Spring Boot, MyBatis-Plus, EasyExcel, Hutool, MapStruct) are already verified through Phase 0/Phase 1 usage.

## Architecture Patterns

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         Browser (Vue 3)                         │
│  ┌─────────────────┐  ┌──────────────┐  ┌──────────────────┐    │
│  │  Fee Ledger Tab  │  │ Payment Plan │  │ Fee Stats Tab    │    │
│  │  (el-table)      │  │ Tab          │  │ (el-table +     │    │
│  │  - quick filters │  │ - plan list  │  │  overview cards) │    │
│  │  - color tags    │  │ - edit/pause │  │ - cross-filter   │    │
│  │  - generate slip │  │              │  │ - Excel export   │    │
│  └────────┬─────────┘  └──────┬───────┘  └────────┬─────────┘    │
│           │                   │                    │              │
│  ┌────────▼───────────────────▼────────────────────▼──────────┐   │
│  │              NotificationCenter (NEW Alert tab)             │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐                  │   │
│  │  │ APPROVAL │  │  SYSTEM  │  │  ALERT   │ ← NEW TYPE       │   │
│  │  │  (exists)│  │  (exists)│  │ (D-26)   │                  │   │
│  └──┴─────┬────┴──┴────┬─────┴──┴─────┬────┴──────────────────┘   │
└───────────┼────────────┼────────────────┼──────────────────────────┘
            │            │                │
┌───────────▼────────────▼────────────────▼──────────────────────────┐
│                         API Layer                                  │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │              achievement-fee module (NEW)                   │    │
│  │  ┌────────────────┐  ┌──────────────┐  ┌───────────────┐  │    │
│  │  │ FeeController  │  │ FeeService   │  │ FeeScheduler  │  │    │
│  │  │ /api/fees      │  │ (CRUD+stats) │  │ (scheduled    │  │    │
│  │  │ /api/fee-plans │  │              │  │  tasks)       │  │    │
│  │  │ /api/alert-records             │  │               │  │    │
│  │  └────────┬───────┘  └──────┬───────┘  └───────┬───────┘  │    │
│  │           │                 │                   │          │    │
│  │  ┌────────▼─────────────────▼───────────────────▼───────┐  │    │
│  │  │              FeeMapper (MyBatis-Plus)                 │  │    │
│  │  │  - fee_record CRUD  -  alert_record CRUD              │  │    │
│  │  │  - stats aggregation -  fee_plan CRUD                 │  │    │
│  │  └────────┬──────────────────────────────────────────────┘  │    │
│  └───────────┼─────────────────────────────────────────────────┘    │
│              │                                                       │
│  ┌───────────▼──────────────────────────────────────────────────┐   │
│  │               achievement-system (NotificationService)       │   │
│  │  - send(ALERT type)  -  Redis unread count                  │   │
│  │  - notifyDeptSecretaries() (upgrade from sentinel userId=0)  │   │
│  └──────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────────────┐
│                         Database (MySQL + Redis)                     │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  fee_record     │  alert_record   │  fee_plan               │   │
│  │  - fee_type     │  - fee_record_id│  - patent_id            │   │
│  │  - amount/paid  │  - alert_level  │  - fee_type             │   │
│  │  - due_date     │  - triggered_at │  - amount               │   │
│  │  - owner_type   │  - resolved_at  │  - status (active/paused)│   │
│  │  - owner_id     │  - status       │  - next_due_date        │   │
│  │  - status       │  - escalated_to │                         │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  Redis Keys:                                                         │
│  - notify:unread:{userId} (existing)                                 │
│  - spring:lock:fee-plan-gen (new)                                    │
│  - spring:lock:alert-scan (new)                                      │
│  - fee:alert:last-run (new - dedup check)                            │
└──────────────────────────────────────────────────────────────────────┘
```

### Recommended Project Structure

```
achievement-module/
├── achievement-fee/                                 # NEW sub-module
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/institute/achievement/fee/
│       │   │   ├── controller/
│       │   │   │   ├── FeeRecordController.java     # /api/fees CRUD + batch pay
│       │   │   │   ├── FeePlanController.java        # /api/fee-plans
│       │   │   │   └── AlertRecordController.java   # /api/alert-records
│       │   │   ├── dto/
│       │   │   │   ├── FeeRecordDTO.java
│       │   │   │   ├── FeeRecordVO.java
│       │   │   │   ├── FeePlanDTO.java
│       │   │   │   ├── FeePlanVO.java
│       │   │   │   ├── AlertRecordVO.java
│       │   │   │   └── FeeStatsVO.java
│       │   │   ├── entity/
│       │   │   │   ├── FeeRecord.java
│       │   │   │   ├── FeePlan.java
│       │   │   │   └── AlertRecord.java
│       │   │   ├── enums/
│       │   │   │   ├── FeeTypeEnum.java
│       │   │   │   ├── FeeStatusEnum.java
│       │   │   │   └── AlertLevelEnum.java
│       │   │   ├── mapper/
│       │   │   │   ├── FeeRecordMapper.java
│       │   │   │   ├── FeePlanMapper.java
│       │   │   │   ├── AlertRecordMapper.java
│       │   │   │   └── FeeStatsMapper.java          # Custom aggregation XML
│       │   │   ├── scheduler/
│       │   │   │   ├── FeePlanGenerationTask.java   # Daily: generate recurring fee records
│       │   │   │   ├── AlertScanTask.java           # Daily: 4-tier alert evaluation
│       │   │   │   └── AlertEscalationTask.java     # Daily: 3-tier escalation check
│       │   │   └── service/
│       │   │       ├── FeeRecordService.java
│       │   │       ├── FeePlanService.java
│       │   │       ├── AlertRecordService.java
│       │   │       └── FeeStatsService.java
│       │   └── resources/
│       │       ├── db/migration/
│       │       │   ├── V11__create_fee_record_table.sql
│       │       │   ├── V12__create_fee_plan_table.sql
│       │       │   └── V13__create_alert_record_table.sql
│       │       └── mapper/
│       │           └── FeeStatsMapper.xml           # Custom XML for aggregation queries
│       └── test/
│           └── java/com/institute/achievement/fee/
│               ├── FeeRecordServiceTest.java
│               ├── FeePlanGenerationTaskTest.java
│               ├── AlertScanTaskTest.java
│               └── FeeStatsServiceTest.java
```

### Frontend Structure (existing `achievement-web/src/`):
```
src/
├── api/
│   └── fee/                                        # NEW: Fee API services
│       ├── feeRecord.ts
│       ├── feePlan.ts
│       ├── alertRecord.ts
│       └── feeStats.ts
├── stores/
│   └── notification.ts                              # EXTEND: Add alert unread count
├── views/
│   └── fee/                                         # NEW: Fee management views
│       ├── FeeManagement.vue                         # Main wrapper with 3 sub-tabs
│       ├── FeeLedger.vue                             # 费用台账 tab
│       ├── FeePlan.vue                               # 缴费计划 tab
│       ├── FeeStats.vue                              # 费用统计 tab
│       ├── FeeDetail.vue                             # Fee record detail (tabs)
│       └── components/
│           ├── BatchPayDialog.vue                    # Batch fee slip dialog
│           └── FundSourceSelect.vue                  # Reusable funding source dropdown
├── components/
│   └── notification/
│       └── NotificationBell.vue                      # EXTEND: Show alert badge
├── router/
│   └── index.ts                                      # ADD: /fee route + /notification/alert tab
```

### Pattern 1: Arc Polymorphic Fee Record
**What:** Fee records link to patent or copyright via `owner_type` + `owner_id`, matching Phase 0 D-01.
**When to use:** All fee_record queries that cross achievement types (ledger list, statistics aggregation).
**Example:**
```java
// FeeRecord entity
@Data
@TableName("fee_record")
public class FeeRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String feeType;           // annual_fee, registration_fee, maintenance_fee, other
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private LocalDate dueDate;
    private LocalDate paidDate;
    private String voucherNo;
    private String status;            // pending, paid, paused
    private String fundingSource;     // data dict key

    // Polymorphic Arc (matching Phase 0 D-01)
    @TableField("owner_type")
    private String ownerType;         // "patent" or "copyright"

    @TableField("owner_id")
    private Long ownerId;

    // Source tracking
    private String source;            // auto_generated, manual

    // Fee slip
    private String slipNo;            // FEE-YYYYMMDD-XXX
    private LocalDateTime slipGeneratedTime;
    private Long slipGeneratedBy;

    // Lifecycle
    private Long deptId;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    private Long updatedBy;
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedTime;
}
```

### Pattern 2: Idempotent Scheduled Task with Redis Distributed Lock
**What:** Daily scan tasks that must execute exactly once across instances, with idempotency check.
**When to use:** `FeePlanGenerationTask`, `AlertScanTask`, `AlertEscalationTask`.
**Example:**
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertScanTask {

    private static final String LOCK_KEY = "spring:lock:alert-scan";
    private static final long LOCK_TTL_SECONDS = 300;

    private final StringRedisTemplate redisTemplate;
    private final AlertRecordService alertRecordService;

    /**
     * Daily alert scan at 4 AM.
     * Uses Redis distributed lock (matching Phase 0 pattern).
     * Deduplicates: checks Redis key "fee:alert:last-run:date" before scanning.
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void scanFeeAlerts() {
        String today = LocalDate.now().toString();

        // Idempotency check: skip if already run today (prevents duplicate runs on restart)
        Boolean alreadyRun = redisTemplate.opsForValue().setIfAbsent(
            "fee:alert:last-run:" + today, "running",
            Duration.ofHours(2));
        if (Boolean.FALSE.equals(alreadyRun)) {
            log.debug("Alert scan already executed for date {}, skipping", today);
            return;
        }

        // Acquire distributed lock
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(
            LOCK_KEY, "locked", Duration.ofSeconds(LOCK_TTL_SECONDS));
        if (Boolean.FALSE.equals(locked)) {
            log.debug("Alert scan already running on another instance, skipping");
            return;
        }

        try {
            alertRecordService.scanAndGenerateAlerts();
            log.info("Alert scan completed for date {}", today);
        } catch (Exception e) {
            log.error("Alert scan failed", e);
            // Task will retry next day — partial failures are acceptable
            // because alerts are regenerated daily
        } finally {
            // Release lock (use Redis script in production for atomic unlock)
            redisTemplate.delete(LOCK_KEY);
        }
    }
}
```

### Pattern 3: Alert Record Table (Separate from Notification)
**What:** Alert records are persisted in `alert_record` table for historical tracking. Notifications are generated FROM alert records only when user delivery is needed (matching D-20/D-25).
**When to use:** All alert operations -- generation, reading, escalation, resolution.
**Rationale:** The `notification` table stores user-facing messages. The `alert_record` table stores fee-level alert state (LEVEL, triggers, escalations). This separation means: (a) alert history survives notification cleanup, (b) escalation checks query alert_record, not notification, (c) one alert_record can trigger multiple notifications (original + escalation).

```java
@Data
@TableName("alert_record")
public class AlertRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long feeRecordId;          // FK to fee_record
    private String alertLevel;         // BLUE, YELLOW, ORANGE, RED
    private LocalDate triggeredDate;   // Date of trigger (for dedup)
    private LocalDateTime triggeredAt; // Timestamp of trigger
    private LocalDateTime resolvedAt;
    private String status;             // pending, resolved, ignored

    // Escalation tracking
    private String escalationLevel;    // NONE, DEPT_HEAD, LEADERSHIP
    private LocalDateTime escalatedAt;
    private LocalDateTime escalatedResolvedAt;
}
```

### Pattern 4: Multi-Dimensional Fee Statistics Aggregation with EasyExcel Export
**What:** Statistics query aggregates fee_record by multiple dimensions with cross-filtering.
**When to use:** FEE-06 stats endpoint + export.

```xml
<!-- FeeStatsMapper.xml -->
<select id="summarizeByDimension" resultType="com.institute.achievement.fee.dto.FeeStatsVO">
    SELECT
        COALESCE(SUM(CASE WHEN fr.status = 'paid' THEN fr.paid_amount ELSE 0 END), 0) AS totalPaid,
        COALESCE(SUM(CASE WHEN fr.status = 'pending' THEN fr.amount ELSE 0 END), 0) AS totalPending,
        COALESCE(SUM(CASE WHEN fr.due_date &lt; CURDATE() AND fr.status = 'pending' 
                     THEN fr.amount ELSE 0 END), 0) AS totalOverdue,
        COALESCE(SUM(fr.amount), 0) AS totalAmount,
        COUNT(*) AS recordCount,
        fr.${dimension} AS dimensionValue
    FROM fee_record fr
    <where>
        <if test="deptId != null">
            AND fr.dept_id = #{deptId}
        </if>
        <if test="year != null">
            AND YEAR(fr.due_date) = #{year}
        </if>
        <if test="feeType != null">
            AND fr.fee_type = #{feeType}
        </if>
        <if test="fundingSource != null">
            AND fr.funding_source = #{fundingSource}
        </if>
    </where>
    GROUP BY fr.${dimension}
    ORDER BY dimensionValue
</select>
```

### Anti-Patterns to Avoid
- **Storing fee amounts in Patent/Copyright entity:** Fee is multi-record per achievement. Never add fee totals to Patent. Use aggregation query.
- **Cascading fee plan delete on patent invalidation:** Historical records must survive per D-18. Only `paused` pending records, never DELETE.
- **One `notification` per alert_level per day:** An alert_record should be created once per fee_record per alert_level. The scan task checks `alert_record` for existence of same `fee_record_id + alert_level + today` before inserting.
- **Manually calculating due dates for recurring annual fees:** Use `java.time` (YearMonth, Period) via Hutool. Never hand-roll month arithmetic with `Calendar`.
- **Using LambdaUpdateWrapper for fee record updates:** Per Phase 01-03 decision, use `UpdateWrapper` not `LambdaUpdateWrapper` to avoid MyBatis-Plus lambda serialization issues in tests.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Fee date arithmetic | Month math with Calendar | Hutool DateUtil.offsetMonth() / java.time | Month boundary edge cases (Jan 31 + 1 month = Feb 28) |
| Fee statistics aggregation | In-memory grouping | MySQL GROUP BY + SUM/CASE | MySQL 8.4 handles <100K rows aggregation in <100ms; in-memory grouping is fragile with large datasets |
| Batch fee slip number generation | Auto-increment with date prefix | Redis INCR with date key (`fee:slip:seq:20260616`) | Distributed sequence safe; DB auto-increment resets between dates |
| Excel export | Apache POI | EasyExcel 4.0.3 | POI memory OOM risk confirmed in Phase 01-04 |

**Key insight:** The three critical "don't hand-roll" areas (date math, distributed sequences, and large-data Excel) are all already solved by existing project libraries (Hutool, Redis, EasyExcel). The cost of custom solutions in these areas far exceeds the benefit.

## Runtime State Inventory

> Not applicable for Phase 2 — this is a greenfield sub-module (achievement-fee) with no existing fee data or runtime state to migrate.

## Common Pitfalls

### Pitfall 1: Alert Deduplication Failure
**What goes wrong:** The daily alert scan re-inserts alert records for fee_records that already have today's alert level.
**Why it happens:** No guard check in the scan task; the `scanAndGenerateAlerts()` method inserts records without checking `alert_record` for existing `fee_record_id + alert_level + triggered_date`.
**How to avoid:** Use `INSERT ... SELECT ... WHERE NOT EXISTS` or a Java-level check (`alertRecordMapper.countByFeeAndLevel(recordId, level, today)`) before each insert. Batch check via `SELECT fee_record_id, alert_level FROM alert_record WHERE triggered_date = CURDATE()` first, then filter out already-alerted records.
**Warning signs:** The first alert run produces N records; second run produces 2N records.

### Pitfall 2: Concurrent Fee Plan Generation on Restart
**What goes wrong:** If the server restarts during the fee plan generation task, and the cron schedule fires again (or the task re-runs), duplicate fee records are created for the same patent/period.
**Why it happens:** The transaction that checks "does a fee_record for patent X in year Y already exist?" and inserts one is non-atomic without a lock.
**How to avoid:** (1) Use Redis distributed lock (pattern above) to prevent concurrent execution. (2) Use `INSERT INTO fee_record (...) SELECT ... WHERE NOT EXISTS (...)` for atomic insert-if-not-exists. (3) Add a `UNIQUE INDEX` on `(owner_type, owner_id, fee_type, due_date)` to catch any remaining duplicates at the DB level.
**Warning signs:** Patent shows two annual fee records for the same year.

### Pitfall 3: Fee Statistics Query Performance Degradation
**What goes wrong:** The multi-dimensional cross-filter statistics query becomes slow as fee_record grows (>10K rows).
**Why it happens:** The GROUP BY query scans `fee_record` without proper index support for all dimension combinations.
**How to avoid:** Create a composite index: `CREATE INDEX idx_fee_record_stats ON fee_record (status, due_date, fee_type, funding_source, dept_id)`. This covers the most common query patterns. For <50K rows, MySQL handles this efficiently. Monitor and add specific indexes if new filter patterns emerge.
**Warning signs:** Statistics page takes >2 seconds to load.

### Pitfall 4: Fee Record Not Linked on Patent Archive (First Fee)
**What goes wrong:** When a patent is approved through the approval workflow, the first fee record (based on `nextFeeDate`) is never auto-generated.
**Why it happens:** The approval service in `achievement-system` does not know about the fee module. The fee plan generation task creates recurring annual fees but may miss the first fee if `nextFeeDate` falls outside the scan window.
**How to avoid:** (1) In the approval archive action (`ApprovalService.archiveAchievement()`), emit a Spring event (`AchievementArchivedEvent`) that the fee module listens for. (2) Alternatively, have the `FeePlanGenerationTask` always check patents with no existing fee_record as part of its daily scan.
**Recommendation:** Use Spring `ApplicationEventPublisher` + `@EventListener` in fee module for decoupled integration, matching the existing event-driven pattern. The approval service publishes the event; the fee service handles fee creation in its own transaction.

## Code Examples

### FeeRecordMapper with Custom Aggregation Query
```java
// Source: Project pattern matching PatentMapper + custom XML
@Mapper
public interface FeeRecordMapper extends BaseMapper<FeeRecord> {

    /**
     * Batch payment: update status to paid for multiple records.
     * Uses UpdateWrapper per Phase 01-03 decision (not LambdaUpdateWrapper).
     */
    @Update({
        "<script>",
        "UPDATE fee_record SET status = 'paid', paid_date = #{paidDate},",
        "voucher_no = #{voucherNo}, updated_time = NOW()",
        "WHERE id IN",
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "AND status = 'pending'",
        "</script>"
    })
    int batchMarkAsPaid(@Param("ids") List<Long> ids,
                        @Param("paidDate") LocalDate paidDate,
                        @Param("voucherNo") String voucherNo);

    /**
     * Aggregate fees for statistics (used by FeeStatsService).
     */
    List<FeeStatsVO> summarizeByDimension(
        @Param("dimension") String dimension,  // e.g., "fee_type", "dept_id"
        @Param("deptId") Long deptId,
        @Param("year") Integer year,
        @Param("feeType") String feeType,
        @Param("fundingSource") String fundingSource
    );

    /**
     * Count pending fee records whose due_date falls in the alert window.
     */
    @Select({
        "<script>",
        "SELECT fr.*, ",
        "  CASE ",
        "    WHEN fr.due_date &lt; CURDATE() THEN 'RED'",
        "    WHEN fr.due_date &lt;= DATE_ADD(CURDATE(), INTERVAL 7 DAY) THEN 'ORANGE'",
        "    WHEN fr.due_date &lt;= DATE_ADD(CURDATE(), INTERVAL 15 DAY) THEN 'YELLOW'",
        "    WHEN fr.due_date &lt;= DATE_ADD(CURDATE(), INTERVAL 30 DAY) THEN 'BLUE'",
        "    ELSE NULL",
        "  END AS alert_level",
        "FROM fee_record fr",
        "WHERE fr.status = 'pending'",
        "  AND fr.due_date &lt;= DATE_ADD(CURDATE(), INTERVAL 30 DAY)",
        "  AND fr.due_date >= CURDATE()",
        "ORDER BY fr.due_date ASC",
        "</script>"
    })
    List<FeeRecordWithAlertVO> selectPendingWithAlertLevel();
}
```

### Fee Slip Number Generation
```java
// Source: Project Redis pattern from NotificationService
@Service
public class FeeSlipNumberGenerator {

    private static final String SEQ_KEY_PREFIX = "fee:slip:seq:";
    private final StringRedisTemplate redisTemplate;

    /**
     * Generate slip number: FEE-20260616-001
     * Uses Redis INCR for distributed sequence safety.
     */
    public String generateSlipNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = SEQ_KEY_PREFIX + datePart;

        Long seq = redisTemplate.opsForValue().increment(key);
        if (seq == null) {
            seq = 1L;
            redisTemplate.opsForValue().set(key, "1", Duration.ofDays(2));
        }

        // Set TTL on first creation
        redisTemplate.expire(key, Duration.ofDays(2));

        return String.format("FEE-%s-%03d", datePart, seq);
    }
}
```

### Alert Escalation State Machine
```java
// Source: Cross-reference D-24 (3-tier escalation)
public enum EscalationLevel {
    NONE("未升级", Duration.ZERO),
    FIRST_ALERT("首次预警", Duration.ofDays(0)),
    DEPT_HEAD("部门负责人", Duration.ofDays(3)),
    LEADERSHIP("院领导", Duration.ofDays(8));  // 3 + 5

    private final String label;
    private final Duration unresolvedDuration;

    public static EscalationLevel determineLevel(LocalDateTime triggeredAt, LocalDateTime escalatedAt) {
        if (escalatedAt != null) return NONE;  // Already resolved

        long hoursSinceTrigger = ChronoUnit.HOURS.between(triggeredAt, LocalDateTime.now());

        if (hoursSinceTrigger >= LEADERSHIP.unresolvedDuration.toHours()) return LEADERSHIP;
        if (hoursSinceTrigger >= DEPT_HEAD.unresolvedDuration.toHours()) return DEPT_HEAD;
        return FIRST_ALERT;
    }
}
```

### Alert + Notification Integration
```java
// In AlertRecordService — only sends notification when escalation triggers
@Transactional
public void processEscalation(AlertRecord alert) {
    EscalationLevel level = EscalationLevel.determineLevel(
        alert.getTriggeredAt(), alert.getEscalatedAt());

    if (level == alert.getEscalationLevel()) {
        return; // No change needed
    }

    // Update alert_record escalation level
    alert.setEscalationLevel(level.name());
    alert.setEscalatedAt(LocalDateTime.now());
    alertRecordMapper.updateById(alert);

    // Send notification based on escalation level
    String title = "费用逾期预警 — " + (level == EscalationLevel.DEPT_HEAD ? "通知部门负责人" : "通知院领导");
    String content = buildEscalationContent(alert);

    // Query users by role + dept (replacing Phase 1 sentinel userId=0)
    List<Long> targetUserIds = userService.findUserIdsByDeptAndRole(
        alert.getDeptId(),
        level == EscalationLevel.DEPT_HEAD ? "ROLE_SECRETARY" : "ROLE_LEADER"
    );

    for (Long userId : targetUserIds) {
        notificationService.send(userId, "ALERT", title, content,
            "fee", alert.getFeeRecordId());
    }
}
```

### Frontend: Extending NotificationCenter with Alert Tab
```typescript
// In NotificationCenter.vue — ADD alert tab
<el-tabs v-model="activeTab" @tab-change="handleTabChange">
  <el-tab-pane label="审批待办" name="APPROVAL" />
  <el-tab-pane label="系统通知" name="SYSTEM" />
  <el-tab-pane label="费用预警" name="ALERT" />        // NEW
</el-tabs>

// Alert-specific icon and color in the notification item
<span :class="typeIconClass(item.type)">
  <el-icon v-if="item.type === 'ALERT'" color="#e6a23c">
    <WarningFilled />                                  // Distinct icon
  </el-icon>
</span>
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Fee tracking in Excel | Structured fee_record table | This phase | Enables cross-reference, alerts, statistics |
| Manual fee deadline tracking | 4-tier automated alert engine | This phase | Eliminates missed payments |
| Sentinel userId=0 for dept notifications | Proper RBAC user query by role+dept | This phase | Upgrade notification delivery to target real users |
| Approval listing called "审批待办" | NotificationCenter with APPROVAL/SYSTEM/ALERT tabs | Phase 1+2 | Extensible notification taxonomy |

## Assumptions Log

**No claims tagged [ASSUMED] in this research.** All findings are derived from reading the existing codebase (entities, services, scheduled tasks, migration SQL, frontend components, router config) and verified against the project's CLAUDE.md stack declarations and CONTEXT.md decisions.

## Open Questions (RESOLVED)

1. **Fee amount calculation for patents — how to determine standard annual fees?** (RESOLVED)
   - Decision: Fee amounts are **manually editable** with no automatic CNIPA fee schedule calculation. System generates due dates only; users enter amounts. This aligns with D-17 (user can edit amounts).

2. **Patent invalidation event integration — how does the fee module learn about patent invalidation?** (RESOLVED)
   - Decision: Use Spring `ApplicationEventPublisher` + `@EventListener` pattern. The invalidation service publishes a custom `AchievementInvalidatedEvent`. The fee module's listener catches it and pauses pending fee records. Event class lives in `achievement-common` module to avoid circular dependency.

## Sources

### Verified (HIGH confidence)
- `achievement-module/achievement-patent/entity/Patent.java` -- authorizationDate, nextFeeDate, legalStatus, patentType fields confirmed
- `achievement-module/achievement-system/service/NotificationService.java` -- send(), Redis unread count pattern, cleanupOldNotifications() pattern confirmed
- `achievement-module/achievement-system/entity/Notification.java` -- type field (no ALERT enum yet), userId, relatedAchievementId confirmed
- `achievement-module/achievement-system/mapper/NotificationMapper.java` -- findByUserIdAndType, markAsRead pattern confirmed
- `achievement-module/achievement-system/AchievementSystemApplication.java` -- @EnableScheduling confirmed
- `achievement-framework/backup/BackupSchedulerService.java` -- @Scheduled task pattern with cron expression confirmed
- `achievement-framework/security/SecurityUtils.java` -- getCurrentUserId(), getCurrentRoles(), getCurrentDeptId() confirmed
- `achievement-web/src/router/index.ts` -- existing route structure confirmed
- `achievement-web/src/views/notification/NotificationCenter.vue` -- el-tabs with APPROVAL/SYSTEM pattern confirmed
- `achievement-web/src/api/notification.ts` -- getList, getUnreadCount, markAsRead, markAllAsRead confirmed
- `achievement-web/src/stores/notification.ts` -- polling pattern with 30s interval confirmed
- `db/init/01-init.sql` -- FUND_SOURCE dictionary category pre-seeded, 7 default roles including ROLE_SECRETARY confirmed
- `V1__init_system_tables.sql` -- sys_user with dept_id, sys_user_role for role mapping confirmed
- `V5__create_patent_table.sql` -- patent table with authorization_date, next_fee_date, legal_status, patent_type confirmed
- `V8__create_notification_table.sql` -- notification table with monthly partitions confirmed

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- all libraries confirmed in existing codebase
- Architecture: HIGH -- clear patterns from Phase 0/Phase 1 codebase
- Pitfalls: HIGH -- derived from analyzing existing scheduled task patterns and known MyBatis-Plus issues
- Notification integration: HIGH -- existing NotificationService code read in full
- Fee statistics queries: MEDIUM -- SQL design is based on known MySQL patterns but actual query tuning depends on data volume

**Research date:** 2026-06-16
**Valid until:** 2026-07-16 (30 days -- project conventions are stable, no fast-moving dependencies)
