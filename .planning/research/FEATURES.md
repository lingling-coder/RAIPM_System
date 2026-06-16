# Feature Research

**Domain:** 科研成果与知识产权管理系统 (Scientific Research Achievement & IP Management System)
**Researched:** 2026-06-16
**Confidence:** HIGH

## Feature Landscape

### Table Stakes (Users Expect These)

Features users assume exist in any research institute IP management system. Missing these = product feels incomplete or unusable.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| 成果统一登记 (论文/专利/软著) | Core purpose of the system; Excel replacement baseline | MEDIUM | Three separate forms with shared attachment/filing backend. DOI auto-complete is a differentiator, basic manual entry is table stakes. |
| 成果审核与审批流 | Must replicate existing paper-based approval workflows | MEDIUM | "Submit -> Department Secretary -> Admin" is the minimum. Configurable flows (by type/secret level) are expected in any serious system. |
| 附件上传与管理 | Users need to attach certificates, PDFs, receipts | LOW | Version management and online preview push toward differentiator. Basic upload/download is table stakes. |
| 专利年费台账 | Primary pain point — replacing manual Excel fee tracking | MEDIUM | Must show: fee type, amount, due date, paid date, status. Periodic fee auto-generation is expected. |
| 费用预警 (多级提醒) | Prevents patent lapse — core value proposition | MEDIUM | 30-day / 15-day / 7-day / overdue is the industry standard tier. |
| 基础检索 (标题/作者/关键词) | Users need to find their own and colleagues' published work | LOW | Chinese word segmentation and fuzzy matching expected in Chinese research context. |
| 部门数据隔离 | Research institutes have strict department boundaries | MEDIUM | "Your department cannot see my department's data" is mandatory, not optional. |
| 审计日志 (操作留痕) | Mandated by internal audit and compliance requirements | LOW | Record who did what and when. Logs must be non-deletable and retained for at least 5 years. |
| 成果统计看板 (基础) | Leadership needs at-a-glance overview of institute output | MEDIUM | Annual trend, type distribution, department ranking. These are baseline, not value-add. |
| 人员/部门数据同步 (HR对接) | Without this, manual user management becomes unsustainable | MEDIUM | Daily incremental sync from HR system or LDAP. Required for department isolation to work. |
| 成果注销/作废 | Legal requirement for IP that is abandoned or invalidated | LOW | Mark as invalid with reason, keep history. Without this the system has no end-of-life path. |
| 站内消息通知 | Primary communication channel for approvals and alerts | LOW | Must support read/unread, link to relevant record. Without this, users miss critical fee deadlines. |
| RBAC 权限模型 | Foundation for all data security; auditor requirement | MEDIUM | Role-based access + department-level isolation + secret-level isolation. Three layers are the standard. |
| 成果导出 (Excel) | Required for annual reporting to ministry of education | LOW | Users need to export their own data for external reporting. Missing = data lock-in complaint. |
| 统一登录与账号体系 | Users refuse to manage another separate password | LOW | SSO or unified credentials. Without this, adoption drops sharply. |

### Differentiators (Competitive Advantage)

Features that set this system apart from the typical "glorified Excel" or generic OA-based solutions. These align with the Core Value from PROJECT.md.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **DOI 自动补全** (多源切换) | Eliminates manual entry of paper metadata. Users type DOI, system fills title/authors/journal/volume from Crossref/Scopus/OpenAlex with configurable priority. | MEDIUM | High-value because it directly attacks the "data entry is tedious" pain point. Multi-source fallback prevents dependency on single vendor. |
| **专利法律状态同步** (CNIPA 接口) | Automatically syncs legal status, fee deadlines, and maintenance status from China National IP Administration. | HIGH | CNIPA has rate limits and unstable APIs. Requires async queue, rate limiting, and manual fallback. High complexity but huge value — prevents manual tracking errors. |
| **引文分析** (个人/部门/全院三级) | Auto-retrieves citation counts, H-index, impact factors from Scopus/WoS/OpenAlex. Updates monthly with manual refresh option. | HIGH | Requires name disambiguation, ORCID matching, and alias handling. Significant data engineering effort. Differentiates from basic counting systems. |
| **转化全生命周期跟踪** (含收益分配) | Tracks tech transfer, licensing, and equity conversion with revenue distribution tracking. Custom nodes for contract -> payment -> invoicing -> completion. Auto-prompt for benefit follow-up at 3/6/12 months. | HIGH | Most systems stop at "patent granted." Full conversion lifecycle with revenue split tracking is rare. Requires polymorphic relation design (one conversion links to paper/patent/software). |
| **四级预警 + 二次催办** | Not just reminders — escalating urgency. First alert routes to researcher, second alert to department head. System-wide popup for emergency-level items. | MEDIUM | Many systems do basic reminders. Escalation chain with department-head notification is uncommon and directly addresses the "fees slip through cracks" problem. |
| **可配置审批流** (按类型/密级差异化) | Not hardcoded workflow. Admin configures different approval chains for different result types, secret levels, and patent tiers. Supports countersign and add-on approval. | HIGH | Requires BPM-like flow engine or at minimum a configurable node graph. Most competing systems use fixed 3-step flows. Configurable = fewer vendor lock-in complaints. |
| **自定义报表 + 定时推送** | Users build their own report templates (dimensions, metrics, filters) and schedule monthly/quarterly/annual email delivery. | MEDIUM | Most systems have fixed reports. Custom report builder with scheduled push is a strong differentiator for leadership users who need regular stat submissions. |
| **批量缴费单生成** | Select N patents with upcoming fees -> system generates N fee requests in one action -> routes through approval flow. | LOW-MEDIUM | Simple to implement (batch create + multi-insert), but most systems only support single-patent fee creation. Significant time saver for department secretaries. |
| **涉密成果独立权限体系** | Separate permission system for classified/secret results. Only authorized secret-level administrators can access. Complete access log on every view/download. | HIGH | Most systems treat all data uniformly. Independent secret-level permission with encryption-at-rest and full audit is rare. Required by Chinese state secrets regulations. |
| **移动端核心操作** (H5轻应用) | Not a full native app — a responsive H5 that supports: view reminders, approve/reject, check stats, quick search. | MEDIUM | Phase 2, but differentiation vs desktop-only competitors. Focus on approval actions and alert reading — not data entry. |
| **成果与项目自动关联** | When registering a result, user selects which funded project(s) produced it. Enables "project X produced N papers, M patents" analysis. Prevents double-counting for evaluation. | MEDIUM | Bridges the gap between project management and achievement management systems. Most orgs manage these separately. |
| **费用来源多维度统计** (按经费来源分类) | Tracks which funding source (institutional/internal grant/horizontal project/external) paid each fee. Enables cost center reporting. | LOW-MEDIUM | Most fee systems track total paid, not who paid. Department heads need to know which projects bear which patent costs. |

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem like good ideas but create significant problems. The requirements document already calls some of these out — these expand on that analysis.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| **实时全文查重 / 学术不端检测** | "Can the system check if a paper is plagiarized?" | Requires integration with CNKI/Wanfang proprietary APIs at high per-check cost. Duplicate with existing dedicated systems (CNKI Academic Mischeck, Wanfang Wencha). Creates false expectation of comprehensive coverage. | Keep a "external check result upload" field. Link to dedicated plagiarism check systems rather than building in-house. |
| **自动抓取全网成果** | "Why can't it just find all our published papers automatically?" | Name disambiguation is extremely hard (common Chinese names, multiple romanizations, name changes). Automatic crawling produces 60-70% accuracy at best. Users spend more time cleaning up bad matches than they would entering data. | DOI-based semi-automated import: user finds paper, provides DOI, system fills metadata. Trust-but-verify model. |
| **财务系统实时对账** | "Can it sync with the finance system in real-time?" | Finance systems in Chinese research institutes are often legacy (Kingdee, UFIDA customizations) with unstable APIs. Real-time sync creates a tight coupling that breaks whenever finance upgrades. Security concerns about bidirectional access. | Phase 2: periodic batch reconciliation file exchange. One-way import of payment records. Manual voucher upload as primary workflow. |
| **短信网关直接集成** | "Email is not urgent enough — send SMS for fee warnings." | SMS gateway requires telecom contract, monthly fees, template registration with authorities. Chinese SMS regulations (real-name, content filtering) add compliance burden. Low marginal value over email + in-app notification. | Reserve the interface slot in Phase 1. Implement in Phase 2 only if fee overdue rate remains high after Phase 1 launch. |
| **职称评审自动计分** | "Can it calculate promotion scores from achievement data?" | Scoring rules vary by year, department, and promotion type. Mixing achievement management with HR evaluation creates scope creep. System becomes a political battleground over rule definitions. | Export achievement data to a separate evaluation system. Keep this system's focus on "what exists" not "what is worth how many points." |
| **AI自动撰写成果摘要** | "Can AI write the abstract for me?" | Risk of hallucinations and inaccuracies. Users will submit without review. Creates liability for incorrect scientific descriptions. No clear quality gate. | Standard template fields + copy-from-existing buttons. Leave content creation to authors. |
| **期刊投稿状态跟踪** | "Track my manuscript through review process." | This is a separate domain (peer review tracking). Requires integration with ScholarOne/Editorial Manager APIs that are not publicly available. Creeps into "manuscript management system" territory. | The system tracks published/registered results only. Pre-registration is a different workflow. |
| **开放科研数据 (Open Data) 仓库** | "Can we store and share research datasets?" | Dataset storage requirements (size, format, metadata schema) are completely different from bibliographic achievement management. Requires separate infrastructure (object storage, DOI minting, data citation tracking). | Focus on achieving description and linking. If dataset sharing is needed, build a separate system or integrate with existing data repositories (Figshare, Science Data Bank). |

## Feature Dependencies

```
成果登记模块
    ├──requires──> 人员/部门数据同步 (HR/LDAP)
    ├──requires──> 附件存储与管理
    ├──enhances──> DOI自动补全 (可选, 有DOI补全则登记更快)
    └──enhances──> 专利法律状态同步 (可选, 自动填充法律状态)

费用管理模块
    ├──requires──> 成果登记模块 (费用必须关联已登记成果)
    ├──requires──> 费用台账数据结构
    ├──requires──> 预警规则引擎
    └──enhances──> 专利法律状态同步 (自动更新缴费截止日)

审批流程模块
    ├──requires──> 成果登记模块 (审批对象是已提交的成果)
    ├──requires──> 人员/部门数据同步 (审批路由依赖组织架构)
    ├──requires──> 站内消息通知 (审批待办通知)
    └──enhances──> 邮件通知 (可选增强触达)

预警与提醒模块
    ├──requires──> 费用管理模块 (费用到期是主要预警源)
    ├──requires──> 成果登记模块 (年费预警依赖专利登记数据)
    └──enhances──> 站内消息 + 邮件通知 (触达渠道)

统计分析模块
    ├──requires──> 成果登记模块 (分析数据源)
    ├──requires──> 费用管理模块 (费用统计数据源)
    ├──requires──> 转化跟踪模块 (如果启用)
    └──enhances──> 引文分析API (如果启用)

全文检索模块
    ├──requires──> 成果登记模块 (索引对象)
    ├──requires──> 中文分词引擎 (Elasticsearch IK)
    └──requires──> RBAC权限过滤 (检索结果必须遵守权限)

转化跟踪模块
    ├──requires──> 成果登记模块 (转化对象)
    ├──requires──> 权限隔离 (转化数据可能包含商业敏感信息)
    └──enhances──> 费用管理模块 (转化收益可能需要关联费用)

涉密成果管理
    ├──requires──> 独立权限体系 (不能复用普通RBAC)
    ├──requires──> 附件加密存储
    └──requires──> 完整审计日志 (每次访问必须记录)
```

### Dependency Notes

- **成果登记 requires 人员同步:** Department membership is required for data isolation. Without knowing which department a user belongs to, the system cannot enforce the "only see your own department" constraint. This is a hard dependency — Phase 1 must include at minimum a department import mechanism, even if LDAP integration is deferred.
- **费用管理 requires 成果登记:** Each fee record must link to an existing patent or software copyright record. This is a database foreign key dependency — you cannot create a fee without the parent record existing.
- **审批流程 requires 成果登记:** The approval engine processes submissions. The "submit" action transitions a result from "draft" to "pending review." Without the result record, there is nothing to approve.
- **统计分析 requires 成果登记 + 费用管理:** Aggregate queries draw from multiple entity tables. The statistics module is a read-only consumer — its data quality depends entirely on upstream data entry quality.
- **转化跟踪 requires 成果登记:** A conversion (tech transfer, license) must reference an existing paper, patent, or software copyright. The polymorphic relation (`relation_type` + `relation_id`) must exist before conversion records can be created.
- **预警模块 requires 费用管理:** Fee deadline calculations are the primary trigger for alerts. Without fee records with due dates, the alert engine has nothing to compute against.
- **全文检索 conflicts with 涉密数据:** Elasticsearch indexes everything unless explicitly separated. Secret-level results must be indexed in a separate index with independent access control, or excluded from full-text search entirely and only served via the secret-level module.

## MVP Definition

Based on the Phase 1 / Phase 2 split defined in PROJECT.md and industry research.

### Launch With (Phase 1 -- 基础刚需版)

Core systems that solve the three primary pain points: Excel replacement, fee overdue prevention, and basic statistics.

- [x] 成果统一登记 (论文/专利/软著) — Core function. Solves "成果信息易遗漏" pain point.
- [x] 部门科研秘书初审流程 — Basic approval. Replaces paper signoff.
- [x] 专利年费台账 (含缴费计划) — Solves "缴费逾期风险高" pain point.
- [x] 四级预警 (30/15/7/逾期) + 二次催办 — Directly prevents fee lapse.
- [x] 基础统计看板 (年度趋势/类型分布/部门排行) — Solves "全院成果底数不清" pain point.
- [x] 基础检索 (标题/作者/关键词) — Users must find their own records.
- [x] RBAC + 部门数据隔离 — Foundation for all data security.
- [x] 审计日志 (操作留痕) — Compliance requirement.
- [x] 站内消息通知 — Required for alert delivery and approval workflow.
- [x] 人员/部门数据导入 (基础) — Manual import/CSV in Phase 1, LDAP sync deferred.
- [x] 附件上传与基础权限 — Users need to attach certificates.
- [x] 成果注销/作废 — End-of-life for IP records.
- [x] 邮件通知推送 — Outbound notification for alerts and approvals.
- [x] 统一登录 — Self-contained auth in Phase 1 with LDAP field reserved.
- [x] 数据每日自动备份 — Recovery compliance.

### Add After Validation (Phase 1.x -- Post-Launch Enhancements)

Features that users will request within 3 months of go-live based on industry patterns.

- [ ] DOI自动补全 (Crossref/Scopus/OpenAlex) — "Why do I have to type everything?" will be the #1 user complaint after launch.
- [ ] 批量缴费单生成 — Department secretaries managing >50 patents will request this immediately.
- [ ] 成果导出 (自定义Excel/PDF) — First annual report cycle will trigger this demand.
- [ ] 引文批量导入 (非API, 文件上传方式) — Users will ask "can I import my WoS export file" before API integration is ready.
- [ ] 提醒模板与批量创建 — Administrators managing recurring reminders will need this.
- [ ] 高级组合检索 (多字段联合查询) — Power users will outgrow basic search.

### Future Consideration (Phase 2+ -- 全流程深化版)

Features deferred to Phase 2 based on complexity, dependency on external systems, or unclear ROI.

- [ ] 成果转化全生命周期跟踪 (含收益分配) — Requires Phase 1 adoption and real conversion cases to exist first.
- [ ] 费用线上审批流程 (财务系统对接) — Requires financial system API integration and Phase 2 commitment.
- [ ] 专利法律状态同步 (CNIPA接口) — CNIPA API has rate limits; requires async queue infrastructure.
- [ ] 引文分析API集成 (Scopus/OpenAlex) — Monthly data sync pipeline; cost center for API subscriptions.
- [ ] 自定义报表 + 定时推送 — Requires understanding of real reporting needs after Phase 1 usage data.
- [ ] 移动端H5核心操作 — Phase 2 item in requirements. Desktop patterns must solidify first.
- [ ] 全文检索 (Elasticsearch + IK分词) — Phase 2 item in requirements; 10K+ records needed before search perf matters.
- [ ] 涉密成果独立权限体系 — Regulatory complexity; Phase 2 unless urgent requirement emerges.
- [ ] 短信通知接口 — Reserve slot only. Implement if fee overdue rate >5% after Phase 1.
- [ ] 大数据量性能优化 (分表/归档) — >100K records trigger. Phase 2 by design.
- [ ] 财务系统深度对接 — Phase 2. Manual voucher upload is sufficient for Phase 1.
- [ ] 成果与项目自动关联 — Requires project management system data; Phase 2 integration.

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| 成果统一登记 | HIGH | MEDIUM | **P1** |
| 部门科研秘书初审 | HIGH | MEDIUM | **P1** |
| 专利年费台账 | HIGH | MEDIUM | **P1** |
| 四级预警 + 二次催办 | HIGH | MEDIUM | **P1** |
| 基础统计看板 | HIGH | MEDIUM | **P1** |
| 基础检索 | MEDIUM | LOW | **P1** |
| RBAC + 部门数据隔离 | HIGH | MEDIUM | **P1** |
| 审计日志 | HIGH | LOW | **P1** |
| 站内消息通知 | HIGH | LOW | **P1** |
| 人员数据导入 | HIGH | MEDIUM | **P1** |
| 附件上传 | HIGH | LOW | **P1** |
| 成果注销/作废 | MEDIUM | LOW | **P1** |
| 邮件通知 | MEDIUM | LOW | **P1** |
| 数据备份 | HIGH | LOW | **P1** |
| DOI自动补全 | HIGH | MEDIUM | **P2** |
| 批量缴费单生成 | MEDIUM | LOW | **P2** |
| 高级组合检索 | MEDIUM | MEDIUM | **P2** |
| 自定义报表 | HIGH | MEDIUM | **P2** |
| 引文分析API集成 | MEDIUM | HIGH | **P2** |
| 专利法律状态同步 | HIGH | HIGH | **P3** |
| 转化全生命周期跟踪 | MEDIUM | HIGH | **P3** |
| 费用线上审批流程 | MEDIUM | HIGH | **P3** |
| 移动端H5核心操作 | MEDIUM | MEDIUM | **P3** |
| 涉密成果独立权限 | HIGH | HIGH | **P3** |
| 全文检索(ES) | MEDIUM | HIGH | **P3** |
| 财务系统对接 | MEDIUM | HIGH | **P3** |
| 成果与项目关联 | MEDIUM | MEDIUM | **P3** |

**Priority key:**
- P1: Must have for Phase 1 launch (solves core pain points, enables system adoption)
- P2: Should have, add in Phase 1.x post-launch (will be requested quickly, medium cost)
- P3: Future consideration, Phase 2+ (high complexity, dependency on external systems, or unclear near-term ROI)

## Competitor Feature Analysis

Compared against typical Chinese research institute systems (泛微OA-based, 长江数据, 浩翔知识产权, 高校自建系统).

| Feature | 泛微OA方案 | 长江数据知识产权系统 | 典型高校自建系统 | Our Approach |
|---------|-----------|---------------------|-----------------|--------------|
| 成果类型覆盖 | 主要专利, 论文较弱 | 专利为主, 论文有限 | 论文+专利+软著 | Paper + Patent + Software Copyright equal citizens |
| DOI自动补全 | 无 | 无 | 少数有 | Multi-source (Crossref/Scopus/OpenAlex) with priority config |
| 年费预警 | 基础邮件提醒 | 多级预警 | 基础 | 4-tier + escalation to department head + secondary reminder |
| 引文分析 | 无 | 无 | 基础 (手动录入) | Auto API sync + personal/department/institute 3-level |
| 审批流配置 | 固定3级(OA模式) | 固定流程 | 部分可配置 | Configurable by type/secret level with countersign support |
| 转化跟踪 | 无 | 基础 | 有限 | Full lifecycle with revenue split and benefit follow-up |
| 涉密数据隔离 | 等保基础 | 无 | 极少数有 | Independent permission system + encrypted storage + full audit |
| 移动端 | 泛微移动OA | 无 | 有限 | H5 responsive for Phase 2 (approve/check/alert) |
| 自定义报表 | 报表模块 | 固定报表 | 部分 | Custom dimension/metric builder + scheduled push |
| 接口开放性 | 封闭 (泛微生态) | 封闭 | 有限 | API-first design with config center, rate limiting, monitoring |

## Sources

- 需求规格说明书 (研究院科研成果管理系统说明) — Primary source for requirements
- Web Search: 中国科研院所专利年费管理工作流最佳实践 — Fee management workflows
- Web Search: 高校科研成果管理系统 SCI/EI/CSCD 引文分析集成方案 — Citation analysis integration patterns
- Web Search: 科研院所多级审批流程成果管理系统设计 — Multi-level approval patterns
- Web Search: 高校科研管理系统功能缺失与用户反馈 — Common feature gaps and user complaints
- Web Search: 科研管理系统绩效评价职称评审自动积分 — Performance evaluation feature landscape
- Web Search: 高校科研诚信与学术不端检测系统功能 — Academic integrity feature landscape
- Web Search: 科研成果管理批量导入与自动定级 — Import and auto-classification features
- Web Search: 科研成果管理与项目管理系统关联 — Project-achievement linkage patterns
- Web Search: 专利年费线上缴费审批流程分级审核 — Online fee approval workflows

---
*Feature research for: 科研成果与知识产权管理系统 (Scientific Research Achievement & IP Management System)*
*Researched: 2026-06-16*
