---
title: 01-UI-SPEC — 成果登记与审批
phase: 01
name: Achievement Registration & Approval
status: draft
version: 1.0
created: 2026-06-16
design_system: Element Plus + Vue Pure Admin
---

# UI Design Contract — Phase 01: 成果登记与审批

## 1. Design System

### 1.1 Component Library

| Property | Value | Source |
|----------|-------|--------|
| Library | Element Plus 2.9+ | CLAUDE.md locked |
| Template | Vue Pure Admin (latest) | CLAUDE.md locked |
| Icon Library | Element Plus Icons (内置) | Default — Element Plus standard |
| Styling | Scoped `<style>` + CSS Variables | Vue 3 SFC convention |
| State Management | Pinia 3.x | CLAUDE.md locked |

### 1.2 Element Plus Theme Configuration

```typescript
// Element Plus CSS Variables override convention
// Use CSS variables for theme consistency across all components
// Primary brand color: #409EFF (Element Plus default blue)
// Success: #67C23A
// Warning: #E6A23C
// Danger: #F56C6C
// Info: #909399
```

### 1.3 Vue Pure Admin Integration

| Integration Point | Usage | Notes |
|-------------------|-------|-------|
| Layout Shell | Sidebar + Header + Content | Vue Pure Admin default layout |
| Sidebar | Navigation menu via route meta | Role-dynamic menu rendering |
| Header | NotificationBell component | Custom extension to Vue Pure Admin header |
| Tabs | No tab-based navigation in layout | Not needed for Phase 1 |

## 2. Spacing Scale

### 2.1 Base Scale (Element Plus Defaults — 4px base)

| Token | Pixels | Usage |
|-------|--------|-------|
| `--el-spacing-1` | 4px | Inner padding for compact elements |
| `--el-spacing-2` | 8px | Gap between related elements |
| `--el-spacing-3` | 12px | Form item spacing |
| `--el-spacing-4` | 16px | Card padding, section spacing |
| `--el-spacing-5` | 20px | Form group spacing |
| `--el-spacing-6` | 24px | Page section vertical spacing |
| `--el-spacing-7` | 28px | Reserved |
| `--el-spacing-8` | 32px | Modal padding, large cards |
| `--el-spacing-9` | 36px | Reserved |
| `--el-spacing-10` | 40px | Page top/bottom padding |

### 2.2 Layout Gaps

| Context | Value | Rationale |
|---------|-------|-----------|
| Form item horizontal | 12px (gap between label and input) | Element Plus default |
| Form item vertical | 24px (bottom margin) | Override Element Plus default `--el-form-item-margin-bottom` |
| Table cell padding | 8px horizontal, 4px vertical | Element Plus default |
| Card inner padding | 20px | Standard content card |
| Page padding | 24px | Consistent page margins |
| Split panel gap | 16px | Left-right approval layout |
| Button group gap | 12px | Action button spacing |
| Tag group gap | 8px | Status tag spacing |

## 3. Typography

### 3.1 Font Family

```css
--el-font-family: "PingFang SC", "Microsoft YaHei", "Hiragino Sans GB",
  "Helvetica Neue", Helvetica, Arial, sans-serif;
```

### 3.2 Font Size Scale

| Token | Size | Context | Elements |
|-------|------|---------|----------|
| `--el-font-size-extra-large` | 20px | Page title | `<h1>`, page header |
| `--el-font-size-medium` | 16px | Section title, modal title, table header | Tab header, card title, dialog title, table `<th>` |
| `--font-size-base` | 14px | **Body text, form labels, auxiliary info** | Most text, inputs, buttons, description text |
| `--el-font-size-extra-small` | 12px | Status tags, timestamps | Timeline time, badge count |

### 3.3 Font Weight

| Token | Weight | Usage |
|-------|--------|-------|
| `--el-font-weight-primary` | 500 (medium) | All body text, form labels |
| `--el-font-weight-secondary` | 400 (regular) | Auxiliary text |
| Bold (700) | 700 | Page titles, section headers (via `font-weight: bold`) |

**Decision:** Only 2 active weights used: 400 (regular body) and 700 (headings/emphasis). Element Plus default `font-weight-primary` is 500 — override to 400 for body text consistency.

### 3.4 Line Height

| Context | Value |
|---------|-------|
| Body text | 1.5 |
| Headings | 1.2 |
| Table content | 1.4 |
| Form labels | 1.4 |

## 4. Color Contract

### 4.1 Element Plus Theme Colors

| Role | Token | Color | Usage |
|------|-------|-------|-------|
| Primary | `--el-color-primary` | #409EFF | Primary buttons, links, active state |
| Primary Light | `--el-color-primary-light-3` | #79BBFF | Hover state, badges |
| Primary Light BG | `--el-color-primary-light-9` | #ECF5FF | Table selected row, info banners |
| Success | `--el-color-success` | #67C23A | Approved/archived status, success text |
| Warning | `--el-color-warning` | #E6A23C | Pending/processing status, warnings |
| Danger | `--el-color-danger` | #F56C6C | Rejected/invalidated, delete buttons |
| Info | `--el-color-info` | #909399 | Draft status, auxiliary info |
| Text Primary | `--el-text-color-primary` | #303133 | Primary text |
| Text Regular | `--el-text-color-regular` | #606266 | Regular body text |
| Text Secondary | `--el-text-color-secondary` | #909399 | Secondary/placeholder text |
| Text Placeholder | `--el-text-color-placeholder` | #C0C4CC | Input placeholders |
| Border Base | `--el-border-color-base` | #DCDFE6 | Component borders |
| BG Page | `--el-bg-color-page` | #F5F7FA | Page background |
| BG Overlay | `--el-bg-color-overlay` | #FFFFFF | Modal/drawer background |

### 4.2 60/30/10 Distribution

| Tier | Coverage | Elements |
|------|----------|----------|
| **60% — Dominant (Surface)** | #F5F7FA (page bg) + #FFFFFF (card bg) | Page background, card backgrounds, form areas |
| **30% — Secondary** | #FFFFFF (white cards) + #F0F2F5 (table stripes) | Cards, side panels, table rows, input backgrounds |
| **10% — Accent (Primary #409EFF)** | #409EFF + its variants | Primary buttons, active tabs, selected items, links, status badges, notification badges |

### 4.3 Semantic Status Colors

| Status | Color | Hex | Used On |
|--------|-------|-----|---------|
| Draft | Info gray | `--el-color-info` | Draft tab, status tag |
| Pending Dept Review | Warning | `--el-color-warning` | Status tag, timeline node |
| Pending Admin Archive | Primary | `--el-color-primary` | Status tag, timeline node |
| Archived | Success | `--el-color-success` | Status tag, timeline node |
| Rejected | Danger | `--el-color-danger` | Status tag, timeline node |
| Invalidated | Danger/light | `--el-color-danger-light-5` | Status tag, timeline node |
| Withdrawn | Info | `--el-color-info` | Status tag |

### 4.4 Classified Marking Colors

| Classification Level | Color | Hex | Usage |
|---------------------|-------|-----|-------|
| 秘密 (Secret) | Orange | #E6A23C | Tag label on classified achievements |
| 机密 (Confidential) | Red | #F56C6C | Tag label on classified achievements |

### 4.5 Accent Reserved-For List

Accent (#409EFF) is **reserved exclusively for**:

1. Primary action buttons (提交, 通过, 保存)
2. Active/selected tab indicators
3. Navigation menu active item
4. Link text
5. Notification badge count
6. Focus ring on form inputs
7. Loading spinners
8. DOI preview dialog confirm button
9. Form field focus border

**No other elements use the primary accent color.** All decorative elements use neutral grays or status-semantic colors.

## 5. Copywriting Contract

All UI text in Chinese (user-facing language).

### 5.1 Registration Forms

#### 5.1.1 Paper Form (论文登记)

| Field | Label | Placeholder | Validation |
|-------|-------|-------------|------------|
| title | 论文标题 | 请输入论文标题 | Required, max 500 chars |
| authors | 作者 | 多个作者请用分号分隔 | Required |
| journal | 期刊/会议名称 | 请输入期刊或会议名称 | Required |
| doi | DOI | 输入DOI后移出输入框可自动补全 | Optional, format: `^10\.\d{4,}\/.*$` |
| issn | ISSN/CN | 如有请填写 | Optional |
| volume | 卷号 | 请输入卷号 | Optional, number |
| issue | 期号 | 请输入期号 | Optional, number |
| pages | 页码 | 如: 123-130 | Optional |
| publishYear | 发表年份 | 请选择发表年份 | Required |
| indexStatus | 收录情况 | 请选择收录情况 | Required |
| impactFactor | 影响因子 | 请输入影响因子 | Optional, precision 3 |
| zone | 中科院分区 | 请选择分区 | Optional |
| abstract | 摘要 | 请输入论文摘要 | Optional, max 2000 chars |

**收录情况 options:** SCI, SSCI, EI, CPCI, CSCD, CSSCI, 北大核心, 其他
**中科院分区 options:** 一区, 二区, 三区, 四区, 无

#### 5.1.2 Patent Form (专利登记)

| Field | Label | Placeholder | Validation |
|-------|-------|-------------|-----------|
| patentName | 专利名称 | 请输入专利名称 | Required, max 500 chars |
| inventors | 发明人 | 多个发明人请用分号分隔 | Required |
| applicationNo | 申请号 | 请输入专利申请号 | Required, unique check on submit |
| authorizationNo | 授权号 | 如有请填写 | Optional |
| applicationDate | 申请日 | 请选择申请日期 | Required |
| authorizationDate | 授权日 | 请选择授权日期 | Optional |
| patentType | 专利类型 | 请选择专利类型 | Required |
| country | 国别 | 请选择国别 | Required (default: 中国) |
| nextFeeDate | 年费下次缴费日 | 请选择下次缴费日期 | Optional |
| legalStatus | 法律状态 | 请选择法律状态 | Required |

**专利类型 options:** 发明, 实用新型, 外观设计
**国别 options:** 中国, 美国, 欧洲, 日本, 韩国, PCT, 其他
**法律状态 options:** 授权, 实审, 公开, 驳回, 撤回, 终止, 无效

#### 5.1.3 Software Copyright Form (软著登记)

| Field | Label | Placeholder | Validation |
|-------|-------|-------------|-----------|
| name | 软著名称 | 请输入软著名称 | Required, max 500 chars |
| copyrightHolder | 著作权人 | 请输入著作权人 | Required |
| registrationNo | 登记号 | 请输入登记号 | Required, unique check on submit |
| registrationDate | 登记日期 | 请选择登记日期 | Required |
| version | 版本号 | 请输入版本号 | Required |
| softwareCategory | 软件类别 | 请选择软件类别 | Required |

**软件类别 options:** 操作系统, 数据库, 中间件, 应用软件, 嵌入式软件, 其他

#### 5.1.4 Common Fields (All Types)

| Field | Label | Placeholder | Validation |
|-------|-------|-------------|-----------|
| projectRef | 所属课题 | 请输入课题名称/编号（自由文本） | Optional |
| isClassified | 涉密标记 | — | Switch toggle |
| classifiedLevel | 密级 | 请选择密级 | Shown only when isClassified=true |
| departmentId | 所属部门 | — | Auto-populated from user profile |
| departmentName | 所属部门 | — | Read-only display |

**密级 options:** 秘密, 机密
**Note:** 绝密 not available — out of scope for Phase 1

### 5.2 Action Buttons & Confirmation Dialogs

| Context | Button Label | Dialog Title | Dialog Message | Confirm Button | Cancel Button |
|---------|-------------|-------------|----------------|----------------|---------------|
| Form submit | 提交 | 确认提交 | 确认提交该成果进行审批？提交后草稿状态将不可恢复。 | 确认提交 | 取消 |
| Save draft | 保存草稿 | (No dialog) | (Inline success toast) | — | — |
| Switch achievement type | (Switch radio) | 切换确认 | 切换成果类型将清空当前填写内容，是否继续？ | 继续切换 | 取消 |
| DOI preview confirm | 确认填入 | (Preview dialog built-in) | (Shows matched fields side-by-side) | 确认填入 | 取消 |
| Approval pass | 通过 | 确认通过 | 确认通过该成果审批？通过后将进入下一审批节点。 | 确认通过 | 取消 |
| Approval reject | 退回 | 确认退回 | 请填写退回原因，退回后将通知提交人修改后重新提交。 | 确认退回 | 取消 |
| Withdraw approval | 撤回 | 确认撤回 | 确认撤回该审批申请？撤回后成果状态恢复为草稿。 | 确认撤回 | 取消 |
| Invalidate achievement | 作废 | 确认作废 | 确认作废该成果？此操作不可撤销。作废后仅创建人和系统管理员可见。 | 确认作废 | 取消 |
| Archive with number | 归档 | 分配归档号 | 请为该成果分配成果编号/归档号： | 确认归档 | 取消 |
| Duplicate detected | (Auto dialog) | 重复提示 | 检测到重复成果：该{DOI/申请号}已存在，请查看已有成果。 | 查看已有 | 继续填写 |
| Delete attachment | 删除 | 确认删除 | 确认删除该附件？此操作不可撤销。 | 确认删除 | 取消 |
| Upload error | (Toast) | — | 附件上传失败，请重试 | — | — |

### 5.3 Empty States

| Page | Empty State Copy | Icon | Action |
|------|-----------------|------|--------|
| Achievement List (active) | 暂无活跃成果，点击右上角"登记成果"开始登记 | `el-icon-document-add` | 登记成果 (link to register) |
| Achievement List (draft) | 暂无草稿 | `el-icon-edit-pen` | 登记成果 |
| Achievement List (invalidated) | 暂无已作废成果 | `el-icon-folder-delete` | — |
| Approval List (pending) | 暂无待审批项 | `el-icon-circle-check` | — |
| Notification Center (approval) | 暂无审批待办通知 | `el-icon-bell` | — |
| Notification Center (system) | 暂无系统通知 | `el-icon-message` | — |
| Batch Import (result) | 暂无导入记录 | `el-icon-upload-filled` | — |
| Attachment list | 暂无附件 | `el-icon-paperclip` | 上传附件 |
| Approval Timeline | 暂无审批记录 | `el-icon-clock` | — |

### 5.4 Error States

| Context | Error Copy | Secondary Copy | Action |
|---------|-----------|----------------|--------|
| DOI lookup failure | DOI 补全失败 | 请检查DOI格式或手动填写所有字段 | 重试 / 手动填写 |
| DOI lookup network error | DOI 补全服务暂时不可用 | 已自动切换备选数据源，或请稍后重试 | 手动填写 |
| Import failure (batch) | 文件导入失败 | {具体错误原因，如文件格式不正确/必填列缺失} | 重新上传 |
| Import partial errors | 部分行导入失败 | 共导入 {success} 行，{failed} 行失败，{skipped} 行跳过 | 下载错误报告 |
| Upload failure (attachment) | 附件上传失败 | {具体原因，如文件大小超过50MB/类型不支持} | 重新选择 |
| Network error | 请求失败 | 网络连接异常，请检查网络后重试 | 重试 |
| Submit error | 提交失败 | 请稍后重试，如持续失败请联系系统管理员 | 确定 |
| Approval failure | 操作失败 | 审批状态可能已发生变化，请刷新后重试 | 刷新 |

### 5.5 Reject Reason Quick-Select Options

| Reason | Used For |
|--------|----------|
| 信息填写不完整，请补充必填字段 | All types |
| 附件缺失，请上传相关证明文件 | All types |
| 成果不属于本部门管理范围 | All types |
| 格式不符合要求，请参考模板 | All types |
| 涉密等级分类有误，请调整 | All types |
| DOI/申请号格式不正确 | Paper/Patent |
| 论文未正式发表或录用 | Paper only |
| 专利法律状态信息不准确 | Patent only |
| 软著著作权人信息有误 | Copyright only |
| 其他原因（请手动填写） | All types |

### 5.6 Draft Save Confirmation

| Event | Toast Message | Duration |
|-------|--------------|----------|
| Draft saved | 草稿已保存 | 2s |
| Draft save failed | 草稿保存失败，请重试 | 3s |
| Draft loaded | 已恢复草稿，上次编辑时间：{time} | 3s (banner alert) |

### 5.7 Status Change Toast Notifications

| Event | Message | Type |
|-------|---------|------|
| Submit success | 提交成功！成果已进入审批流程 | success |
| Draft saved | 草稿已保存 | info |
| Approved (dept pass) | 已通过部门审核，等待管理员归档 | success |
| Rejected (dept) | 已退回至提交人 | warning |
| Archived | 已归档，成果编号：{archiveNo} | success |
| Withdrawn | 已撤回，成果恢复为草稿 | info |
| Invalidated | 成果已作废 | warning |
| Duplicate blocked | 提交失败：该{DOI/申请号}已存在 | error |

## 6. Screen Specifications

### 6.1 AchievementRegister.vue — 成果登记

**Path:** `/achievement/register`
**Layout:** Single-column centered form, max-width 960px

```
┌──────────────────────────────────────────────────────┐
│  成果登记                                              │
│  [○ 论文] [○ 专利] [○ 软件著作权]    (activeType radio) │
│  ┌──────────────────────────────────────────────────┐ │
│  │  PaperForm / PatentForm / CopyrightForm (动态)     │ │
│  │  ┌────────────────────────────────────────────┐   │ │
│  │  │  Fields listed in §5.1 per type             │   │ │
│  │  └────────────────────────────────────────────┘   │ │
│  │  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  │ │
│  │  涉密标记  [Toggle]  [密级选择 ▼] (conditional)    │ │
│  │  所属课题  [_____________________________]         │ │
│  │  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  │ │
│  │  附件                                           │ │
│  │  ┌─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐    │ │
│  │  │  Drag & Drop or Click to Upload           │ │  │
│  │  │  支持 PDF/Word/Excel/图片/压缩包           │ │  │
│  │  │  单文件不超过 50MB，不限制数量              │ │  │
│  │  └─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘    │ │
│  │  ┌────────────────────────────────────────────┐ │ │
│  │  │  [已上传文件列表] 文件名 大小 删除按钮       │ │ │
│  │  └────────────────────────────────────────────┘ │ │
│  │                                                  │ │
│  │  [提交]  [保存草稿]                               │ │
│  └──────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────┘
```

**States & Interactions:**

| Interaction | Behavior |
|-------------|----------|
| Type switch | Show confirmation dialog (D-01). Clear form data on confirm. Do NOT clear if draft exists for that type. |
| DOI blur | Call API. Show inline `el-icon-loading` next to DOI field. On result: open `DoiPreviewDialog`. On fail: show warning toast "DOI 补全失败，请检查格式或手动填写" (D-10, D-13, D-15). |
| DOI preview confirm | Apply matched fields to form. Show success toast. |
| DOI preview cancel | Close dialog. Leave DOI field value as entered. |
| Submit click | Validate all required fields. Check duplicate (D-45). If duplicate: show DuplicateDialog. If pass: submit API, show success toast, reset form, keep same type (D-07). |
| Save draft click | Validate only title/name (for identification). Save current state. Show toast "草稿已保存". |
| Attachment upload | Validate file type and size client-side. Upload via file proxy API. Show progress bar. |
| Classified toggle on | Show classifiedLevel select (D-06). |
| Classified toggle off | Hide classifiedLevel select, clear value. |

**DoiPreviewDialog:**

```
┌─────────────────────────────────────────┐
│  DOI 补全结果预览                        │
│  ┌─────────────────────────────────────┐│
│  │  DOI: 10.xxxx/xxxx                  ││
│  │  标题: ✓ 自动匹配 = {title}          ││
│  │  作者: ✓ 自动匹配 = {authors}        ││
│  │  期刊: ✓ 自动匹配 = {journal}        ││
│  │  卷号: ✓ 自动匹配 = {volume}         ││
│  │  期号: ✓ 自动匹配 = {issue}          ││
│  │  页码: ✓ 自动匹配 = {pages}          ││
│  │  发表年份: ✓ 自动匹配 = {year}        ││
│  │  摘要: ✓ 自动匹配                    ││
│  │                               (大小) ││
│  │  [确认填入]  [取消]                   ││
│  └─────────────────────────────────────┘│
└─────────────────────────────────────────┘
```

### 6.2 AchievementList.vue — 成果列表

**Path:** `/achievement/list`
**Layout:** Full-width table with top filter bar

```
┌──────────────────────────────────────────────────────────┐
│  成果列表                                                  │
│  ┌──────┬──────┬──────────┐                               │
│  │ 活跃 │ 草稿 │ 已作废    │  (tabs — el-tabs)             │
│  ├──────┴──────┴──────────┤                               │
│  │  [成果类型 ▼]  [提交时间]  [关键词搜索]  [🔍]            │
│  ├──────────────────────────────────────────────────────┤ │
│  │  ┌────────────────────────────────────────────────┐  │ │
│  │  │  # │ 标题 │ 类型  │ 状态  │ 提交人  │ 提交时间  │  │ │
│  │  ├────────────────────────────────────────────────┤  │ │
│  │  │  1 │ xxx  │ 论文  │ 待审核 │ 张三    │ 2026-06 │  │ │
│  │  │  2 │ xxx  │ 专利  │ 已归档 │ 李四    │ 2026-06 │  │ │
│  │  │  ...                                          │  │ │
│  │  └────────────────────────────────────────────────┘  │ │
│  │  分页: [<] [1] [2] [3] [...] [>]  共 N 条            │ │
│  └──────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
```

**Default:** Active tab (tabs: 活跃 | 草稿 | 已作废)

**Status Tags Mapping:**
| Status | Tag Type | Label |
|--------|----------|-------|
| DRAFT | info | 草稿 |
| PENDING_DEPT_REVIEW | warning | 待部门审核 |
| PENDING_ADMIN_ARCHIVE | primary | 待管理员归档 |
| ARCHIVED | success | 已归档 |
| REJECTED | danger | 已退回 |
| INVALIDATED | info (light) | 已作废 |
| WITHDRAWN | info | 已撤回 |

**Filters:**
| Filter | Component | Behavior |
|--------|-----------|----------|
| Achievement Type | `el-select` | All / Paper / Patent / Software Copyright |
| Date Range | `el-date-picker` (type="daterange") | Submit time range |
| Keyword | `el-input` | Search title, debounced 300ms |
| Reset Button | `el-button` | Clear all filters |

**Row click:** Navigate to AchievementDetail page.

### 6.3 AchievementDetail.vue — 成果详情

**Path:** `/achievement/detail/:id`
**Layout:** Above-fold action bar + below tabs

```
┌──────────────────────────────────────────────────────────┐
│  [← 返回列表]                              (breadcrumb)  │
│                                                          │
│  论文标题: {title}                    [状态标签] [密级标签] │
│  ┌──────────────────────────────────────────────────────┐│
│  │  [编辑] [撤回] [作废]   (动作栏，按状态动态显示)       ││
│  └──────────────────────────────────────────────────────┘│
│                                                          │
│  ┌──────────────────────────────────────────────────────┐│
│  │  [基本信息]  [审批进度]  [附件]  [操作日志]   (tabs)  ││
│  ├──────────────────────────────────────────────────────┤│
│  │                                                      ││
│  │  Tab Content (根据选中tab动态渲染)                     ││
│  │                                                      ││
│  └──────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────┘
```

**Tab: Basic Info**
| Section | Fields | Layout |
|---------|--------|--------|
| Basic Info | All registration fields (read-only for non-draft) | Description list (label: value) |
| Classification | 涉密标记, 密级 (if classified) | Description list |
| Project Link | 所属课题 | Description list |

**Tab: Approval Progress**
- `ApprovalTimeline` component (see §6.5)

**Tab: Attachments**
| Column | Content |
|--------|---------|
| File Name | Display name |
| File Size | Formatted (KB/MB) |
| Upload Time | Datetime |
| Uploader | User name |
| Action | [下载] [删除] (delete only for draft/rejected status) |

**Tab: Operation Log**
| Column | Content |
|--------|---------|
| Time | Datetime |
| Operator | User name |
| Action | Operation description |
| Detail | Additional info |

**Top Action Bar — Dynamic by Status:**
| Status | Visible Actions |
|--------|-----------------|
| DRAFT | 编辑 (link to register with draft loaded) |
| PENDING_DEPT_REVIEW | 撤回 |
| PENDING_ADMIN_ARCHIVE | 撤回 (submitter only) |
| REJECTED | 编辑 (link to register with rejected data loaded) |
| ARCHIVED | 作废 (for creator/secretary only), 下载附件 |
| INVALIDATED | (view only) |

### 6.4 ApprovalList.vue — 待审批列表

**Path:** `/approval/pending`
**Layout:** Full-width table with top filter bar

```
┌──────────────────────────────────────────────────────────┐
│  审批待办                                                  │
│  [成果类型 ▼]  [提交时间 ▼]  [关键词搜索]  [🔍]           │
│  ┌──────────────────────────────────────────────────────┐│
│  │  # │ 标题 │ 类型  │ 提交人  │ 所属部门  │ 提交时间  │  ││
│  ├──────────────────────────────────────────────────────┤│
│  │  1 │ xxx  │ 论文  │ 张三    │ 信息所    │ 2026-06  │  ││
│  │  2 │ xxx  │ 专利  │ 李四    │ 材料所    │ 2026-06  │  ││
│  │  ...                                              │  ││
│  └──────────────────────────────────────────────────────┘│
│  分页: [<] [1] [2] [3] [...] [>]  共 N 条                │
└──────────────────────────────────────────────────────────┘
```

**Row click:** Navigate to ApprovalDetail page.

### 6.5 ApprovalDetail.vue — 审批详情

**Path:** `/approval/detail/:id`
**Layout:** Left-right split (left 60%, right 40%/fixed)

```
┌─────────────────────────────────────────────────────────────┐
│  [← 返回待办列表]                                            │
│  ┌─────────────────────────┬──────────────────────────────┐ │
│  │  LEFT (scrollable)      │  RIGHT (fixed, not scroll)   │ │
│  │                         │                              │ │
│  │  ┌──────────────────┐   │  **************************  │ │
│  │  │  Title            │   │  * 审批操作                *  │ │
│  │  │  Status Tag       │   │  *                        *  │ │
│  │  │  Table: Key=Value │   │  *  [通过]  [退回]         *  │ │
│  │  │  (all registration │   │  *                        *  │ │
│  │  │   fields, read-only)│   │  *  退回原因:             *  │ │
│  │  └──────────────────┘   │  *  [▼常用原因]             *  │ │
│  │                         │  *  [_______________]       *  │ │
│  │  ┌──────────────────┐   │  *                        *  │ │
│  │  │  附件列表          │   │  *  (退回原因仅在点击退回 │  │ │
│  │  │  [文件1] [下载]   │   │  *   后显示)              *  │ │
│  │  └──────────────────┘   │  **************************  │ │
│  │                         │  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  │ │
│  │  ┌──────────────────┐   │  **************************  │ │
│  │  │  审批时间线        │   │  * 审批历史                *  │ │
│  │  │  ● 提交 张三 --- │   │  * ● 提交 张三             *  │ │
│  │  │  ○ 待审核        │   │  * ○ 待审核                *  │ │
│  │  └──────────────────┘   │  **************************  │ │
│  └─────────────────────────┴──────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

**Right Panel — Approval Action Section (D-22, D-23):**
- Top: 审批操作 (section header)
- Pass/Reject buttons (large, full-width)
- Reject reason textarea (hidden by default, shown on clicking 退回)
- Quick-select options (el-select or chip group)
- Confirmation on both pass and reject

**Right Panel — Approval History Section (D-26):**
- Timeline-style list showing each step

### 6.6 BatchImport.vue — 批量导入

**Path:** `/batch-import`
**Layout:** Centered card layout

```
┌──────────────────────────────────────────────────────────┐
│  批量导入                                                  │
│                                                          │
│  ┌──────────────────────────────────────────────────────┐│
│  │  第一步：下载模板                                      ││
│  │  [下载Excel导入模板]                                    ││
│  │  模板包含论文/专利/软著所有字段，通过"类型"列区分        ││
│  └──────────────────────────────────────────────────────┘│
│                                                          │
│  ┌──────────────────────────────────────────────────────┐│
│  │  第二步：上传文件                                      ││
│  │  ┌─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐ ││
│  │  │  将Excel文件拖拽到此处，或点击上传                 │  ││
│  │  │  支持 .xlsx / .xls 格式                          │  ││
│  │  └─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘ ││
│  └──────────────────────────────────────────────────────┘│
│                                                          │
│  ┌──────────────────────────────────────────────────────┐│
│  │  导入结果 (显示在文件上传处理后)                       ││
│  │                                                      ││
│  │  共 {total} 行，成功导入 {success} 行                 ││
│  │  失败 {failed} 行，跳过重复 {skipped} 行               ││
│  │                                                      ││
│  │  [下载错误报告]  （仅在有失败行时显示）                 ││
│  └──────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────┘
```

### 6.7 NotificationCenter.vue — 通知中心

**Path:** `/notification`
**Layout:** Full-width list with tabs

```
┌──────────────────────────────────────────────────────────┐
│  通知中心                                                  │
│  ┌────────────┬──────────┐                                │
│  │ 审批待办   │ 系统通知  │  (el-tabs)                    │
│  ├────────────┴──────────┤                                │
│  │  ┌────────────────────────────────────────────────┐   │
│  │  │  [审批图标] 成果《xxx》已进入待审批队列         │   │
│  │  │            张三 · 2026-06-16 09:00        [未读]│   │
│  │  ├────────────────────────────────────────────────┤   │
│  │  │  [审批图标] 您的成果《xxx》已通过审核           │   │
│  │  │            管理员 · 2026-06-16 08:00      [已读]│   │
│  │  ├────────────────────────────────────────────────┤   │
│  │  │  [系统图标] 系统将于今晚23:00进行维护           │   │
│  │  │            系统 · 2026-06-15 18:00        [已读]│   │
│  │  └────────────────────────────────────────────────┘   │
│  │  分页: [<] [1] [2] [3] [...] [>]  共 N 条            │
│  └──────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
```

**NotificationBell.vue (Navbar Component):**

```
┌─────────────────────┐
│  [🔔] [{unreadCount}]  │  ← Navbar right section
└─────────────────────┘
```
- Click opens NotificationCenter page (not dropdown)
- Badge shows count; hidden when count = 0
- 30-second polling for count update (D-27, D-53, D-56)

### 6.8 AchievementTimeline.vue — 审批时间线

Reusable component used in:
- AchievementDetail (Tab: 审批进度)
- ApprovalDetail (Right panel: 审批历史)

**Node Types & Visual:**
| Node | Color | Icon |
|------|-------|------|
| 提交 (Submit) | Primary | `el-icon-upload` |
| 通过 (Pass) | Success | `el-icon-circle-check` |
| 退回 (Reject) | Danger | `el-icon-circle-close` |
| 归档 (Archive) | Success | `el-icon-folder-checked` |
| 撤回 (Withdraw) | Warning | `el-icon-refresh-left` |
| 作废 (Invalidate) | Info | `el-icon-delete` |
| 当前待审 (Current Pending) | Primary (dashed) | `el-icon-time` |

### 6.9 DuplicateDialog.vue — 重复检测弹窗

```
┌────────────────────────────────────────────────┐
│  ⚠️ 重复提示                                     │
│                                                  │
│  检测到重复成果：                                  │
│  ┌────────────────────────────────────────────┐  │
│  │  已有成果：                                  │  │
│  │  标题：{existingTitle}                      │  │
│  │  类型：{existingType}                       │  │
│  │  状态：{existingStatus}                     │  │
│  │  提交时间：{existingTime}                   │  │
│  └────────────────────────────────────────────┘  │
│                                                  │
│  您正在登记的成果与上述成果{DOI/申请号}相同。       │
│                                                  │
│  [查看已有成果]  [继续填写并提交]                    │
└────────────────────────────────────────────────┘
```

## 7. Interaction Patterns

### 7.1 Loading States

| Context | Component | Pattern |
|---------|-----------|---------|
| Page initial load | `el-skeleton` | Show 3-5 skeleton rows matching table/ form layout |
| DOI lookup | Inline spinner | `el-icon-loading` next to DOI input field |
| Form submit | Button loading | `el-button loading` on 提交 button, disable all form actions |
| File upload | Progress bar | `el-progress` percentage bar per file |
| Batch import | Full page loading | `el-loading` directive on import card area |
| Approval action | Button loading | `el-button loading` on active button |
| List refresh | Table body loading | `v-loading` directive on `el-table` |

### 7.2 Error States

| Context | Pattern | Recovery |
|---------|---------|----------|
| API call failure | `ElMessage.error()` | Auto-dismiss 3s |
| Network offline | `ElMessage({ type: 'warning', duration: 0 })` | Persistent until dismissed |
| Validation error | `el-form-item__error` inline | Correct after field focus |
| Upload failed | File-level error icon + tooltip | Re-upload button |
| Import failed | Result card with error details | Download error report |

### 7.3 Confirmation Dialogs

| Type | Component | Notes |
|------|-----------|-------|
| Destructive (invalidate, withdraw, delete) | `ElMessageBox.confirm` | Type: warning, confirmButtonText: 确认, cancelButtonText: 取消 |
| Status change (submit, approve, reject) | `ElMessageBox.confirm` | Type: info, confirmButtonText: 确认, cancelButtonText: 取消 |
| Type switch | `ElMessageBox.confirm` | Type: warning, confirmButtonText: 继续切换, cancelButtonText: 取消 |
| Duplicate detected | Custom `DuplicateDialog` | Custom slot content, two action buttons |

### 7.4 Toast Notifications

| Priority | Component | Style | Duration |
|----------|-----------|-------|----------|
| Success | `ElMessage.success()` | Green | 2s |
| Warning | `ElMessage.warning()` | Orange | 3s |
| Error | `ElMessage.error()` | Red | 4s |
| Info | `ElMessage.info()` | Gray | 2s |

### 7.5 Transitions

| Context | Pattern | Duration |
|---------|---------|----------|
| Tab switch (list) | `el-tab-pane` default | 200ms |
| Tab switch (detail) | `el-tab-pane` default | 200ms |
| Route navigation | Vue Router default | 200ms |
| Modal open/close | Element Plus default | 200ms |
| Loading state | Immediate (no delay) | 0ms |
| Filter change | Immediate refresh | 0ms |

## 8. Responsive Behavior

### 8.1 Minimum Supported Width

**1366px** — Enterprise standard minimum resolution.

### 8.2 Breakpoints

Not applicable for Phase 1. All screens designed for desktop-only (1366px+).

### 8.3 Behavior at <1366px

- ApprovalDetail: Left-right split stacks to top-bottom
- All tables show horizontal scrollbar instead of truncation
- Form label width reduces from 120px to 100px
- Multi-column filters wrap to single column

## 9. Component Tree

```
Layout (Vue Pure Admin)
├── Sidebar
│   ├── NavItem: 成果登记 (-> /achievement/register)
│   ├── NavItem: 成果列表 (-> /achievement/list)
│   ├── NavItem: 审批待办 (-> /approval/pending)
│   └── NavItem: 批量导入 (-> /batch-import)
├── Header
│   └── NotificationBell.vue
│       ├── ElBadge (unread count)
│       └── ElIcon (bell icon → click to /notification)
└── Content
    ├── AchievementRegister.vue
    │   ├── ElRadioGroup (type selector)
    │   ├── ElForm
    │   │   ├── PaperForm.vue (conditional)
    │   │   │   ├── ElInput (title)
    │   │   │   ├── DoiAutoComplete.vue
    │   │   │   │   ├── ElInput (doi)
    │   │   │   │   └── ElIcon (loading spinner)
    │   │   │   ├── ElInput (authors, journal, issn, pages)
    │   │   │   ├── ElInputNumber (volume, issue, impactFactor)
    │   │   │   ├── ElDatePicker (publishYear)
    │   │   │   ├── ElSelect (indexStatus, zone)
    │   │   │   └── ElInput (abstract, type="textarea")
    │   │   ├── PatentForm.vue (conditional)
    │   │   │   ├── ElInput (patentName, inventors, applicationNo, authorizationNo)
    │   │   │   ├── ElDatePicker (applicationDate, authorizationDate, nextFeeDate)
    │   │   │   ├── ElSelect (patentType, country, legalStatus)
    │   │   │   └── ElInput (annotations)
    │   │   ├── CopyrightForm.vue (conditional)
    │   │   │   ├── ElInput (name, copyrightHolder, registrationNo, version)
    │   │   │   ├── ElDatePicker (registrationDate)
    │   │   │   └── ElSelect (softwareCategory)
    │   │   ├── ElFormItem (isClassified)
    │   │   │   ├── ElSwitch
    │   │   │   └── ElSelect (classifiedLevel, conditional)
    │   │   ├── ElFormItem (projectRef)
    │   │   │   └── ElInput
    │   │   ├── AttachmentUploader.vue
    │   │   │   ├── ElUpload (drag & drop)
    │   │   │   └── ElTable (uploaded files list)
    │   │   └── ElFormItem (actions)
    │   │       ├── ElButton (submit, type="primary")
    │   │       └── ElButton (save draft)
    │   └── DoiPreviewDialog.vue
    │       ├── ElDialog
    │       ├── ElDescriptions (preview data)
    │       └── ElButton (confirm/cancel)
    │
    ├── AchievementList.vue
    │   ├── ElTabs (active/draft/invalidated)
    │   ├── ElForm (filter bar)
    │   │   ├── ElSelect (achievement type)
    │   │   ├── ElDatePicker (date range)
    │   │   ├── ElInput (keyword search)
    │   │   └── ElButton (reset)
    │   ├── ElTable
    │   │   ├── ElTableColumn (title)
    │   │   ├── ElTableColumn (type)
    │   │   ├── ElTableColumn (status → ElTag)
    │   │   ├── ElTableColumn (submitter)
    │   │   └── ElTableColumn (submit time)
    │   └── ElPagination
    │
    ├── AchievementDetail.vue
    │   ├── ElBreadcrumb
    │   ├── ElTag (status)
    │   ├── ElTag (classified level, conditional)
    │   ├── ActionBar
    │   │   └── ElButton[] (dynamic per status)
    │   ├── ElTabs
    │   │   ├── ElTabPane (基本信息)
    │   │   │   └── ElDescriptions
    │   │   ├── ElTabPane (审批进度)
    │   │   │   └── AchievementTimeline.vue
    │   │   │       └── ElTimeline
    │   │   │           └── ElTimelineItem[]
    │   │   ├── ElTabPane (附件)
    │   │   │   ├── ElTable (file list)
    │   │   │   └── ElButton[] (download/delete)
    │   │   └── ElTabPane (操作日志)
    │   │       └── ElTable (operation logs)
    │   └── InvalidConfirmDialog (conditional)
    │
    ├── ApprovalList.vue
    │   ├── ElForm (filter bar — same style as achievement list)
    │   ├── ElTable
    │   │   ├── ElTableColumn (title)
    │   │   ├── ElTableColumn (type)
    │   │   ├── ElTableColumn (submitter)
    │   │   ├── ElTableColumn (department)
    │   │   └── ElTableColumn (submit time)
    │   └── ElPagination
    │
    ├── ApprovalDetail.vue
    │   ├── ElBreadcrumb
    │   ├── SplitLayout (left 60%, right 40%)
    │   │   ├── LeftPanel (scrollable)
    │   │   │   ├── ElDescriptions (achievement info, read-only)
    │   │   │   ├── ElTable (attachments with download)
    │   │   │   └── AchievementTimeline.vue
    │   │   └── RightPanel (fixed)
    │   │       ├── ApprovalActions.vue
    │   │       │   ├── ElButton (pass, type="primary", full-width)
    │   │       │   ├── ElButton (reject, type="danger", full-width)
    │   │       │   ├── ElSelect (quick-select reject reasons, conditional)
    │   │       │   └── ElInput (reject reason, type="textarea", conditional)
    │   │       └── ElDivider
    │   │           └── AchievementTimeline.vue (mini version)
    │
    ├── BatchImport.vue
    │   ├── ElCard (step 1: 下载模板)
    │   │   └── ElButton (download template, type="primary")
    │   ├── ElCard (step 2: 上传文件)
    │   │   └── ElUpload (drag and drop)
    │   └── ElCard (step 3: 导入结果, conditional)
    │       ├── ElResult (success/warning icon)
    │       ├── ElText (summary stats)
    │       └── ElButton (download error report, conditional)
    │
    └── NotificationCenter.vue
        ├── ElTabs (审批待办 / 系统通知)
        └── NotificationList
            └── NotificationItem.vue (repeated)
                ├── ElIcon (type icon)
                ├── ElText (title)
                ├── ElText (time)
                └── ElTag (read/unread status)
```

## 10. Route Design

### 10.1 Phase 1 Routes

```typescript
const routes = [
  {
    path: '/achievement',
    meta: { title: '成果管理', icon: 'Document' },
    redirect: '/achievement/register',
    children: [
      {
        path: 'register',
        name: 'AchievementRegister',
        meta: { title: '成果登记', icon: 'Edit' },
        component: () => import('@/views/achievement/AchievementRegister.vue')
      },
      {
        path: 'list',
        name: 'AchievementList',
        meta: { title: '成果列表', icon: 'List' },
        component: () => import('@/views/achievement/AchievementList.vue')
      },
      {
        path: 'detail/:id',
        name: 'AchievementDetail',
        meta: { title: '成果详情', hidden: true },
        component: () => import('@/views/achievement/AchievementDetail.vue')
      }
    ]
  },
  {
    path: '/approval',
    meta: { title: '审批管理', icon: 'Check' },
    redirect: '/approval/pending',
    children: [
      {
        path: 'pending',
        name: 'ApprovalPending',
        meta: { title: '审批待办', icon: 'Clock' },
        component: () => import('@/views/approval/ApprovalList.vue')
      },
      {
        path: 'detail/:id',
        name: 'ApprovalDetail',
        meta: { title: '审批详情', hidden: true },
        component: () => import('@/views/approval/ApprovalDetail.vue')
      }
    ]
  },
  {
    path: '/batch-import',
    name: 'BatchImport',
    meta: { title: '批量导入', icon: 'Upload' },
    component: () => import('@/views/batch/BatchImport.vue')
  },
  {
    path: '/notification',
    name: 'NotificationCenter',
    meta: { title: '通知中心', hidden: true },
    component: () => import('@/views/notification/NotificationCenter.vue')
  }
]
```

### 10.2 Route Guard Behaviors

| Guard | Behavior |
|-------|----------|
| Auth check | Redirect to login if not authenticated |
| Role check | Sidebar rendering via Vue Pure Admin role-based menu (Phase 0 RBAC) |
| 404 | Catch-all redirect to 404 page |
| Detail page | `hidden: true` in meta — not shown in sidebar |

### 10.3 Link Relationships

| From | To | Trigger |
|------|----|---------|
| AchievementList row | AchievementDetail | Row click |
| ApprovalList row | ApprovalDetail | Row click |
| Notification item (approval) | ApprovalDetail | Item click |
| Notification item (system) | (void, mark-as-read only) | Item click |
| DuplicateDialog "查看已有" | AchievementDetail | Button click |
| "编辑" in detail action bar | AchievementRegister (with draft data) | Button click |
| NotificationBell | NotificationCenter | Bell icon click |

## 11. Global Component States Matrix

| Component | Loading | Empty | Error | Edge Case |
|-----------|---------|-------|-------|-----------|
| AchievementRegister | Skeleton on initial load | — | Submit failure toast | Type switch with unsaved data |
| PaperForm | DOI loading spinner | No form data | Validation inline | DOI format edge cases |
| PatentForm | — | No form data | Validation inline | — |
| CopyrightForm | — | No form data | Validation inline | — |
| DoiAutoComplete | Inline spinner | No results found toast | Network error toast | Fallback source behavior |
| DoiPreviewDialog | — | (Only shown when data exists) | — | All-fields-empty edge case |
| AttachmentUploader | Per-file progress | Empty upload area | Upload failure per-file | Max file size exceeded |
| AchievementList | Table body loading | Empty state per tab | Fetch error toast | Tab switch with stale data |
| ApprovalList | Table body loading | Empty state | Fetch error toast | Real-time status change |
| AchievementDetail | Page loading | — | Fetch error toast | Archive-no input validation |
| ApprovalDetail | Page loading | — | Action failure toast | Concurrent approval |
| BatchImport | Full page loading during import | No import history | Import error report | Partial import with mixed errors |
| NotificationCenter | List loading | Empty state per tab | Fetch error toast | Click-to-mark-as-read optimization |
| ApprovalHistory | — | Empty: "暂无审批记录" | — | Long timeline with many entries |

## 12. Data Flow & State Management

### 12.1 Pinia Stores

```typescript
// stores/achievement.ts
export const useAchievementStore = defineStore('achievement', () => {
  const currentType = ref<'paper' | 'patent' | 'copyright'>('paper')
  const formData = ref<PaperForm | PatentForm | CopyrightForm | null>(null)
  const drafts = ref<DraftItem[]>([])
  const currentDraftId = ref<string | null>(null)

  // Actions
  const switchType = (type: string) => { /* reset form */ }
  const loadDraft = (id: string) => { /* populate form */ }
  const saveDraft = () => { /* persist to API */ }
  const resetForm = () => { /* clear all fields */ }

  return { currentType, formData, drafts, currentDraftId, switchType, loadDraft, saveDraft, resetForm }
})

// stores/approval.ts
export const useApprovalStore = defineStore('approval', () => {
  const pendingCount = ref(0)
  const filters = ref({ type: null, dateRange: null, keyword: '' })

  const fetchPendingCount = async () => { /* API call */ }
  const approve = async (id: number, comment: string) => { /* API call */ }
  const reject = async (id: number, reason: string) => { /* API call */ }

  return { pendingCount, filters, fetchPendingCount, approve, reject }
})

// stores/notification.ts
export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  let pollingTimer: number | null = null

  const startPolling = () => { /* 30s interval */ }
  const stopPolling = () => { /* clear interval */ }
  const markAsRead = async (id: number) => { /* API call */ }

  return { unreadCount, startPolling, stopPolling, markAsRead }
})
```

### 12.2 Form Data Flow

```
User Input → PaperForm/PatentForm/CopyrightForm (v-model)
  → AchievementRegister.formData (parent v-model)
    → Submit: API POST /api/{type}/submit
    → Save Draft: API POST /api/achievement/draft
    → Load Draft: API GET /api/achievement/draft/{id}
```

### 12.3 Approval Data Flow

```
ApprovalList: API GET /api/approval/pending?page=&size=&type=&date=
  → Row click → ApprovalDetail: API GET /api/approval/{id}
    → Approve: API POST /api/approval/{id}/approve { comment }
    → Reject: API POST /api/approval/{id}/reject { reason }
      → notify submitter (backend → notification table → polling)
```

## 13. Accessibility Notes (Chinese Enterprise Context)

| Requirement | Implementation |
|-------------|---------------|
| Form label association | All `el-form-item` have `label` prop, labels visually aligned left |
| Keyboard navigation | Element Plus default: Tab through form fields, Enter to submit |
| Color contrast | Element Plus default colors pass WCAG AA for text on white bg |
| Screen reader | Element Plus ARIA attributes on all components |
| Focus visible | Element Plus default focus ring styles |
| Error announcement | Inline validation messages below fields, not just visual |

## 14. Registry Note

**Design system:** Element Plus + Vue Pure Admin (not shadcn).
**Third-party registries:** Not applicable (no shadcn registry pattern used).
**Component sourcing:** All components from Element Plus official package; custom components built in-house.

---

## Appendix: Design Decisions Source Map

| Section | Pre-populated From | User Confirmed |
|---------|-------------------|----------------|
| Design System | CLAUDE.md (locked stack) | — |
| Spacing Scale | Element Plus defaults | — |
| Typography | Element Plus defaults + Chinese font conventions | — |
| Color | Element Plus theme + status semantics from CONTEXT.md | — |
| Copywriting (Register) | CONTEXT.md D-01~09, REQUIREMENTS.md REG-01~03 | — |
| Copywriting (Approval) | CONTEXT.md D-22~33, D-25 reject reasons | — |
| Copywriting (Empty/Error) | Standard defaults | — |
| Screen Specs | All 56 decisions in CONTEXT.md compiled | — |
| Interaction Patterns | CONTEXT.md D-10~15 (DOI), D-45~47 (duplicate) | — |
| Component Tree | RESEARCH.md Architecture Patterns | — |
| Route Design | RESEARCH.md Patterns section | — |
| Notification | CONTEXT.md D-52~56 | — |
| Registry | Not applicable (Element Plus) | — |

---

## Checker Sign-Off

- [x] Dimension 1 Copywriting: PASS
- [x] Dimension 2 Visuals: PASS
- [x] Dimension 3 Color: PASS
- [x] Dimension 4 Typography: PASS (fixed: reduced from 6 to 4 sizes)
- [x] Dimension 5 Spacing: PASS (fixed: 22px → 24px)
- [x] Dimension 6 Registry Safety: PASS (Element Plus ecosystem)

**Approval:** approved 2026-06-16

---

*UI-SPEC verified and approved.*
