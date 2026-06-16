# Phase 2: Fee Management & Alerts - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-16
**Phase:** 2-fee-management-alerts
**Areas discussed:** 台账展示与交互, 缴费计划生成策略, 四级预警与催办流程, 费用统计与导出

---

## 台账展示与交互

| Option | Description | Selected |
|--------|-------------|----------|
| 标准表格视图 | el-table 与 Phase 01 成果列表一致 | ✓ |
| 卡片+表格混合 | 顶部统计卡片+下方表格 | |
| 你决定 | 自由描述 | |

**User's choice:** 标准表格视图

---

| Option | Description | Selected |
|--------|-------------|----------|
| 四维筛选+快捷标签 | 四个筛选器横向排列+快捷标签 | |
| 精简筛选+高级展开 | 默认2-3个筛选器，其余折叠+快捷标签 | ✓ |
| 仅基础筛选 | 不放快捷标签 | |

**User's choice:** 精简筛选+高级展开
**Notes:** 保留快捷筛选标签：即将逾期/已逾期/本月需缴费/全部

---

| Option | Description | Selected |
|--------|-------------|----------|
| 查看+编辑+标记 | 行内可标记已缴费 | |
| 仅查看与编辑 | 标记缴费走批量操作 | ✓ |
| 查看+状态流转 | 含催办/暂停按钮 | |

**User's choice:** 仅查看与编辑

---

| Option | Description | Selected |
|--------|-------------|----------|
| 简单流程 | 勾选→生成缴费单(自动编号)→标记完成 | ✓ |
| 带待缴费中间态 | 增加中间暂存状态 | |

**User's choice:** 简单流程
**Notes:** 编号格式 FEE-YYYYMMDD-XXX

---

| Option | Description | Selected |
|--------|-------------|----------|
| 标签页式 | 基本信息/缴费历史/附件/操作日志 | ✓ |
| 单页滚动式 | 所有信息在一页滚动展示 | |

**User's choice:** 标签页式（同成果详情）

---

| Option | Description | Selected |
|--------|-------------|----------|
| 独立一级菜单 | 顶部导航「费用管理」/fee | ✓ |
| 挂在成果管理下 | 作为子页面 | |

**User's choice:** 独立一级菜单

---

| Option | Description | Selected |
|--------|-------------|----------|
| 支持，独立附件区 | 复用AttachmentUploader | ✓ |
| 暂不支持 | 仅记录凭证号 | |

**User's choice:** 支持，独立附件区

---

| Option | Description | Selected |
|--------|-------------|----------|
| 下拉选择+字典配置 | 数据字典管理经费来源 | ✓ |
| 自由文本 | 用户手动填写 | |

**User's choice:** 下拉选择+字典配置

---

| Option | Description | Selected |
|--------|-------------|----------|
| 混合模式（推荐） | 自动生成首条+手动添加一次性费用 | ✓ |
| 全手动 | 所有费用手动新增 | |

**User's choice:** 混合模式（推荐）
**Notes:** 首条费用根据专利 nextFeeDate 自动生成

---

## 缴费计划生成策略

| Option | Description | Selected |
|--------|-------------|----------|
| 基于授权日（推荐） | 专利授权日为基准 | ✓ |
| 基于首次缴费日 | 首次缴费日为基准 | |
| 基于申请日 | 专利申请日为基准 | |

**User's choice:** 基于授权日（推荐）

---

| Option | Description | Selected |
|--------|-------------|----------|
| 定时任务每日扫描（推荐） | 凌晨自动扫描生成 | ✓ |
| 提交/归档时触发 | 一次性生成全生命周期计划 | |

**User's choice:** 定时任务每日扫描（推荐）

---

| Option | Description | Selected |
|--------|-------------|----------|
| 完整可编辑 | 金额+日期+暂停均可编辑 | |
| 仅金额和暂停 | 日期锁定，仅金额和暂停可编辑 | ✓ |

**User's choice:** 仅金额和暂停

---

| Option | Description | Selected |
|--------|-------------|----------|
| 手动添加，统一台账 | 一次性费用与年费并列展示 | ✓ |
| 独立管理页面 | 一次性费用独立管理 | |

**User's choice:** 手动添加，统一台账

---

| Option | Description | Selected |
|--------|-------------|----------|
| 统一模型，类型字段区分 | 年费/登记费/维护费/其他 | ✓ |
| 软著仅手动添加 | 软著费用独立处理 | |

**User's choice:** 统一模型，类型字段区分

---

| Option | Description | Selected |
|--------|-------------|----------|
| 缴费后自动续期 | 缴费后自动生成下年度 | |
| 缴费后不自动续期 | 由定时任务统一管理 | ✓ |

**User's choice:** 缴费后不自动续期

---

| Option | Description | Selected |
|--------|-------------|----------|
| 多态Arc模式（推荐） | owner_type+owner_id | ✓ |
| 独立外键 | patent_id+copyright_id | |

**User's choice:** 多态Arc模式（推荐）

---

| Option | Description | Selected |
|--------|-------------|----------|
| 自动暂停所有相关计划 | 专利作废后自动暂停 | ✓ |
| 手动处理 | 需用户手动暂停 | |

**User's choice:** 自动暂停所有相关计划

---

## 四级预警与催办流程

| Option | Description | Selected |
|--------|-------------|----------|
| 台账内标签+独立预警中心（推荐） | 双重展示 | ✓ |
| 仅台账内标签 | 仅颜色标签 | |
| 仅独立预警中心 | 仅独立页面 | |

**User's choice:** 台账内标签+独立预警中心（推荐）

---

| Option | Description | Selected |
|--------|-------------|----------|
| 蓝/黄/橙/红（推荐） | 30天/15天/7天/逾期 | ✓ |
| 绿/黄/橙/红 | 30天改为绿色 | |

**User's choice:** 蓝/黄/橙/红（推荐）

---

| Option | Description | Selected |
|--------|-------------|----------|
| 三级升级 | 首次→3天→部门负责人→5天→主管→逾期红色 | ✓ |
| 两级升级 | 首次→7天→部门负责人→逾期红色 | |

**User's choice:** 三级升级

---

| Option | Description | Selected |
|--------|-------------|----------|
| 可配置（推荐） | 通过管理页面配置 | |
| 硬编码 | 写死在代码中 | ✓ |

**User's choice:** 硬编码

---

| Option | Description | Selected |
|--------|-------------|----------|
| 站内消息（推荐） | 复用现有通知中心 | ✓ |
| 站内消息+邮件 | 需提前集成邮件 | |

**User's choice:** 站内消息（推荐）
**Notes:** 邮件推迟到 Phase 4

---

| Option | Description | Selected |
|--------|-------------|----------|
| 独立「费用预警」标签页（推荐） | 新增分类标签 | ✓ |
| 归入「系统通知」 | 不新增标签页 | |

**User's choice:** 独立「费用预警」标签页（推荐）

---

| Option | Description | Selected |
|--------|-------------|----------|
| 定时任务批量扫描（推荐） | 每日凌晨扫描 | ✓ |
| 实时计算 | 每次查看时实时计算 | |

**User's choice:** 定时任务批量扫描（推荐）

---

| Option | Description | Selected |
|--------|-------------|----------|
| 持久化（推荐） | 写入alert_record表 | ✓ |
| 不持久化 | 实时计算不存储 | |

**User's choice:** 持久化（推荐）

---

## 费用统计与导出

| Option | Description | Selected |
|--------|-------------|----------|
| 概览行+多维交叉表格（推荐） | 统计卡片+交叉筛选表格 | ✓ |
| 简单表格+柱状图 | 含eCharts图表 | |

**User's choice:** 概览行+多维交叉表格（推荐）
**Notes:** 图表能力留待Phase 3 Dashboard

---

| Option | Description | Selected |
|--------|-------------|----------|
| 费用管理子页面（推荐） | 费用台账/缴费计划/费用统计标签页 | ✓ |
| 独立页面 | 单独一级或挂Dashboard下 | |

**User's choice:** 费用管理子页面（推荐）

---

| Option | Description | Selected |
|--------|-------------|----------|
| 支持Excel导出（推荐） | EasyExcel导出当前筛选数据 | ✓ |
| 仅页面展示 | 不提供导出 | |

**User's choice:** 支持Excel导出（推荐）

---

| Option | Description | Selected |
|--------|-------------|----------|
| 整合在费用模块（推荐） | achievement-fee子模块中 | ✓ |
| 独立统计模块 | 抽取独立模块 | |

**User's choice:** 整合在费用模块（推荐）

---

## Claude's Discretion

无 — 所有灰色区域均经用户明确决策。

## Deferred Ideas

- 邮件预警通知 → Phase 4 邮件集成
- 费用线上审批流程 → v2 (FEE-v2-01)
- 图表可视化 → Phase 3 Dashboard
- 历史欠费追溯查询 → v2 (FEE-v2-02)
