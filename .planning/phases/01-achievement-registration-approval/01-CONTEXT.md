# Phase 01: Achievement Registration & Approval - Context

**Gathered:** 2026-06-16
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 1 delivers the core achievement registration and approval workflow for three types of achievements (papers, patents, software copyrights), including DOI auto-complete, Excel batch import, attachment upload, draft saving, and a 3-step approval workflow with in-app notifications. This phase focuses on the achievement lifecycle from creation through archiving — covering the front-end form UX, approval interface, notification integration, and data validation.

**In scope (from ROADMAP):**
- Paper/patent/software copyright registration with full metadata fields
- DOI auto-complete (Crossref/Scopus/OpenAlex multi-source)
- Excel batch import (unified template)
- Attachment upload with download permission checks
- Achievement-project linkage (lightweight, free text)
- Draft save functionality
- 3-step approval workflow (submit → department review → admin archive)
- In-app notification for approval tasks
- Duplicate detection (DOI/application_no)
- Achievement invalidation with reason logging
- Classified marking with access control
</domain>

<decisions>
## Implementation Decisions

### 登记表单布局
- **D-01:** 统一页面+动态切换 — 顶部选择成果类型（论文/专利/软著），下方动态渲染对应字段
- **D-02:** 一步表单 — 不拆分步骤向导，直接填写所有字段后提交
- **D-03:** 子类型通过下拉框处理（如专利的发明/实用新型/外观设计）
- **D-04:** 单列字段布局
- **D-05:** 附件上传区位于表单底部
- **D-06:** 涉密标记为表单内开关，开启后显示密级选择
- **D-07:** 提交成功后留在当前页继续登记同类型成果
- **D-08:** 成果-课题关联为自由文本输入（不强关联）
- **D-09:** 需要草稿保存功能 — 未完成表单暂存至"草稿箱"

### DOI 补全交互
- **D-10:** 失焦自动触发 — 用户输入DOI后移出输入框自动发起补全
- **D-11:** 数据源优先级为全局配置 — 系统管理员在后台配置，用户无感知
- **D-12:** 补全结果预览确认后填入 — 弹窗/下拉展示结果，用户确认后再填充表单
- **D-13:** 补全失败时建议重试+允许手动录入，不阻塞登记
- **D-14:** 首选源无结果时自动 fallback 到次选数据源
- **D-15:** 行内加载状态展示 — DOI输入框旁转圈图标，字段显示"加载中"占位

### 批量导入流程
- **D-16:** 直接导入出报告 — 上传后立即导入，成功后展示结果报告
- **D-17:** 统一模板 — 一个Excel模板包含所有成果类型字段，通过"类型"列区分
- **D-18:** 部分导入 — 有效行先导入，失败行记录错误原因供下载修正
- **D-19:** 仅支持Excel格式（RIS格式二期考虑）
- **D-20:** 重复数据自动跳过+在报告中列出
- **D-21:** 系统内提供"下载模板"按钮下载空白模板

### 审批界面布局
- **D-22:** 左右分栏布局 — 左侧展示成果详情（可滚动），右侧固定审批操作区
- **D-23:** 审批操作选项：通过/退回（通过进入下一节点，退回至提交人修改）
- **D-24:** 逐个审批，不支持批量操作
- **D-25:** 退回必填原因，提供常用原因快捷选择
- **D-26:** 审批历史以时间线展示（各节点状态、时间、操作人）
- **D-27:** 通知中心+导航栏徽标通知待办（不直接跳转审批页）
- **D-28:** 退回修改后重新提交需走完整三步流程
- **D-29:** 提交人可在审批过程中撤回申请（计入审计日志）
- **D-30:** 待办列表基本筛选（成果类型+提交时间）
- **D-31:** 多人审批策略：部门内任一科研秘书审批即可（先到先审）
- **D-32:** 归档后成果锁定不可编辑
- **D-33:** 管理员归档时需分配成果编号/归档号

### 成果注销/作废
- **D-34:** 直接作废（无需审批），创建人或科研秘书可作废
- **D-35:** 作废后仅创建人和系统管理员可见（其他用户隐藏）
- **D-36:** 作废不可撤销

### 成果列表与详情页
- **D-37:** 列表默认仅展示活跃成果（审批中+已归档），草稿和作废在独立标签页
- **D-38:** 表格视图展示（标题、类型、状态、提交时间等字段）
- **D-39:** 详情页使用标签页切换组织（基本信息/审批进度/附件/操作日志）
- **D-40:** 详情页顶部操作栏，根据状态显示可用操作

### 附件管理
- **D-41:** 单文件上限50MB
- **D-42:** 不限制单个成果的附件数量
- **D-43:** 支持常用文档类型（PDF、Word、Excel、图片、压缩包）
- **D-44:** 不实现在线预览，仅支持下载

### 重复检测
- **D-45:** 提交时统一检测（非实时检测）
- **D-46:** 检测到重复时弹窗提示，可查看已存在成果
- **D-47:** 草稿不检测重复

### 成果编辑与版本管理
- **D-48:** 草稿可随时直接编辑保存
- **D-49:** 退回后的成果可直接修改后提交（无需额外操作）
- **D-50:** 编辑操作记入审计日志（操作人、时间、IP），不在UI中展示字段级版本差异
- **D-51:** 归档后不可编辑

### 站内消息通知中心
- **D-52:** 消息按类型分类（标签页：审批待办、系统通知等）
- **D-53:** 点击消息即标记为已读，导航栏未读数同步更新
- **D-54:** 消息列表精简展示（标题+时间+已读状态+类型图标）
- **D-55:** 消息保留30天自动清理
- **D-56:** 通知中心入口位于顶部导航栏（铃铛图标+未读计数）

### Prior Phase 0 Decisions Carried Forward
- **P0-01:** Exclusive Arc 模式处理多态关联（费用/附件/转化 → 论文/专利/软著）
- **P0-02:** MyBatis-Plus SQL 层权限注入（非应用层过滤）
- **P0-03:** MySQL ngram 全文索引（Phase 1-3 搜索）
- **P0-04:** 涉密数据独立 Schema 隔离
- **P0-05:** 仅追加审计日志 + 哈希链 + 月度分区
- **P0-06:** 代理式文件服务（UUID 文件名，不暴露直接路径）

### Claude's Discretion
无 — 所有灰色区域都经用户明确决策。
</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase Requirements
- `.planning/REQUIREMENTS.md` — Full requirement definitions: REG-01~10, APPR-01~03, API-01 for Phase 1 (lines 10-26, 22-27)
- `.planning/ROADMAP.md` — Phase 1 success criteria and plan structure (section "Phase 1: Achievement Registration & Approval")

### Project Context & Constraints
- `.planning/PROJECT.md` — Project overview, user roles (7 types), architecture constraints, key decisions
- `.planning/STATE.md` — Current project state and prior Phase 0 decisions (Arc pattern, permission injection, audit log, etc.)

### Phase 0 Foundation (Dependency)
- Phase 0 must complete before Phase 1 implementation begins — provides user/auth/RBAC, file proxy service, audit log framework, API integration framework
</canonical_refs>

<code_context>
## Existing Code Insights

### Project Status
Project is greenfield — no code exists yet. Phase 0 (Foundation & Infrastructure) is the current phase, planned to deliver the system management foundation that Phase 1 will build upon.

### Key Phase 0 Deliverables Phase 1 Depends On
- User/role/department management and RBAC permission model
- JWT authentication
- SQL-layer data permission injection (MyBatis-Plus interceptor)
- Append-only audit log with hash chain
- File proxy service with UUID naming
- API integration framework (configuration center, Resilience4j retry)
- Browser compatibility baseline (Chrome, Edge, 360)
</code_context>

<specifics>
## Specific Ideas

No specific reference examples were discussed — the implementation follows standard Chinese enterprise management system patterns. Key UX principles: efficiency-focused (auto-complete, batch imports), safety-first (confirmation dialogs for destructive actions), and clear status visibility (timeline, tags, badges).
</specifics>

<deferred>
## Deferred Ideas

- **RIS格式导入支持** — 二期评估，本期仅实现Excel模板导入
</deferred>

---

*Phase: 01-Achievement Registration & Approval*
*Context gathered: 2026-06-16*
