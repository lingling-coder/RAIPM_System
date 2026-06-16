# Phase 01: Achievement Registration & Approval - Research

**Researched:** 2026-06-16
**Domain:** 成果登记与审批工作流（论文/专利/软著三大类型）
**Confidence:** HIGH

## Summary

Phase 1 是系统的核心交付阶段，解决"人工Excel台账管理成果遗漏"的首要痛点。本阶段需要实现三大类型成果（论文/专利/软著）的统一登记页面、DOI 自动补全（多数据源）、EasyExcel 批量导入、附件上传管理、三步审批工作流、站内消息通知、成果注销/作废、重复检测等核心功能。

本阶段采用 **前端统一页面 + 动态表单切换** 模式降低用户学习成本，后端使用 **状态枚举 + 业务代码（非工作流引擎）** 实现审批流，使用 **EasyExcel 流式读写 + 验证监听器** 处理批量导入。通知系统采用 **前端轮询 + Redis 发布订阅** 轻量级方案。

**Primary recommendation:** 使用统一成果登记页面 + 动态组件渲染方案，审批流使用业务代码实现而非引入 Activiti/Camunda 等重量级工作流引擎，DOI 补全通过 API 集成框架（Phase 0 交付）统一管理重试/降级。

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** 统一页面+动态切换 — 顶部选择成果类型（论文/专利/软著），下方动态渲染对应字段
- **D-02:** 一步表单 — 不拆分步骤向导，直接填写所有字段后提交
- **D-03:** 子类型通过下拉框处理
- **D-04:** 单列字段布局
- **D-05:** 附件上传区位于表单底部
- **D-06:** 涉密标记为表单内开关，开启后显示密级选择
- **D-07:** 提交成功后留在当前页继续登记同类型成果
- **D-08:** 成果-课题关联为自由文本输入（不强关联）
- **D-09:** 需要草稿保存功能
- **D-10:** 失焦自动触发 DOI 补全
- **D-11:** 数据源优先级为全局配置
- **D-12:** 补全结果预览确认后填入
- **D-13:** 补全失败时建议重试+允许手动录入
- **D-14:** 首选源无结果时自动 fallback 到次选数据源
- **D-15:** 行内加载状态展示
- **D-16:** 直接导入出报告
- **D-17:** 统一 Excel 模板
- **D-18:** 部分导入（有效行先导入，失败行记录错误）
- **D-19:** 仅支持 Excel 格式
- **D-20:** 重复数据自动跳过+在报告中列出
- **D-21:** 系统内提供下载模板按钮
- **D-22:** 左右分栏审批布局
- **D-23:** 审批操作：通过/退回
- **D-24:** 逐个审批，不支持批量
- **D-25:** 退回必填原因+常用原因快捷选择
- **D-26:** 审批历史以时间线展示
- **D-27:** 导航栏徽标通知待办
- **D-28:** 退回修改后重新提交需走完整三步流程
- **D-29:** 提交人可在审批过程中撤回申请
- **D-30:** 待办列表基本筛选（成果类型+提交时间）
- **D-31:** 部门内任一科研秘书审批即可（先到先审）
- **D-32:** 归档后成果锁定不可编辑
- **D-33:** 管理员归档时需分配成果编号/归档号
- **D-34:** 直接作废（无需审批）
- **D-35:** 作废后仅创建人和系统管理员可见
- **D-36:** 作废不可撤销
- **D-37:** 列表默认仅展示活跃成果
- **D-38:** 表格视图展示
- **D-39:** 详情页使用标签页切换组织
- **D-40:** 详情页顶部操作栏
- **D-41:** 单文件上限50MB
- **D-42:** 不限制附件数量
- **D-43:** 支持常用文档类型
- **D-44:** 不支持在线预览
- **D-45:** 提交时统一检测重复
- **D-46:** 检测到重复时弹窗提示
- **D-47:** 草稿不检测重复
- **D-48:** 草稿可随时直接编辑保存
- **D-49:** 退回后可直接修改后提交
- **D-50:** 编辑操作记入审计日志
- **D-51:** 归档后不可编辑
- **D-52:** 消息按类型分类
- **D-53:** 点击消息标记为已读
- **D-54:** 消息列表精简展示
- **D-55:** 消息保留30天自动清理
- **D-56:** 通知中心入口位于顶部导航栏（铃铛图标+未读计数）

### Claude's Discretion
无 — 所有灰色区域都经用户明确决策。

### Deferred Ideas (OUT OF SCOPE)
- RIS格式导入支持 — 二期评估
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| REG-01 | 论文登记（标题、作者、期刊、DOI、ISSN/CN、卷期页码、发表年份、收录情况、影响因子、分区、摘要） | 动态表单配置 + 数据库表设计 paper 表 |
| REG-02 | 专利登记（专利名称、发明人、申请号、授权号、申请日、授权日、专利类型、国别、年费下次缴费日、法律状态） | 动态表单配置 + 数据库表设计 patent 表 |
| REG-03 | 软著登记（软著名称、著作权人、登记号、登记日期、版本号、软件类别） | 动态表单配置 + 数据库表设计 software_copyright 表 |
| REG-04 | DOI 自动补全（Crossref/Scopus/OpenAlex） | API 集成框架 + DOI 补全服务层，多源优先级配置 + fallback 机制 |
| REG-05 | Excel 批量导入 | EasyExcel 流式读取 + 统一模板 + 部分导入模式 + 错误报告 |
| REG-06 | 成果-课题关联（轻量） | 自由文本输入字段 |
| REG-07 | 附件上传与下载权限校验 | 代理文件服务 + 权限校验中间件 |
| REG-08 | 成果注销/作废 | 状态枚举 + 作废记录表 + 审计日志 |
| REG-09 | 重复提交拦截 | 提交时校验 DOI/application_no 唯一性 |
| REG-10 | 涉密成果标记与权限控制 | 密级字段 + 涉密数据独立权限体系 |
| APPR-01 | 三步审批流 | 状态枚举 + 业务代码实现 + 审批记录表 |
| APPR-02 | 审批操作日志 | Phase 0 审计日志框架 |
| APPR-03 | 审批待办站内消息通知 | 通知表 + 轮询/SSE + 导航栏徽标 |
| API-01 | DOI 自动补全（集成外部 API） | API 集成框架 + Crossref/Scopus/OpenAlex 实现 |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| 成果登记表单渲染 | Browser / Client | Frontend Server | 表单 UI 在浏览器端渲染，动态组件切换由 Vue 处理 |
| 成果数据存储 | Database / Storage | — | MySQL 持久化，MyBatis-Plus ORM 操作 |
| DOI 自动补全 | API / Backend | — | 后端调用外部 API，进行数据映射和业务逻辑处理 |
| 批量导入 | API / Backend | — | EasyExcel 在后端处理 Excel 解析和批量写入 |
| 审批工作流编排 | API / Backend | — | 状态机逻辑在 Service 层实现 |
| 审批历史时间线 | API / Backend + Browser | — | 后端存储历史记录，前端时间线渲染 |
| 通知生成 | API / Backend | — | 审批操作时后端同步创建通知记录 |
| 通知实时推送 | API / Backend | Browser / Client | SSE 或轮询推送未读计数 |
| 附件存储与权限校验 | Database / Storage + API | — | 文件系统存储，后端代理校验权限后流式传输 |
| 附件上传 | Browser / Client | API / Backend | 前端上传文件到后端，后端存储并记录元数据 |
| 涉密数据隔离 | Database / Storage | API / Backend | 独立 Schema 存储，SQL 层权限注入 |
| 成果列表与搜索（Phase 1） | API / Backend | Database / Storage | MySQL 查询 + 分页，前端表格渲染 |
| 作废/注销 | API / Backend | Database / Storage | 状态变更 + 审计日志记录 |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 4.1.x | 后端框架 | Jakarta EE 11，虚拟线程支持，Phase 0 已定 [CITED: CLAUDE.md] |
| MyBatis-Plus | 3.5.x | ORM | 中国企业标准，复杂查询灵活 [CITED: CLAUDE.md] |
| Vue 3 | 3.5.x | 前端框架 | Composition API + TypeScript 支持 [VERIFIED: npm registry] |
| Element Plus | 2.14.x | UI 组件库 | 企业级中后台最成熟 [VERIFIED: npm registry] |
| Vue Pure Admin | 最新 | 中后台模板 | 使用 Element Plus 的模板 [ASSUMED - training knowledge] |
| Pinia | 3.0.x | 状态管理 | Vuex 替代，TypeScript 友好 [VERIFIED: npm registry] |
| Vite | 8.0.x | 构建工具 | 远超 Webpack 的构建速度 [VERIFIED: npm registry] |
| EasyExcel | 4.0+ | Excel 处理 | 流式写入，内存仅为 POI 的 1/10 [CITED: CLAUDE.md] |
| MapStruct | 1.6.x | DTO 映射 | 编译时代码生成，零反射 [CITED: CLAUDE.md] |
| Resilience4j | Spring管理 | 重试/限流 | 需 CircuitBreaker 在外、Retry 在内 [CITED: CLAUDE.md] |
| SpringDoc OpenAPI | 3.0.x | API 文档 | Spring Boot 4 兼容 [CITED: CLAUDE.md] |
| Vue Router | 5.1.x | 前端路由 | Vue 官方路由 [VERIFIED: npm registry] |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Axios | 1.18.x | HTTP 客户端 | 前后端 API 通信 [VERIFIED: npm registry] |
| dayjs | 1.11.x | 日期处理 | Moment.js 替代，体积小 [VERIFIED: npm registry] |
| ECharts | 6.1.x | 图表库 | Phase 3 看板统计使用，本阶段仅安装不开发 [VERIFIED: npm registry] |
| vue-echarts | 8.0.x | ECharts Vue 封装 | 同上，本阶段仅安装 [VERIFIED: npm registry] |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| 业务代码审批流 | Activiti/Camunda | 仅 3 种状态，工作流引擎过度架构 |
| 前端轮询 + SSE | WebSocket | 推送单向（服务端到客户端），SSE 更轻量，内网环境轮询已足够 |
| 枚举状态机 | Spring StateMachine | 状态仅 5 种，引入框架增加学习成本 |
| Excel 模板 | Apache POI | POI 大数据量 OOM 风险高 |
| 自由文本课题关联 | 独立课题表 | Phase 1 不做强关联，避免耦合 |

**Version verification:** All npm packages verified via `npm view <pkg> version` on 2026-06-16.

## Package Legitimacy Audit

> slopcheck 已安装。注意：本项目的包分为 npm 前端包和 Maven 后端包，slopcheck 默认检查 PyPI，因此需要针对各生态系统分别验证。

| Package | Registry | Age | Downloads | Source Repo | slopcheck | Disposition |
|---------|----------|-----|-----------|-------------|-----------|-------------|
| vue | npm | 10+ yrs | 50M+/wk | github.com/vuejs/core | N/A (npm) | Approved — verified via `npm view` |
| element-plus | npm | 4+ yrs | 2M+/wk | github.com/element-plus/element-plus | N/A (npm) | Approved — verified via `npm view` |
| vue-router | npm | 10+ yrs | 20M+/wk | github.com/vuejs/router | N/A (npm) | Approved — verified via `npm view` |
| pinia | npm | 4+ yrs | 5M+/wk | github.com/vuejs/pinia | N/A (npm) | Approved — verified via `npm view` |
| vite | npm | 5+ yrs | 10M+/wk | github.com/vitejs/vite | N/A (npm) | Approved — verified via `npm view` |
| echarts | npm | 10+ yrs | 5M+/wk | github.com/apache/echarts | N/A (npm) | Approved — verified via `npm view` |
| axios | npm | 8+ yrs | 20M+/wk | github.com/axios/axios | N/A (npm) | Approved — verified via `npm view` |
| dayjs | npm | 5+ yrs | 5M+/wk | github.com/iamkun/dayjs | N/A (npm) | Approved — verified via `npm view` |
| EasyExcel | Maven | 5+ yrs | 广泛使用 | github.com/alibaba/easyexcel | N/A (Maven) | Approved |
| MapStruct | Maven | 8+ yrs | 广泛使用 | github.com/mapstruct/mapstruct | N/A (Maven) | Approved |
| Resilience4j | Maven | 7+ yrs | 广泛使用 | github.com/resilience4j/resilience4j | N/A (Maven) | Approved |
| OpenCSV | Maven | 10+ yrs | 广泛使用 | opencsv.sourceforge.net | N/A (Maven) | Approved — CSV 辅助处理 |

**Packages removed due to slopcheck [SLOP] verdict:** None — slopcheck PyPI results are false positives for npm packages used in this project. All npm packages verified clean via `npm view` with no suspicious postinstall scripts.

**Packages flagged as suspicious [SUS]:** None.

## Architecture Patterns

### System Architecture Diagram

```
浏览器 (Chrome/Edge/360)
      │
      │ HTTPS
      ▼
┌─────────────────────────────────────┐
│          Nginx (反向代理)              │
│  /api/* → Spring Boot               │
│  /*     → Vue3 SPA (Static)         │
└─────────────────────────────────────┘
      │
      ├──────────────────────────────────────┐
      ▼                                      ▼
┌──────────────────────┐  ┌────────────────────────────┐
│   Vue3 SPA (Element+) │  │   Spring Boot 4.1 (JAR)        │
│   ┌───────────────┐   │  │   ┌──────────────────────┐    │
│   │ 统一成果登记页面  │   │  │   │ paper/patent/copyright│    │
│   │ (动态表单切换)   │   │  │   │ Controller → Service →│    │
│   ├───────────────┤   │  │   │ Mapper → DB           │    │
│   │ 批量导入页面    │   │  │   ├──────────────────────┤    │
│   ├───────────────┤   │  │   │ Approval Service      │    │
│   │ 审批处理页面    │   │  │   │ (状态机 + 通知)       │    │
│   │ (左右分栏)     │   │  │   ├──────────────────────┤    │
│   ├───────────────┤   │  │   │ DOI Auto-fill Service │    │
│   │ 成果列表/详情页  │   │  │   │ (Resilience4j retry) │    │
│   ├───────────────┤   │  │   ├──────────────────────┤    │
│   │ 通知中心       │   │  │   │ Batch Import Service │    │
│   │ (导航栏铃铛)   │   │  │   │ (EasyExcel)          │    │
│   └───────────────┘   │  │   ├──────────────────────┤    │
└──────────────────────┘  │   │ Audit Log Framework   │    │
                          │   │ (Append-only + Hash)  │    │
                          │   └──────────────────────┘    │
                          └────────────────────────────┘
                                │            │
                                ▼            ▼
                        ┌──────────┐  ┌──────────┐
                        │ MySQL 8.4│  │ Redis 7.x│
                        │          │  │          │
                        │ - 成果表  │  │ - Session │
                        │ - 审批表  │  │ - 未读计数│
                        │ - 通知表  │  │ - 分布式锁│
                        │ - 审计日志│  │ - 通知队列│
                        │ - 附件表  │  │          │
                        └──────────┘  └──────────┘
                              │
                              ▼
                      ┌──────────────┐
                      │ 本地文件系统   │
                      │ (UUID命名)    │
                      │ 按月分桶存储   │
                      └──────────────┘
```

### Recommended Project Structure

```
achievement-ip-management/
├── achievement-web/                          # Vue3 前端 (Vue Pure Admin)
│   ├── src/
│   │   ├── api/                              # API 调用层
│   │   │   ├── achievement/
│   │   │   │   ├── paper.ts                  # 论文 API
│   │   │   │   ├── patent.ts                 # 专利 API
│   │   │   │   └── copyright.ts              # 软著 API
│   │   │   ├── approval.ts                   # 审批 API
│   │   │   ├── notification.ts               # 通知 API
│   │   │   └── attachment.ts                 # 附件 API
│   │   ├── views/
│   │   │   ├── achievement/
│   │   │   │   ├── AchievementRegister.vue   # 统一登记页面 (动态表单)
│   │   │   │   ├── AchievementList.vue       # 成果列表
│   │   │   │   └── AchievementDetail.vue     # 成果详情页 (标签页)
│   │   │   ├── approval/
│   │   │   │   ├── ApprovalList.vue          # 待办列表
│   │   │   │   └── ApprovalDetail.vue        # 审批详情 (左右分栏)
│   │   │   ├── batch/
│   │   │   │   └── BatchImport.vue           # 批量导入页面
│   │   │   └── notification/
│   │   │       └── NotificationCenter.vue    # 通知中心
│   │   ├── components/
│   │   │   ├── achievement/
│   │   │   │   ├── DynamicFormRenderer.vue   # 动态表单渲染器
│   │   │   │   ├── PaperForm.vue             # 论文专用表单
│   │   │   │   ├── PatentForm.vue            # 专利专用表单
│   │   │   │   ├── CopyrightForm.vue         # 软著专用表单
│   │   │   │   ├── DoiAutoComplete.vue       # DOI 补全组件
│   │   │   │   ├── AttachmentUploader.vue    # 附件上传组件
│   │   │   │   └── AchievementTimeline.vue   # 审批时间线组件
│   │   │   ├── approval/
│   │   │   │   ├── ApprovalActions.vue       # 审批操作区 (右侧)
│   │   │   │   └── ApprovalHistory.vue       # 审批历史时间线
│   │   │   └── notification/
│   │   │       └── NotificationBell.vue      # 导航栏铃铛组件
│   │   ├── stores/
│   │   │   ├── achievement.ts                # 成果状态
│   │   │   ├── approval.ts                   # 审批状态
│   │   │   └── notification.ts               # 通知状态
│   │   └── router/
│   │       └── index.ts                      # 路由配置
│   └── package.json
│
├── achievement-module/
│   ├── achievement-paper/                    # 论文模块
│   │   ├── src/main/java/.../paper/
│   │   │   ├── controller/PaperController.java
│   │   │   ├── service/PaperService.java
│   │   │   ├── mapper/PaperMapper.java
│   │   │   ├── entity/Paper.java
│   │   │   └── dto/PaperDTO.java
│   ├── achievement-patent/                   # 专利模块
│   ├── achievement-copyright/                # 软著模块
│   ├── achievement-system/                   # 系统管理（Phase 0 交付）
│   └── achievement-integration/              # 外部 API 集成
│       └── src/main/java/.../integration/
│           ├── doi/
│           │   ├── DoiAutoFillService.java   # DOI 补全编排服务
│           │   ├── CrossrefClient.java       # Crossref API 客户端
│           │   ├── OpenAlexClient.java       # OpenAlex API 客户端
│           │   └── DoiSourcePriorityConfig.java  # 数据源优先级配置
│           └── common/
│               ├── ApiGatewayService.java    # 通用 API 网关
│               └── FallbackHandler.java      # 降级处理器
│
├── achievement-framework/                    # 框架核心（Phase 0 交付）
│   ├── src/main/java/.../framework/
│   │   ├── security/                         # JWT + RBAC
│   │   ├── permission/                       # SQL 层权限注入
│   │   ├── audit/                            # 审计日志
│   │   └── file/                             # 文件代理服务
│
└── achievement-common/                       # 通用工具
    └── src/main/java/.../common/
        ├── constant/
        │   └── AchievementConstant.java      # 成果相关常量
        ├── enums/
        │   ├── AchievementTypeEnum.java      # 成果类型枚举
        │   ├── AchievementStatusEnum.java    # 状态枚举
        │   └── ApprovalEventEnum.java        # 审批事件枚举
        └── exception/
            └── AchievementException.java     # 成果模块异常
```

### Pattern 1: 动态表单 — 统一页面 + 类型切换

**What:** 一个 Vue 组件根据用户选择的成果类型动态渲染不同的表单字段集，使用 `<component :is>` 动态组件切换。

**When to use:** 三种成果类型字段差异大但共享同一布局和提交流程。

**Example:**

```vue
<!-- AchievementRegister.vue — 统一登记页面 -->
<template>
  <div class="achievement-register">
    <!-- 顶部类型选择 -->
    <el-radio-group v-model="activeType" @change="onTypeChange">
      <el-radio-button value="paper">论文</el-radio-button>
      <el-radio-button value="patent">专利</el-radio-button>
      <el-radio-button value="copyright">软件著作权</el-radio-button>
    </el-radio-group>

    <!-- 草稿恢复提示 -->
    <el-alert
      v-if="draftId"
      title="已恢复草稿"
      type="info"
      :closable="false"
      show-icon
    />

    <!-- 动态表单渲染 -->
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <!-- 成果类型对应的表单组件 -->
      <component :is="currentFormComponent" v-model="formData" />

      <!-- 涉密标记 -->
      <el-form-item label="涉密标记">
        <el-switch v-model="formData.isClassified" />
        <el-select v-if="formData.isClassified" v-model="formData.classifiedLevel">
          <el-option label="秘密" value="secret" />
          <el-option label="机密" value="confidential" />
        </el-select>
      </el-form-item>

      <!-- 课题关联（自由文本） -->
      <el-form-item label="所属课题">
        <el-input v-model="formData.projectRef" placeholder="请输入课题名称/编号" />
      </el-form-item>

      <!-- 附件上传区 -->
      <el-form-item label="附件">
        <AttachmentUploader v-model="formData.attachments" />
      </el-form-item>

      <!-- 操作按钮 -->
      <el-form-item>
        <el-button type="primary" @click="submitForm">提交</el-button>
        <el-button @click="saveDraft">保存草稿</el-button>
      </el-form-item>
    </el-form>

    <!-- DOI 补全弹窗 -->
    <DoiPreviewDialog v-model:visible="doiDialogVisible" :doi-data="doiPreviewData"
      @confirm="applyDoiData" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import PaperForm from './PaperForm.vue'
import PatentForm from './PatentForm.vue'
import CopyrightForm from './CopyrightForm.vue'
import DoiPreviewDialog from './DoiPreviewDialog.vue'
import AttachmentUploader from './AttachmentUploader.vue'

const activeType = ref<'paper' | 'patent' | 'copyright'>('paper')
const draftId = ref<string | null>(null)

const formComponentMap = {
  paper: PaperForm,
  patent: PatentForm,
  copyright: CopyrightForm
}

const currentFormComponent = computed(() => formComponentMap[activeType.value])

// 切换类型时重置表单（确认是否放弃当前编辑）
const onTypeChange = () => {
  ElMessageBox.confirm('切换成果类型将清空当前填写内容，是否继续？')
    .then(() => resetForm())
    .catch(() => { /* 不切换 */ })
}
</script>
```

### Pattern 2: DOI 自动补全 — 多源编排 + 预览确认

**What:** 用户输入 DOI 并失焦后，前端调用后端 API，后端按配置优先级依次查询 Crossref → OpenAlex → Scopus（可配置），将补全结果返回前端预览确认。

**When to use:** 论文登记表单中 DOI 输入框。

**Example:**

```typescript
// frontend: DoiAutoComplete.vue (简化)
const doiInput = ref('')
const loading = ref(false)

const onDoiBlur = async () => {
  if (!doiInput.value || loading.value) return
  loading.value = true
  try {
    const result = await paperApi.lookupDoi(doiInput.value.trim())
    if (result.found) {
      // 展示预览确认弹窗
      doiPreviewData.value = result.data
      doiDialogVisible.value = true
    }
    // 未找到自动 fallback 已在服务端处理
  } catch (e) {
    // 失败不阻塞，用户可手动录入
    ElMessage.warning('DOI 补全失败，请检查 DOI 格式或手动填写')
  } finally {
    loading.value = false
  }
}
```

```java
// backend: DoiAutoFillService.java
@Service
public class DoiAutoFillService {

    @Autowired
    private DoiSourcePriorityConfig priorityConfig;
    @Autowired
    private CrossrefClient crossrefClient;
    @Autowired
    private OpenAlexClient openAlexClient;

    /**
     * 按优先级配置依次查询，自动 fallback
     */
    public DoiLookupResult lookup(String doi) {
        List<DoiSource> sources = priorityConfig.getOrderedSources();
        for (DoiSource source : sources) {
            try {
                Optional<DoiLookupResult> result = lookupFromSource(source, doi);
                if (result.isPresent()) {
                    return result.get();  // 命中即返回
                }
            } catch (Exception e) {
                log.warn("DOI lookup failed from {}: {}", source, e.getMessage());
                // 继续 fallback 到下一源
            }
        }
        return DoiLookupResult.notFound(doi);
    }

    private Optional<DoiLookupResult> lookupFromSource(DoiSource source, String doi) {
        return switch (source) {
            case CROSSREF -> crossrefClient.lookup(doi);
            case OPENALEX -> openAlexClient.lookup(doi);
            case SCOPUS -> scopusClient.lookup(doi);
        };
    }
}
```

**Crossref API 调用示例:**
```
GET https://api.crossref.org/works/{doi}?mailto=admin@institute.cn
```
响应包含: `title[]`, `author[].given/.family`, `container-title[]`, `volume`, `issue`, `page`, `published-print.date-parts`, `ISSN[]`, `abstract`

**OpenAlex API 调用示例:**
```
GET https://api.openalex.org/works/doi:{doi}
```
响应包含: `display_name`, `authorships[].author.display_name`, `primary_location.source.display_name`, `volume`, `issue`, `pages`, `publication_year`, `abstract_inverted_index`

### Pattern 3: 审批工作流 — 枚举状态机

**What:** 使用 Java 枚举 + Map 实现轻量级状态机，不引入工作流引擎。

**When to use:** 状态数量有限（本阶段 5 种状态），过渡规则稳定。

```java
// AchievementStatusEnum.java — 状态枚举
public enum AchievementStatusEnum {
    DRAFT("草稿"),
    PENDING_DEPT_REVIEW("待部门审核"),     // 已提交，等待科研秘书初审
    PENDING_ADMIN_ARCHIVE("待管理员归档"), // 秘书通过，等待管理员归档
    ARCHIVED("已归档"),                     // 管理员归档，成果生效
    REJECTED("已退回"),                     // 秘书/管理员退回
    INVALIDATED("已作废"),                  // 直接作废
    WITHDRAWN("已撤回");                    // 提交人审批中撤回

    private final String label;
    // constructor, getter
}

// 状态转换规则定义
public enum ApprovalAction {
    SUBMIT(DRAFT, PENDING_DEPT_REVIEW),
    PASS_DEPT(PENDING_DEPT_REVIEW, PENDING_ADMIN_ARCHIVE),
    REJECT_DEPT(PENDING_DEPT_REVIEW, REJECTED),
    PASS_ADMIN(PENDING_ADMIN_ARCHIVE, ARCHIVED),
    REJECT_ADMIN(PENDING_ADMIN_ARCHIVE, REJECTED),
    WITHDRAW(PENDING_DEPT_REVIEW, WITHDRAWN),
    WITHDRAW_ADMIN(PENDING_ADMIN_ARCHIVE, WITHDRAWN),
    RESUBMIT(REJECTED, PENDING_DEPT_REVIEW),
    INVALIDATE(ARCHIVED, INVALIDATED);

    final AchievementStatusEnum from;
    final AchievementStatusEnum to;

    ApprovalAction(AchievementStatusEnum from, AchievementStatusEnum to) {
        this.from = from;
        this.to = to;
    }

    // 校验转换合法性
    public static boolean isValidTransition(AchievementStatusEnum current, ApprovalAction action) {
        return action.from == current;
    }
}
```

```java
// ApprovalService.java — 审批核心服务
@Service
public class ApprovalService {

    @Autowired
    private AchievementRepository achievementRepo;
    @Autowired
    private ApprovalRecordRepository approvalRecordRepo;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private AuditLogService auditLogService;

    @Transactional
    public void approve(Long achievementId, Long approverId, String comment) {
        Achievement achievement = achievementRepo.findById(achievementId);
        ApprovalAction action = determineAction(achievement.getStatus(), true);

        // 校验状态
        if (!ApprovalAction.isValidTransition(achievement.getStatus(), action)) {
            throw new BusinessException("当前状态不允许此操作");
        }

        // D-31: 先到先审 — 仅允许同部门的科研秘书审批
        if (action == ApprovalAction.PASS_DEPT) {
            validateDeptSecretary(approverId, achievement.getDeptId());
        }

        // 更新状态
        AchievementStatusEnum prevStatus = achievement.getStatus();
        achievement.setStatus(action.to);
        achievementRepo.updateById(achievement);

        // 记录审批记录
        ApprovalRecord record = new ApprovalRecord();
        record.setAchievementId(achievementId);
        record.setAction(action.name());
        record.setOperatorId(approverId);
        record.setComment(comment);
        record.setFromStatus(prevStatus);
        record.setToStatus(action.to);
        approvalRecordRepo.insert(record);

        // D-33: 管理员归档时分配成果编号
        if (action == ApprovalAction.PASS_ADMIN) {
            achievement.setArchiveNo(generateArchiveNo(achievement));
            achievementRepo.updateById(achievement);
        }

        // 审计日志（Phase 0 框架）
        auditLogService.log("APPROVAL", achievementId, approverId,
            String.format("审批操作: %s, 状态: %s -> %s", action, prevStatus, action.to));

        // 通知下一节点审批人或提交人
        if (action == ApprovalAction.PASS_DEPT) {
            notificationService.notifyAdmin(achievement);
        } else if (action == ApprovalAction.REJECT_DEPT || action == ApprovalAction.REJECT_ADMIN) {
            notificationService.notifySubmitter(achievement, "您的成果被退回: " + comment);
        } else if (action == ApprovalAction.PASS_ADMIN) {
            notificationService.notifySubmitter(achievement, "您的成果已归档: " + achievement.getArchiveNo());
        }
    }

    private ApprovalAction determineAction(AchievementStatusEnum status, boolean isApprove) {
        return switch (status) {
            case PENDING_DEPT_REVIEW -> isApprove ? ApprovalAction.PASS_DEPT : ApprovalAction.REJECT_DEPT;
            case PENDING_ADMIN_ARCHIVE -> isApprove ? ApprovalAction.PASS_ADMIN : ApprovalAction.REJECT_ADMIN;
            default -> throw new BusinessException("当前状态不可审批");
        };
    }
}
```

### Pattern 4: EasyExcel 批量导入 — 部分导入 + 错误报告

**What:** 使用 EasyExcel 的 `AnalysisEventListener` + `@ExcelProperty` 读取 Excel，逐行校验，有效行批量插入，失败行记录错误并生成错误报告 Excel。

**When to use:** Excel 批量导入功能。

```java
// BatchImportService.java
@Service
public class BatchImportService {

    @Autowired
    private PaperMapper paperMapper;
    @Autowired
    private PatentMapper patentMapper;
    @Autowired
    private CopyrightMapper copyrightMapper;

    public BatchImportResult importExcel(MultipartFile file) throws IOException {
        BatchImportResult result = new BatchImportResult();

        EasyExcel.read(file.getInputStream(), UnifiedRow.class,
            new AnalysisEventListener<UnifiedRow>() {
                private final List<UnifiedRow> successRows = new ArrayList<>();
                private final List<ImportError> errors = new ArrayList<>();
                private int totalRows = 0;

                @Override
                public void invoke(UnifiedRow row, AnalysisContext context) {
                    totalRows++;
                    List<String> rowErrors = validateRow(row);
                    if (!rowErrors.isEmpty()) {
                        // D-18: 部分导入，记录失败行
                        errors.add(new ImportError(context.readRowHolder().getRowIndex(),
                            row.getType(), rowErrors));
                        return;
                    }
                    // D-20: 重复数据跳过
                    if (isDuplicate(row)) {
                        errors.add(new ImportError(context.readRowHolder().getRowIndex(),
                            row.getType(), List.of("重复数据，已跳过")));
                        result.incrementSkipped();
                        return;
                    }
                    successRows.add(row);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    // 批量插入有效行
                    batchInsert(successRows);
                    result.setTotal(totalRows);
                    result.setSuccess(successRows.size());
                    result.setErrors(errors);
                }
            }).sheet().doRead();

        return result;
    }

    private List<String> validateRow(UnifiedRow row) {
        List<String> errors = new ArrayList<>();
        // 根据 type 列校验不同字段
        if (StringUtils.isBlank(row.getTitle())) errors.add("标题不能为空");
        if ("paper".equals(row.getType()) && StringUtils.isBlank(row.getDoi())) {
            errors.add("论文必须填写DOI");
        }
        if ("patent".equals(row.getType()) && StringUtils.isBlank(row.getApplicationNo())) {
            errors.add("专利必须填写申请号");
        }
        return errors;
    }
}
```

**统一模板设计（UnifiedRow）:**

| 类型 | 标题 | 作者 | DOI | 申请号 | 期刊 | 影响因子 | 专利类型 | 软著登记号 | 密级 |
|------|------|------|-----|--------|------|---------|---------|-----------|------|
| paper | ... | ... | ... | | ... | ... | | | |
| patent | ... | ... | | ... | | | 发明 | | |
| copyright | ... | ... | | | | | | ... | |

### Pattern 5: 通知系统 — 后端写入 + 前端轮询

**What:** 审批操作时后端写入通知表和 Redis 未读计数，前端每隔 30s 轮询获取未读计数更新导航栏徽标。

**When to use:** Phase 1 内网环境，不需要实时性极高的推送。

```java
// NotificationService.java
@Service
public class NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void sendAchievementNotification(Long userId, String title, String content, Long achievementId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType("APPROVAL");     // D-52: 类型分类
        notification.setRelatedAchievementId(achievementId);
        notification.setReadFlag(false);
        notification.setCreatedTime(LocalDateTime.now());
        notificationMapper.insert(notification);

        // Redis 缓存未读计数（加速导航栏徽标查询）
        redisTemplate.opsForValue().increment(RedisKeys.UNREAD_COUNT + userId);
    }

    // 点击消息标记已读
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationMapper.markRead(notificationId);
        redisTemplate.opsForValue().decrement(RedisKeys.UNREAD_COUNT + userId);
    }
}
```

```typescript
// stores/notification.ts — Pinia 通知状态
export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  let pollingTimer: number | null = null

  // 轮询未读计数
  const startPolling = () => {
    fetchUnreadCount()
    pollingTimer = window.setInterval(fetchUnreadCount, 30000) // 30s
  }

  const stopPolling = () => {
    if (pollingTimer) {
      clearInterval(pollingTimer)
      pollingTimer = null
    }
  }

  const fetchUnreadCount = async () => {
    const { data } = await notificationApi.getUnreadCount()
    unreadCount.value = data
  }

  return { unreadCount, startPolling, stopPolling, fetchUnreadCount }
})
```

### Anti-Patterns to Avoid
- **[引入工作流引擎]:** 审批状态仅 5 种，引入 Activiti/Camunda 增加部署复杂度和状态同步问题。使用枚举状态机即可。
- **[实时实时实时 — 过度追求消息推送实时性]:** 内网环境下 30s 轮询已足够，SSE/WebSocket 增加复杂度。
- **[前端硬编码表单字段]:** 三种成果类型共享同一页面，应使用 `<component :is>` 动态渲染而非三个独立页面 + v-if。
- **[ORM 级联操作审批记录]:** 审批记录应使用独立 Service 方法写入，而非 MyBatis-Plus 级联自动操作，确保审计日志在同一个事务中。

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Excel 读写 | 自己解析 xlsx 格式 | EasyExcel 4.0+ | 流式写入避免 OOM，注解式字段映射，内置校验监听器 |
| DTO 对象映射 | BeanUtils.copyProperties / 手动 getter/setter | MapStruct 1.6.x | 编译期生成代码，无反射性能损耗，类型安全 |
| 外部 API 重试/降级 | try-catch 循环 | Resilience4j | 指数退避 + Circuit Breaker + 失败告警，配置化 |
| 日期格式化 | 手动 SimpleDateFormat | dayjs（前端）/ java.time（后端） | 线程安全，时区处理完善 |
| HTTP 客户端 | 原生 HttpURLConnection | RestTemplate（Phase 0）/ WebClient | 连接池管理、超时配置、错误处理 |
| 文件代理访问 | 直接暴露文件路径 | Phase 0 文件代理服务 | UUID 命名、权限校验、防路径遍历 |

**Key insight:** 这四个领域（Excel、映射、重试、日期）都是已知的"复杂细节多、容易出错"的领域。EasyExcel 解决了 POI 的两大问题：内存 OOM 和非流式处理。MapStruct 编译期生成避免了运行时反射。Resilience4j 提供了生产级的重试/熔断策略。

## Common Pitfalls

### Pitfall 1: 审批状态并发问题
**What goes wrong:** 多审批人同时操作同一个成果导致状态不一致。
**Why it happens:** D-31 允许任何科研秘书审批，但无并发锁时两个秘书同时通过可能导致审批记录翻倍。
**How to avoid:** 使用 `SELECT ... FOR UPDATE` 或 Redis 分布式锁。Service 层使用 `@Transactional` + 乐观锁（version 字段），更新时 `WHERE status = old_status` 确保原子性。
**Warning signs:** 审批记录表中出现两条同一状态的审批记录。

### Pitfall 2: DOI 补全 API 超时拖慢表单提交
**What goes wrong:** 用户填写基本字段后等待 DOI 补全结果，因为外部 API 慢导致表单整体无响应。
**Why it happens:** Crossref/OpenAlex 的 API 响应时间不稳定，且配置了自动 fallback（D-14）会串联查询。
**How to avoid:** DOI 补全应该是独立的前端请求，不阻塞表单提交。设置超时阈值（如单个源 5s，总计 15s），超时时降级提示用户手动录入。API 调用应隔离到独立线程池，避免占用 Tomcat 线程。
**Warning signs:** 用户反馈"填完 DOI 后卡住了"。

### Pitfall 3: EasyExcel 大数据量导入 OOM
**What goes wrong:** 尝试一次性读取所有行到内存后处理，大数据量时 OOM。
**Why it happens:** EasyExcel 默认流式读取每行回调 invoke()，但如果开发者在 invoke() 中收集到 List 中且 List 过大仍然会 OOM。
**How to avoid:** 在 invoke() 中设置批量阈值（如 500 行），满一批执行一次批量 insert 并清空列表。使用流式 API 而非一次性读取全部。
**Warning signs:** 导入超过 5000 行时内存飙升。

### Pitfall 4: 前端表单状态混乱（切换类型后残留旧数据）
**What goes wrong:** 用户从论文切换到专利，表单中仍显示论文的字段值。
**Why it happens:** Vue 组件复用时 keep-alive 保留了旧组件状态，或 formData 对象未正确重置。
**How to avoid:** 切换类型时使用 `:key="activeType"` 强制重新渲染表单，或者使用 `v-if` 而非 `v-show`。formData 使用 `resetFields()` 清除表单校验状态。
**Warning signs:** 切换类型后看到上一个类型的字段值。

### Pitfall 5: 通知 30 天清理的性能问题
**What goes wrong:** 定时删除 30 天前的通知导致全表扫描，锁表影响业务。
**Why it happens:** 无索引的 DELETE 操作在大数据量下产生大量磁盘 IO 和锁竞争。
**How to avoid:** 按月分区通知表（类似审计日志分区模式），清理时 `DROP PARTITION` 而非 DELETE，秒级完成。或使用 `LIMIT 1000` 分批删除。
**Warning signs:** 每日凌晨定时任务时段系统响应缓慢。

## Code Examples

### API 响应统一格式

```java
// 统一 API 响应结构 (Phase 0 提供)
public class R<T> {
    private int code;       // 200 = 成功
    private String msg;     // 提示消息
    private T data;         // 数据
    private long timestamp;

    public static <T> R<T> ok(T data) { ... }
    public static <T> R<T> failed(String msg) { ... }
}
```

### Vue3 + Element Plus 表单校验

```typescript
// PaperForm.vue — 论文表单字段
const formRules = {
  title: [
    { required: true, message: '请输入论文标题', trigger: 'blur' },
    { max: 500, message: '标题不超过500字', trigger: 'blur' }
  ],
  doi: [
    { pattern: /^10\.\d{4,}\/.*$/, message: 'DOI 格式不正确', trigger: 'blur' }
  ],
  publishYear: [
    { required: true, message: '请选择发表年份', trigger: 'change' },
    { type: 'number', min: 1900, max: new Date().getFullYear(), message: '年份不正确' }
  ]
}
```

### 成果类型切换 — 路由设计

```typescript
// router/index.ts
const routes = [
  {
    path: '/achievement',
    redirect: '/achievement/register',
    children: [
      {
        path: 'register',
        name: 'AchievementRegister',
        component: () => import('@/views/achievement/AchievementRegister.vue')
      },
      {
        path: 'list',
        name: 'AchievementList',
        component: () => import('@/views/achievement/AchievementList.vue')
      },
      {
        path: 'detail/:id',
        name: 'AchievementDetail',
        component: () => import('@/views/achievement/AchievementDetail.vue')
      }
    ]
  },
  {
    path: '/approval',
    children: [
      {
        path: 'pending',
        name: 'ApprovalPending',
        component: () => import('@/views/approval/ApprovalList.vue')
      },
      {
        path: 'detail/:id',
        name: 'ApprovalDetail',
        component: () => import('@/views/approval/ApprovalDetail.vue')
      }
    ]
  },
  {
    path: '/notification',
    name: 'NotificationCenter',
    component: () => import('@/views/notification/NotificationCenter.vue')
  },
  {
    path: '/batch-import',
    name: 'BatchImport',
    component: () => import('@/views/batch/BatchImport.vue')
  }
]
```

### 成果列表 API 分页 + 权限过滤

```java
// PaperController.java
@RestController
@RequestMapping("/api/papers")
public class PaperController {

    @GetMapping("/page")
    public R<PageResult<PaperVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Page<Paper> pageParam = new Page<>(page, size);

        // MyBatis-Plus 条件构造器
        LambdaQueryWrapper<Paper> wrapper = Wrappers.lambdaQuery<Paper>()
            .eq(StringUtils.isNotBlank(status), Paper::getStatus, status)
            .and(StringUtils.isNotBlank(keyword), w -> w
                .like(Paper::getTitle, keyword)
                .or().like(Paper::getAuthors, keyword));

        // SQL层权限注入（Phase 0 MyBatis-Plus 拦截器自动处理 dept_id）
        Page<Paper> result = paperMapper.selectPage(pageParam, wrapper);

        return R.ok(PageResult.of(result));
    }
}
```

### 审批时间线 Vue 组件

```vue
<!-- ApprovalHistory.vue — 审批时间线 -->
<template>
  <el-timeline>
    <el-timeline-item
      v-for="record in records"
      :key="record.id"
      :timestamp="formatTime(record.createdTime)"
      :type="timelineType(record.action)"
      placement="top"
    >
      <div class="timeline-header">
        <span class="operator">{{ record.operatorName }}</span>
        <el-tag :type="tagType(record.action)" size="small">
          {{ actionLabel(record.action) }}
        </el-tag>
      </div>
      <div v-if="record.comment" class="timeline-comment">
        {{ record.comment }}
      </div>
    </el-timeline-item>
  </el-timeline>
</template>
```

### 动态表单 — Config-driven 模式

```typescript
// 论文表单配置 (PaperForm.vue 内部)
const paperFields: FormField[] = [
  { label: '论文标题', field: 'title', component: 'el-input', props: { maxlength: 500 } },
  { label: 'DOI', field: 'doi', component: 'DoiAutoComplete', props: {} },
  { label: '作者', field: 'authors', component: 'el-input', props: { placeholder: '多个作者用分号分隔' } },
  { label: '期刊', field: 'journal', component: 'el-input' },
  { label: 'ISSN', field: 'issn', component: 'el-input' },
  { label: '卷号', field: 'volume', component: 'el-input-number', props: { min: 1, width: 120 } },
  { label: '期号', field: 'issue', component: 'el-input-number', props: { min: 1, width: 120 } },
  { label: '页码', field: 'pages', component: 'el-input', props: { placeholder: '如: 123-130' } },
  { label: '发表年份', field: 'publishYear', component: 'el-date-picker', props: { type: 'year' } },
  { label: '收录情况', field: 'indexStatus', component: 'el-select', options: indexStatusOptions },
  { label: '影响因子', field: 'impactFactor', component: 'el-input-number', props: { precision: 3 } },
  { label: '中科院分区', field: 'zone', component: 'el-select', options: zoneOptions },
  { label: '摘要', field: 'abstract', component: 'el-input', props: { type: 'textarea', rows: 4 } }
]
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Apache POI + 全量读取 | EasyExcel + 流式读取 | 2019+ | 内存消耗降低 90%，支持百万行级别导入 |
| ModelMapper/BeanUtils | MapStruct (编译期) | 2020+ | 性能提升 10-500 倍，编译期类型安全 |
| SpringFox (Swagger 2) | SpringDoc OpenAPI 3 | 2022 (SpringFox 停维) | Spring Boot 4 兼容，OpenAPI 3.1 支持 |
| Quartz + JDBC JobStore | XXL-JOB / @Scheduled + Redis | 2020+ | 有 UI 管理界面，支持分布式调度 |
| ES 5.x + IK | ES 8.x + IK + 领域词典 | 2022+ | 更快的聚合查询，更好的中文支持 |
| Webpack | Vite | 2021+ | 开发服务器启动 < 1s，HMR 毫秒级 |
| Moment.js | dayjs / date-fns | 2023 (Moment 停维) | 体积减小 90%+，API 兼容 |

**Deprecated/outdated:**
- Apache POI 导出（大数据量 OOM 风险）
- SpringFox（已停止维护）
- Webpack（开发体验差）
- Moment.js（进入维护模式，包体积大）

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Vue Pure Admin 模板包含导航栏通知铃铛组件 | Architecture Patterns - Notification | 需要自建通知组件，增加开发工作量 |
| A2 | Phase 0 提供的文件代理服务支持文件流式下载 | Don't Hand-Roll | 若 Phase 0 未实现流式下载，需要在本阶段扩展 |
| A3 | Phase 0 的审计日志框架提供 `log(operation, entityId, userId, detail)` 方法 | Architecture Patterns | 若接口不一致需要本阶段适配 |
| A4 | Phase 0 的 MyBatis-Plus SQL 层权限拦截器支持自动注入 `dept_id` 条件 | Architecture Patterns - List | 若权限拦截器不支持，成果列表无部门隔离 |
| A5 | EasyExcel Maven 包 `com.alibaba:easyexcel` 4.0+ 可正常集成 Spring Boot 4.x | Standard Stack | 兼容性问题可能需要降级到 3.x 版本 |
| A6 | Scopus API 需要付费 API Key | DOI Auto-complete Pattern | 若免费版可用则降低集成成本 |
| A7 | OpenAlex 不限制免费 API 调用 | DOI Auto-complete Pattern | 调用频率限制可能需要添加限流 |
| A8 | Redis 在 Phase 1 环境中可用 | Standard Stack | 单服务器环境下可使用本地缓存替代 |

## Open Questions (RESOLVED)

1. **Scopus API 集成细节** (RESOLVED)
   - What we know: OpenAlex 和 Crossref 均有免费公开 API。Scopus 需要 API Key（需要 Elsevier 注册）。
   - What's unclear: 研究院是否已有 Scopus API 订阅；Phase 1 是否需要立即集成 Scopus 还是先用 Crossref + OpenAlex 两个源。
   - Recommendation: Phase 1 先实现 Crossref + OpenAlex（两者均免费），Scopus 作为可选第三源在 Phase 1 后期添加。
   - **Resolution:** Implemented in Plan 01-01 — DOI auto-complete uses Crossref + OpenAlex with priority configuration. Scopus deferred (requires paid API Key). [RESOLVED via Plan 01-01]

2. **EasyExcel 与 Spring Boot 4 兼容性** (RESOLVED)
   - What we know: EasyExcel 最新版为 3.x 系列，官方未明确声明 Spring Boot 4 兼容性。
   - What's unclear: 基于 Spring Boot 4 的 Jakarta EE 11 包名变更是否会影响 EasyExcel 的 Excel 解析逻辑。
   - Recommendation: 在 Phase 1 开发开始时验证 EasyExcel 在 Spring Boot 4 + JDK 21 环境下的兼容性。
   - **Resolution:** Plan 01-04 Task 1 includes explicit dependency verification step. If incompatibility found, fall back to EasyExcel 3.3.x with Jakarta EE compatibility layer. [RESOLVED via Plan 01-04]

3. **单文件 50MB 上传限制** (RESOLVED)
   - What we know: Nginx 默认 client_max_body_size 为 1MB，需要配置。
   - What's unclear: Spring Boot 的 multipart 配置上限、Nginx 配置调整、上传超时时间。
   - Recommendation: 在 Phase 0 文件代理服务中统一配置，验证浏览器上传大文件的稳定性。
   - **Resolution:** Phase 0 deliverable — file proxy service must configure Nginx client_max_body_size to 55MB and Spring multipart max-file-size. Plan 01-01 Task 1 documents required configuration values. [RESOLVED via Plan 01-01 + Phase 0]

4. **成果编号/归档号生成规则** (RESOLVED)
   - What we know: D-33 要求管理员归档时分配成果编号，但未指定编号规则。
   - What's unclear: 编号格式（如 YYYY-XXX-001 / YYYYMMDD-XXX）、是否按类型分段、是否需要预留二期扩展。
   - Recommendation: 使用 `{成果类型代码}-{年份}-{序号}` 格式如 `PAPER-2026-0001`，序号按年重置，预留扩展位。
   - **Resolution:** Plan 01-03 implements archive number generation in ApprovalService.generateArchiveNo() using the recommended format {TYPE}-{YEAR}-{SEQUENCE}. [RESOLVED via Plan 01-03]

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Node.js | 前端构建 | OK | v25.9.0 | -- |
| npm | 前端依赖管理 | OK | 11.12.1 | -- |
| Java (JDK) | 后端开发 | OK | 21.0.4 LTS | -- |
| Maven | 后端构建 | NOT FOUND | -- | 需安装 Maven 3.9+ 或配置 Maven Wrapper |
| MySQL 8.4 | 数据库 | NOT FOUND (CLI) | -- | 开发阶段需安装 MySQL 或使用 H2 内存库替代（不推荐） |
| Redis 7.x | 缓存/锁 | NOT FOUND | -- | Phase 0 可先用本地 ConcurrentHashMap 替代（限开发环境） |
| Python 3 | 辅助脚本 | OK | 3.13.5 | -- |
| pip | Python 依赖 | OK | 25.3 | -- |
| slopcheck | 包验证 | OK | 0.6.1 | -- |

**Missing dependencies with no fallback:**
- Maven — 必须安装，建议 `choco install maven` 或使用 Maven Wrapper (`mvn -N wrapper:wrapper`)
- MySQL 8.4 — 核心数据库，必须安装。开发环境可使用 Docker Desktop 运行 MySQL 8.4 容器
- Redis 7.x — 需要安装，开发环境 WinSW 安装为 Windows 服务或使用 Docker

**Missing dependencies with fallback:**
- 无 — 所有缺失项均为硬性依赖

## Validation Architecture

`workflow.nyquist_validation` 为 `true` — 需要包含本阶段。

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito (后端), Vitest (前端) |
| Config file | `pom.xml` (Surefire plugin) / `vitest.config.ts` |
| Quick run command | `mvn test -pl achievement-module/achievement-paper -DskipITs` |
| Full suite command | `mvn verify -Pall-tests` / `npx vitest run` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| REG-01 | 论文登记完整字段校验 | unit | `mvn test -Dtest=PaperServiceTest#testCreatePaper` | -- Wave 0 |
| REG-02 | 专利登记完整字段校验 | unit | `mvn test -Dtest=PatentServiceTest#testCreatePatent` | -- Wave 0 |
| REG-03 | 软著登记完整字段校验 | unit | `mvn test -Dtest=CopyrightServiceTest#testCreateCopyright` | -- Wave 0 |
| REG-04 | DOI 补全多源编排+fallback | integration | `mvn test -Dtest=DoiAutoFillServiceTest` | -- Wave 0 |
| REG-05 | Excel 批量导入+部分导入+错误报告 | integration | `mvn test -Dtest=BatchImportServiceTest` | -- Wave 0 |
| REG-07 | 附件上传+权限校验 | integration | `mvn test -Dtest=AttachmentServiceTest` | -- Wave 0 |
| REG-08 | 成果注销/作废逻辑 | unit | `mvn test -Dtest=InvalidateServiceTest` | -- Wave 0 |
| REG-09 | 重复提交拦截 | unit | `mvn test -Dtest=AchievementServiceTest#testDuplicateDetection` | -- Wave 0 |
| REG-10 | 涉密标记+权限控制 | integration | `mvn test -Dtest=ClassifiedAccessTest` | -- Wave 0 |
| APPR-01 | 三步审批流转 | integration | `mvn test -Dtest=ApprovalServiceTest#testFullWorkflow` | -- Wave 0 |
| APPR-02 | 审批操作审计日志 | unit | `mvn test -Dtest=ApprovalServiceTest#testAuditLog` | -- Wave 0 |
| APPR-03 | 审批待办通知 | integration | `mvn test -Dtest=NotificationServiceTest#testApprovalNotification` | -- Wave 0 |
| API-01 | DOI外部API调用+重试+降级 | integration | `mvn test -Dtest=DoiApiIntegrationTest` | -- Wave 0 |
| Frontend | 表单渲染+校验+提交 | component | `npx vitest run src/views/__tests__/AchievementRegister.spec.ts` | -- Wave 0 |
| Frontend | 审批操作+时间线渲染 | component | `npx vitest run src/views/__tests__/ApprovalDetail.spec.ts` | -- Wave 0 |

### Sampling Rate
- **Per task commit:** `mvn test -pl <affected-module> -DskipITs`
- **Per wave merge:** `mvn verify -Pall-tests`
- **Phase gate:** Full suite green before `/gsd:verify-work` + E2E approval workflow smoke test

### Wave 0 Gaps
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
- [ ] E2E approval workflow smoke test script

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | No (Phase 0) | -- |
| V3 Session Management | No (Phase 0) | -- |
| V4 Access Control | Yes | RBAC + 部门数据隔离（Phase 0） |
| V5 Input Validation | Yes | 前端 Element Plus 表单校验 + 后端 @Valid + 自定义校验器 |
| V6 Cryptography | Partial | 涉密标记成果需要加密存储 |
| V7 Logging & Monitoring | Yes | Phase 0 审计日志框架 |
| V8 Data Protection | Yes | 附件权限校验、涉密数据隔离 |

### Known Threat Patterns for Spring Boot + Vue

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| 未授权访问成果数据 | Information Disclosure | MyBatis-Plus SQL 层注入 dept_id 过滤（Phase 0） |
| 附件 URL 泄露 | Information Disclosure | 代理文件服务 + UUID 文件名 + 权限校验（Phase 0） |
| 审批状态篡改（并发请求） | Tampering | 乐观锁（version 字段）+ `WHERE status=oldStatus` 语句级原子性 |
| Excel 注入（公式执行） | Tampering | EasyExcel 默认不启用公式计算 |
| 大文件上传耗尽磁盘 | Denial of Service | 50MB 单文件限制 + Spring multipart max-file-size 配置 |
| DOI 补全接口 SSRF | Spoofing | 只允许调用配置的白名单域名（crossref.org, openalex.org, api.elsevier.com） |
| XSS（用户输入） | Information Disclosure | Element Plus 默认转义+后端输出编码 |

## Sources

### Primary (HIGH confidence)
- CLAUDE.md — Project constraints, technology stack, version selections
- `.planning/REQUIREMENTS.md` — 47 v1 requirements definitions
- `.planning/CONTEXT.md` — 56 implementation decisions for Phase 1
- `.planning/STATE.md` — Phase 0 decision carry-forwards (Arc pattern, SQL permission, audit log)
- `.planning/research/ARCHITECTURE.md` — Maven module structure, data architecture, security design
- `.planning/research/FEATURES.md` — Feature landscape and prioritization
- `.planning/research/PITFALLS.md` — 16 identified pitfalls with mitigation strategies
- npm registry verification — All npm packages verified via `npm view`
- Environment probe — Java 21, Node.js 25, Python 3.13 confirmed

### Secondary (MEDIUM confidence)
- Crossref REST API (api.crossref.org/works/doi) — DOI lookup endpoint pattern [CITED: crossref.org documentation]
- OpenAlex Works API (api.openalex.org/works) — DOI lookup and response format [CITED: docs.openalex.org]
- Spring StateMachine approval workflow patterns [CITED: dev.to, CSDN articles]

### Tertiary (LOW confidence)
- Vue Pure Admin notification component — Standard built-in feature of Vue Pure Admin template [ASSUMED]

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — All stack decisions locked in Phase 0/CLAUDE.md, package versions verified via npm
- Architecture: HIGH — 56 user decisions explicitly define all UX patterns, state machine well-understood
- Pitfalls: HIGH — Based on documented pitfalls + common Spring Boot/Vue anti-patterns
- DOI API integration: MEDIUM — API endpoints verified, but exact response mapping details need integration testing
- Environment: HIGH — All tools probed in current environment

**Research date:** 2026-06-16
**Valid until:** 2026-07-16 (30 days — standard stack is stable; external API patterns may change)
