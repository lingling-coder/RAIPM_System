# Phase 2: Fee Management & Alerts - Context

**Gathered:** 2026-06-16
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 2 delivers the patent/software copyright fee management and alert lifecycle, covering fee ledger CRUD, payment plan generation (single + recurring annual), a 4-tier alert engine with secondary escalation, batch fee slip generation, and multi-dimensional fee statistics. This phase builds on the fee-related fields already present in the Patent entity (`nextFeeDate`) and reuses the existing notification center for alert delivery.

**In scope (from ROADMAP):**
- Fee ledger: view patent/software copyright fee records with multi-dimensional filtering
- Payment plan engine: automatic single/recurring annual fee generation, manual edit/pause
- 4-tier alert engine: 30d/15d/7d/overdue alerts with notification triggers
- Secondary escalation: unresolved alerts auto-escalate to department head
- Batch fee slip generation and batch mark-as-paid
- Fee statistics by department, year, patent type, funding source

**Out of scope:**
- Email notification integration (deferred to Phase 4)
- Financial system deep integration (v2)
- Online fee approval workflow (v2 FEE-v2-01)
- Chart-based visualization (Phase 3 Dashboard handles this)
</domain>

<decisions>
## Implementation Decisions

### 模块结构
- **D-01:** 新增 `achievement-fee` 子模块（`achievement-module/achievement-fee/`），与 `achievement-paper`/`achievement-patent`/`achievement-copyright` 同级。费用台账、缴费计划、预警引擎、统计的后端代码均放在此模块中。
- **D-02:** 前端新增独立一级菜单「费用管理」，路由路径 `/fee`。包含三个子标签页：费用台账 / 缴费计划 / 费用统计。

### 费用台账展示与交互
- **D-03:** 费用记录列表使用标准表格视图（el-table），与 Phase 01 成果列表风格一致。支持排序、分页。
- **D-04:** 筛选区采用精简布局 — 默认展示 2-3 个关键筛选器，其余折叠在「高级筛选」按钮中。可选快捷筛选标签：「即将逾期」「已逾期」「本月需缴费」「全部」。
- **D-05:** 行内操作仅包含查看与编辑。标记缴费走批量操作流程。
- **D-06:** 费用记录详情页使用标签页布局：基本信息 / 缴费历史 / 附件 / 操作日志。与成果详情页风格一致。
- **D-07:** 缴费单生成流程：台账勾选记录 → 点击「生成缴费单」→ 自动生成编号（格式 `FEE-YYYYMMDD-XXX`）→ 填写缴费日期和凭证号 → 标记完成。不设「待缴费」中间状态。
- **D-08:** 「经费来源」字段使用下拉选择 + 数据字典配置。管理员可在系统管理 → 数据字典中维护经费来源选项（纵向科研经费 / 横向科研经费 / 院配套 / 自筹等）。
- **D-09:** 费用记录支持上传缴费凭证/票据附件。附件区放在详情页标签页中，复用 Phase 01 的 `AttachmentUploader` 组件。

### 费用记录数据模型
- **D-10:** 费用记录与成果的关联使用多态 Arc 模式（`owner_type` + `owner_id`），与 Phase 0 D-01 保持一致。
- **D-11:** 费用记录的来源采用混合模式：
  - 专利登记/审批通过后，系统根据 `nextFeeDate` 自动生成首条费用记录
  - 年费类费用通过定时任务每日扫描自动生成后续待缴计划
  - 用户可手动添加一次性费用（滞纳金、变更费、登记费等）
- **D-12:** 统一费用模型，通过类型字段区分不同费用种类：`annual_fee`（专利年费）、`registration_fee`（登记费）、`maintenance_fee`（维护费）、`other`（其他）。软著使用后三种类型。
- **D-13:** 费用记录表使用 `fee_record` 表名，包含：`fee_type`（费用类型）、`amount`（金额）、`due_date`（截止日期）、`paid_date`（缴费日期）、`paid_amount`（实缴金额）、`voucher_no`（凭证号）、`status`（待缴费/已缴费/已暂停）、`funding_source`（经费来源）、`owner_type`/`owner_id`（多态关联）。

### 缴费计划引擎
- **D-14:** 周期性专利年费的下次缴费日期基于**授权日**（`authorizationDate`）计算。按月/日对齐授权日。
- **D-15:** 年费计划的生成由定时任务每日凌晨扫描触发。扫描所有已授权且启用的专利，自动生成即将到期的年费待缴记录。
- **D-16:** 缴费完成后**不**自动续期下一年度计划，由定时任务统一管理下年度计划生成。
- **D-17:** 缴费计划编辑管控：
  - 用户可编辑：金额、暂停/恢复计划
  - 用户不可编辑：缴费日期（由系统根据规则锁定）
  - 暂停期间不产生预警
- **D-18:** 专利作废/失效后，系统自动暂停所有关联的待缴费计划。历史缴费记录保留。

### 四级预警与催办
- **D-19:** 预警引擎通过定时任务每日批量扫描运行。扫描所有未缴费且未逾期的费用记录，判断预警等级并生成预警记录。
- **D-20:** 预警记录持久化到 `alert_record` 表，支持历史追溯。包含字段：`fee_record_id`、`alert_level`（预警等级）、`triggered_at`（触发时间）、`resolved_at`（处理时间）、`status`（待处理/已处理/已忽略）。
- **D-21:** 预警展示双重方式：
  - 费用台账表格中通过颜色标签展示预警状态
  - 独立预警中心页面集中查看所有预警记录及处理状态
- **D-22:** 四级预警颜色方案：
  - 30天前 → 蓝色 `ⓘ` 即将缴费
  - 15天前 → 黄色 `⚠` 请尽快缴费
  - 7天前 → 橙色 `🔴` 截止在即
  - 逾期 → 红色 `🚫` 逾期 X 天
- **D-23:** 预警触发时间间隔**硬编码**固定（30天/15天/7天/逾期），不做可配置。预警记录在触发时生成。
- **D-24:** 二次催办采用三级升级路径：
  - 首次预警触发 → 3天未处理 → 通知部门负责人
  - 部门负责人通知后 → 5天仍未处理 → 通知部门主管/院领导
  - 逾期后自动升级为红色状态
- **D-25:** 预警和催办通知仅通过站内消息发送（邮件推迟到 Phase 4）。
- **D-26:** 通知中心新增独立「费用预警」标签页，与「审批待办」「系统通知」并列。预警消息使用独立图标和颜色区分。

### 费用统计
- **D-27:** 费用统计展示方式：顶部概览卡片行（总费用/已缴费/待缴费/逾期金额）+ 下方多维度交叉筛选表格。不使用图表（图表能力由 Phase 3 Dashboard 统一提供）。
- **D-28:** 统计维度包括：部门、年份、专利类型、经费来源。支持四个维度的交叉筛选与展示。
- **D-29:** 统计表格支持 Excel 导出（EasyExcel），导出当前筛选条件下的数据。
- **D-30:** 统计数据后台整合在 `achievement-fee` 模块中，保持费用领域内聚。不抽取独立统计模块。

### Claude's Discretion
本次讨论已覆盖所有灰色地带，用户对所有关键决策做出明确选择。无需要Claude自行判断的领域。

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase Requirements
- `.planning/REQUIREMENTS.md` — FEE-01~FEE-06 (lines 28-35)
- `.planning/ROADMAP.md` — Phase 2 success criteria and plan structure (section "Phase 2: Fee Management & Alerts")

### Project Context & Constraints
- `.planning/PROJECT.md` — Project overview, user roles (7 types), architecture constraints, key decisions
- `.planning/STATE.md` — Current project state

### Prior Phase Decisions
- `.planning/phases/00-foundation-infrastructure/00-CONTEXT.md` — Phase 0 decisions: Arc polymorphic pattern (D-01), RBAC, SQL-layer permission injection, audit log hash chain, file proxy service
- `.planning/phases/01-achievement-registration-approval/01-CONTEXT.md` — Phase 1 UI patterns, notification center design (D-52~D-56), AttachmentUploader component, table-based list views

### Technology Stack
- `CLAUDE.md` — Technology stack versions, architecture constraints, code conventions

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **AttachmentUploader.vue** — Phase 01 attachment upload component, reusable for fee voucher uploads (D-09)
- **NotificationBell.vue + NotificationCenter.vue** — Existing notification system, extend with new "费用预警" tab (D-26)
- **Patent entity (`nextFeeDate` field)** — Existing fee date field on Patent, used as source data for fee plan engine
- **Data dictionary components** (SysDictCategory/SysDictEntry) — Reusable for funding source configuration (D-08)
- **Vue Pure Admin tab layout** — Existing page structure pattern for fee sub-tabs (台账/计划/统计)
- **Audit log framework** — Fee operations (create/edit/pay) should log through the existing audit system

### Established Patterns
- **Controller pattern:** `@Slf4j @RestController @RequestMapping("/api/fees") @RequiredArgsConstructor` — consistent with PatentController
- **Entity pattern:** `@Data @TableName("fee_record")` MyBatis-Plus entity — consistent with existing entities
- **API service pattern:** Typed DTO/VO interfaces in `src/api/fee/` — consistent with `src/api/achievement/patent.ts`
- **Polymorphic Arc pattern:** `owner_type` + `owner_id` — consistent with Phase 0 D-01 (used for fee-achievement relationship D-10)
- **Table + drawer editing:** el-table with el-drawer for edit forms — consistent with Phase 01 patterns

### Integration Points
- **Routing:** Add `/fee` route with children to `achievement-web/src/router/index.ts`, following the existing route definition pattern
- **Notification store:** Extend `achievement-web/src/stores/notification.ts` with alert-fetching logic
- **API integration:** Fee API services go in `achievement-web/src/api/fee/`
- **Scheduled task:** Alert engine and fee plan generation use `@Scheduled` + Redis distributed lock (consistent with Phase 0 API framework)

</code_context>

<specifics>
## Specific Ideas

Phase 2 follows the standard Chinese enterprise management system patterns established in Phase 0 and Phase 01: efficiency-focused table views with compact filters, clear status visibility through color-coded tags, and batch operations for common workflows. The fee alert system introduces a new notification category but otherwise reuses established UI patterns.

</specifics>

<deferred>
## Deferred Ideas

- **邮件预警通知** — 推迟到 Phase 4 邮件集成时统一实现
- **费用线上审批流程** — v2 功能（FEE-v2-01），二期评估
- **图表可视化费用统计** — 由 Phase 3 Dashboard 统一提供图表能力
- **历史欠费追溯查询** — v2 功能（FEE-v2-02），二期评估

</deferred>

---

*Phase: 2-Fee Management & Alerts*
*Context gathered: 2026-06-16*
