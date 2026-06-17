# Phase 3: Dashboard & Search - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-17
**Phase:** 3-dashboard-search
**Areas discussed:** 看板页面布局, 数据刷新策略, 图表导出方案, 搜索入口与精准度策略

---

## 看板页面布局

**Question 1: 看板页面布局方案怎么选？**

| Option | Description | Selected |
|--------|-------------|----------|
| 合并到首页 | 欢迎页下方直接展示4个图表卡片区域，保留欢迎词和快速操作入口 | ✓ |
| 独立看板页面 | 新增独立一级菜单「数据看板」，路由 /statistics，布局更充裕 | |

**Question 2: 4种图表如何组织？**

| Option | Description | Selected |
|--------|-------------|----------|
| 一页4图并行 | 4个卡片区域并行展示，一眼扫完所有图表 | |
| 标签页切换 | 顶部标签页切换，每次只看一个图表，区域更大 | ✓ |
| 缩略图+点击展开 | 概览缩略图展示，点击展开全屏 | |

**Question 3: 图表交互方式？**

| Option | Description | Selected |
|--------|-------------|----------|
| 静态展示 | 仅供查看，点击无交互 | ✓ |
| 点击钻取到列表 | 点击跳转带筛选的成果列表 | |
| Tooltip详情展示 | 弹窗展示具体数字，不跳转 | |

**User's choice:** 合并到首页，标签页切换，静态展示（无钻取）
**Notes:** 用户偏好简洁高效的看板体验，避免过度交互

---

## 数据刷新策略

**Question 1: 看板数据以什么频率刷新？**

| Option | Description | Selected |
|--------|-------------|----------|
| 每次页面加载实时查 | 永远最新但每次切换标签页都要等待 | |
| 5分钟Redis缓存 | 平衡新鲜度和性能 | ✓ |
| 30分钟缓存 | 性能最优但数据可能有滞后 | |
| 缓存+后台异步刷新 | stale-while-revalidate 模式 | |

**Question 2: 是否需要手动刷新按钮？**

| Option | Description | Selected |
|--------|-------------|----------|
| 不需要 | 完全依赖缓存自动过期 | ✓ |
| 需要手动刷新按钮 | 右上角放置刷新按钮 | |

**User's choice:** 5分钟Redis缓存，无手动刷新按钮

---

## 图表导出方案

**Question 1: Excel 导出内容？**

| Option | Description | Selected |
|--------|-------------|----------|
| 导出图表明细数据 | 图表背后的数据表格，EasyExcel实现 | ✓ |
| 导出图表图片+数据 | 图片插入Excel附带数据表 | |
| 不导出Excel | 仅图片 | |

**Question 2: PDF 导出方案？**

| Option | Description | Selected |
|--------|-------------|----------|
| 前端 ECharts 导出+拼图 | 纯前端，开发快但排版有限 | |
| 后端 IText 渲染PDF | 精确控制排版，适合正式报告 | ✓ |
| 浏览器打印 | 零开发成本 | |

**User's choice:** Excel导出图表明细数据（EasyExcel），PDF使用后端IText渲染

---

## 搜索入口与精准度策略

**Question 1: 搜索入口放在哪里？**

| Option | Description | Selected |
|--------|-------------|----------|
| 顶部导航栏全局搜索 | 始终可见，站内搜索风格 | ✓ |
| 独立搜索页面 | 新增一级菜单「成果检索」 | |
| 居中搜索首页风格 | 类似Google搜索页体验 | |

**Question 2: 搜索触发方式？**

| Option | Description | Selected |
|--------|-------------|----------|
| 点击/回车提交搜索 | 减少无效查询 | ✓ |
| 实时搜索（防抖） | 输入即搜但查询量大 | |

**Question 3: 搜索范围？**

| Option | Description | Selected |
|--------|-------------|----------|
| 核心字段 | 标题+摘要+作者+关键词，MySQL FULLTEXT | ✓ |
| 扩展字段 | 含期刊/专利号/登记号等 | |

**Question 4: 搜索结果风格？**

| Option | Description | Selected |
|--------|-------------|----------|
| 详细列表（含摘要高亮） | 知网风格，信息丰富 | |
| 精简列表 | 标题+类型标签+状态，信息密度高 | ✓ |

**User's choice:** 顶部导航栏全局搜索，点击/回车提交，核心字段，精简列表

---

## Claude's Discretion

以下领域由 Claude 自行判断：图表配色和ECharts样式细节、搜索结果筛选区布局、IText PDF模板样式、导航栏全局搜索框的交互细节。下游agent可以根据Element Plus + ECharts最佳实践自行决定。

## Deferred Ideas

- 图表点击钻取到成果列表（未来增强）
- 实时搜索/typeahead（当前数据量无需）
- 高级组合搜索（v2功能）
- Elasticsearch升级（v2功能，>=5万条数据时）
- 自定义报表与定时推送（v2功能）
