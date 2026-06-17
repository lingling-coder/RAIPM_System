# Phase 3: Dashboard & Search - Context

**Gathered:** 2026-06-17
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 3 delivers the visualization dashboard and full-text search capabilities. The existing `/dashboard` welcome page is transformed into a data-rich dashboard with 4 ECharts chart types (tab-switchable). A global search box is added to the top navigation bar for full-text achievement search with MySQL ngram, Chinese word segmentation, keyword highlighting, and permission-filtered results. Export supports Excel (chart detail data via EasyExcel) and PDF (IText server-side rendering).

**In scope (from ROADMAP):**
- 4 standard chart types: annual trend (line), type distribution (pie), department ranking (bar), patent valid/invalid ratio
- Charts displayed on the dashboard page (tab-switchable), replacing existing static welcome cards
- Excel export of chart detail data (EasyExcel pattern from Phase 2 FeeStats)
- PDF export via server-side IText rendering
- Full-text search via MySQL ngram parser on core fields (title, abstract, author/inventor, keywords)
- Keyword highlighting in search results
- Search result filtering by achievement type, department, year range, classification level
- Permission-filtered search results (DataPermissionInterceptor from Phase 0)
- 50-user concurrent performance target (OPS-04)

**Out of scope:**
- Elasticsearch upgrade (Phase 2/v2 SRCH-v2-02)
- Advanced combined search (Phase 2/v2 SRCH-v2-01)
- Citation analysis (Phase 2/v2 STAT-v2-01)
- Custom reports and scheduled push (Phase 2/v2 STAT-v2-02, STAT-v2-03)
- Chart drill-down to achievement list (deferred)
- Real-time search / typeahead suggestions
- Search query logging and hot search term analytics (Phase 2/v2 SRCH-v2-03)
</domain>

<decisions>
## Implementation Decisions

### 看板页面布局
- **D-01:** 看板合并到首页（`/dashboard` 路由下）。现有欢迎词和统计卡片保留，下方新增图表区域。管理员和普通用户看到范围不同的图表数据。
- **D-02:** 4种图表通过标签页切换。标签页为：年度趋势 / 类型分布 / 部门排行 / 专利状态。每个标签页下单个图表全区域展示，交互更精细。
- **D-03:** 图表为静态展示，无点击钻取交互。用户可在标签页间切换查看不同维度的数据，点击图表元素不做跳转或弹窗。

### 数据刷新策略
- **D-04:** 看板数据使用 Redis 5分钟缓存。后端统计聚合查询结果缓存5分钟，过期后重新查询。用户5分钟内多次切换标签页不触发重复查询。
- **D-05:** 页面不设手动刷新按钮。用户完全依赖缓存自动过期刷新机制，简化界面。

### 图表导出方案
- **D-06:** Excel 导出图表背后的明细数据（非图表图片）。按 Phase 2 FeeStats 的 EasyExcel 模式实现，每个图表导出对应的数据表格。
- **D-07:** PDF 使用后端 IText 渲染正式报表 PDF。后端聚合图表数据并渲染为排版精确的报表文件，包含图表数据和汇总信息。

### 搜索入口与精准度策略
- **D-08:** 搜索入口为顶部导航栏全局搜索框，始终可见。类似站内搜索体验，用户可在任何页面发起搜索。
- **D-09:** 用户点击搜索按钮或按回车后提交查询。非实时搜索——减少无效数据库查询，搜索引擎风格的一次性提交。
- **D-10:** 模糊搜索覆盖 4 个核心字段：标题（title）、摘要（abstract）、作者/发明人（authors/inventors）、关键词（keywords）。使用 MySQL ngram FULLTEXT 索引覆盖这些字段。
- **D-11:** 搜索结果使用精简列表风格：每行展示标题+类型标签+状态，点击跳转到成果详情页。信息密度高，适合快速扫描结果。

### MySQL ngram 搜索技术决策
- **D-12:** 使用 MySQL ngram 全文解析器实现中文分词。Phase 1 阶段数据量 <5万条，MySQL FULLTEXT + ngram 足够满足需求。Index 建立在 title、abstract、authors、keywords 的组合 FULLTEXT 索引上。
- **D-13:** 搜索结果关键词高亮使用前端 JavaScript 正则匹配实现（后端返回匹配位置信息）。
- **D-14:** 搜索结果自动按相关性排序（MySQL MATCH...AGAINST 返回的 relevance score）+ 权限过滤（DataPermissionInterceptor SQL 层注入部门过滤条件）。

### 权限与数据隔离
- **D-15:** 搜索结果自动应用 Phase 0 的 DataPermissionInterceptor 进行部门级过滤，用户只能看到本部门数据（领导角色根据权限配置可见更大范围）。
- **D-16:** 涉密成果在搜索结果中对无权限用户不显示（涉密数据独立权限体系过滤）。

### Claude's Discretion
图表精确样式（颜色方案、尺寸、ECharts 动画配置等）由 Claude 根据 Element Plus + ECharts 最佳实践自行决定。搜索结果列表的筛选区布局和交互细节由 Claude 自行决定。IText PDF 报表模板样式（表头、排版、字体）由 Claude 自行决定。导航栏全局搜索框的具体样式（宽度、展开动画、placeholder 等）由 Claude 参考 Vue Pure Admin 模板自行决定。
</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase Requirements
- `.planning/REQUIREMENTS.md` — STAT-01~02 (lines 38-40), SRCH-01~04 (lines 44-50), OPS-04 (line 64)
- `.planning/ROADMAP.md` — Phase 3 success criteria and plan structure (section "Phase 3: Dashboard & Search", lines 83-100)

### Project Context & Constraints
- `.planning/PROJECT.md` — Project overview, user roles (7 types), architecture constraints, key decisions
- `.planning/STATE.md` — Current project state, prior phase decisions summary

### Prior Phase Decisions
- `.planning/phases/00-foundation-infrastructure/00-CONTEXT.md` — Phase 0 decisions: RBAC, DataPermissionInterceptor, SQL-layer permission injection, JWT auth, file proxy service
- `.planning/phases/02-fee-management-alerts/02-CONTEXT.md` — Phase 2 FeeStats EasyExcel export pattern (D-29), FeeStats aggregation query pattern (D-27~D-28), Arc polymorphic pattern

### Technology Stack
- `CLAUDE.md` — Technology stack: ECharts 5.5 + vue-echarts 7, EasyExcel 4.0+, MySQL 8.4 ngram parser, IText (新增)
</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **FeeStatsService / FeeStatsController** — Phase 2 聚合查询模式（overview + grouped stats），复用为看板后端 API 的设计参考
- **FeeStatsExcelVO + EasyExcel export** — Phase 2 Excel 导出模式，复用为看板图表明细数据导出的实现参考
- **DataPermissionInterceptor** — Phase 0 SQL 层权限注入拦截器，搜索查询自动应用部门过滤
- **FeeStatsMapper.xml** — MyBatis `<choose>` 维度安全 GROUP BY 模式，避免 SQL 注入
- **NotificationCenter.vue** — 现有通知中心标签页切换模式，图表标签页切换可参考

### Established Patterns
- **Controller pattern:** `@Slf4j @RestController @RequestMapping("/api/...") @RequiredArgsConstructor` — consistent with all existing controllers
- **API response:** `Result<T>` wrapper — consistent project-wide
- **Vue Pure Admin layout:** `achievement-web/src/layout/index.vue` — 导航栏结构可扩展添加全局搜索框
- **Router pattern:** `createWebHashHistory` + lazy-loaded components in `src/router/index.ts`
- **Element Plus table + pagination:** 精简列表风格的搜索结果展示可复用现有组件模式

### Integration Points
- **Router:** 修改 `achievement-web/src/router/index.ts` 中的 `/dashboard` 路由组件（或保持现有路由，直接修改 dashboard/index.vue）
- **Layout nav:** 在 `achievement-web/src/layout/index.vue` 导航栏右侧添加全局搜索框
- **Dashboard page:** 改造 `achievement-web/src/views/dashboard/index.vue`，移除静态占位卡片，替换为 ECharts 图表区域
- **API layer:** 新增 `achievement-web/src/api/dashboard.ts`（看板 API）和 `achievement-web/src/api/search.ts`（搜索 API）
- **Backend:** 新增搜索服务（可能放在 framework 层或独立 search 模块），看板统计服务参考 FeeStatsService 模式

### Dependencies to Add
- **ECharts 5.5 + vue-echarts 7** — 前端图表库（package.json 未安装）
- **IText** — 后端 PDF 渲染库（pom.xml 未引入）
</code_context>

<specifics>
## Specific Ideas

看板遵循标准中国企业管理仪表盘模式：顶部统计概览卡片 + 标签页切换的图表区域。搜索体验类似站内搜索风格（顶部输入框 → 精简结果列表）。图表配色和样式由 Claude 根据 Element Plus 设计语言和 ECharts 最佳实践自行决定。

用户偏好简洁高效：不需要手动刷新按钮、不需要图表钻取交互、不需要实时搜索。界面设计以实用性和性能优先，避免过度交互。
</specifics>

<deferred>
## Deferred Ideas

- **图表点击钻取到成果列表** — 用户选择了静态展示，钻取交互作为未来增强
- **实时搜索（输入即搜）** — 当前数据量下无需，且 MySQL ngram 不适合高频查询
- **高级组合搜索** — Phase 2 / v2 功能（SRCH-v2-01）
- **Elasticsearch 升级** — Phase 2 / v2 功能（SRCH-v2-02），当数据量达 5 万条以上时迁移
- **自定义报表与定时推送** — Phase 2 / v2 功能（STAT-v2-02, STAT-v2-03）
</deferred>

---

*Phase: 3-Dashboard & Search*
*Context gathered: 2026-06-17*
