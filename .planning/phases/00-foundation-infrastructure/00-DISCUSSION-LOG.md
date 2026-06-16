# Phase 0: Foundation & Infrastructure - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-16
**Phase:** 0-Foundation & Infrastructure
**Areas discussed:** Project Module Architecture, User/Role/Permission Design, JWT & Session Configuration, Audit Log/File Proxy/Backup, API Integration Framework, Navigation Menu Structure, Login Page Design, Classified Data Schema

---

## Project Module Architecture

| Option | Description | Selected |
|--------|-------------|----------|
| 4+1 模块 | achievement-web + achievement-common + achievement-framework + achievement-module/* | ✓ |
| 前后端两模块 | 前端和后端各自独立模块 | |
| 单模块 | 所有代码在一个 Maven 模块中 | |

**User's choice:** 4+1 模块
**Notes:** 项目命名为 achievement-xxx 风格，包名 com.institute.achievement.*

| Option | Description | Selected |
|--------|-------------|----------|
| achievement-xxx | 如 achievement-common | ✓ |
| ipms-xxx | 使用 IPMS 缩写 | |

**User's choice:** achievement-xxx

| Option | Description | Selected |
|--------|-------------|----------|
| Maven Wrapper + Docker | Maven Wrapper + Docker Compose 管理 MySQL/Redis | ✓ |
| 本地直接安装 | 本地安装 Maven/MySQL/Redis | |

**User's choice:** Maven Wrapper + Docker

| Option | Description | Selected |
|--------|-------------|----------|
| Monorepo | 前端后端同仓库 | ✓ |
| 分仓库管理 | 前端后端独立仓库 | |

**User's choice:** Monorepo

| Option | Description | Selected |
|--------|-------------|----------|
| Vue Pure Admin | 基于模板搭建 | ✓ |
| 从零搭建 | 手动搭建 Vite + Element Plus | |

**User's choice:** Vue Pure Admin

---

## User/Role/Permission Design

| Option | Description | Selected |
|--------|-------------|----------|
| 表格+抽屉编辑 | 标准表格+搜索+抽屉编辑 | ✓ |
| 卡片视图 | 卡片展示+独立详情页 | |

**User's choice:** 表格+抽屉编辑

| Option | Description | Selected |
|--------|-------------|----------|
| 菜单级权限 | 控制到菜单/路由级别 | ✓ |
| 按钮/API 级权限 | 精细到按钮和 API | |

**User's choice:** 菜单级权限

| Option | Description | Selected |
|--------|-------------|----------|
| 平级部门 | 无层级关系 | ✓ |
| 树形层级部门 | 父级/子级关系 | |

**User's choice:** 平级部门

| Option | Description | Selected |
|--------|-------------|----------|
| 覆盖更新 | 匹配工号则更新，新记录插入 | ✓ |
| 仅新增 | 已存在跳过不更新 | |

**User's choice:** 覆盖更新

| Option | Description | Selected |
|--------|-------------|----------|
| 左树右表分组管理 | 左侧字典分类树+右侧字典项列表 | ✓ |
| 统一平铺列表 | 通过"类型"列区分展示 | |

**User's choice:** 左树右表分组管理

| Option | Description | Selected |
|--------|-------------|----------|
| 用户编辑时选择角色 | 弹窗中选择 | ✓ |
| 独立分配界面 | 单独的角色分配页面 | |

**User's choice:** 用户编辑时选择角色

| Option | Description | Selected |
|--------|-------------|----------|
| 标准密码策略 | 8位+字母+数字 | ✓ |
| 高强度策略 | 10位+大小写+特殊字符+90天过期 | |

**User's choice:** 标准密码策略

| Option | Description | Selected |
|--------|-------------|----------|
| 首次运行引导创建 | 第一次访问时创建管理员 | |
| 预置默认管理员 | 预置 admin 账号，首次登录强制改密 | ✓ |

**User's choice:** 预置默认管理员

| Option | Description | Selected |
|--------|-------------|----------|
| 邮箱重置 | 通过邮箱发送重置链接 | |
| 管理员手动重置 | 联系系统管理员 | ✓ |

**User's choice:** 管理员手动重置

| Option | Description | Selected |
|--------|-------------|----------|
| 失败锁定 | 5次失败锁定30分钟 | ✓ |
| 不限制 | 无登录失败限制 | |

**User's choice:** 失败锁定

| Option | Description | Selected |
|--------|-------------|----------|
| 提供个人中心 | 用户可修改基本信息+密码 | ✓ |
| 完全由管理员维护 | 用户信息只能由管理员编辑 | |

**User's choice:** 提供个人中心

| Option | Description | Selected |
|--------|-------------|----------|
| 支持多角色 | 一个用户多个角色 | ✓ |
| 单一角色 | 一个用户一个角色 | |

**User's choice:** 支持多角色

| Option | Description | Selected |
|--------|-------------|----------|
| 有角色管理页面 | 独立角色管理，树形勾选权限 | ✓ |
| 角色硬编码 | 代码中预置7类角色不可编辑 | |

**User's choice:** 有角色管理页面

| Option | Description | Selected |
|--------|-------------|----------|
| 有启停状态 | 启用/停用控制 | ✓ |
| 无状态控制 | 创建后永久有效 | |

**User's choice:** 有启停状态

| Option | Description | Selected |
|--------|-------------|----------|
| 逻辑删除 | 标记已删除，数据保留 | ✓ |
| 物理删除 | 直接删除数据库记录 | |

**User's choice:** 逻辑删除

---

## JWT & Session Configuration

| Option | Description | Selected |
|--------|-------------|----------|
| 2h access + 7d refresh | Access Token 2h + Refresh Token 7d | ✓ |
| 24h 单 token | 单 token 24h 有效期 | |
| 30min access + 30d refresh | Access Token 30min + Refresh Token 30d | |

**User's choice:** 2h access + 7d refresh

| Option | Description | Selected |
|--------|-------------|----------|
| 允许多设备 | 多设备同时登录 | ✓ |
| 单设备登录 | 仅一处登录，新登录踢旧 | |

**User's choice:** 允许多设备

| Option | Description | Selected |
|--------|-------------|----------|
| 内存+httpOnly Cookie | Access Token 存内存，Refresh Token 存 Cookie | ✓ |
| localStorage | Access Token 存 localStorage | |

**User's choice:** 内存+httpOnly Cookie

| Option | Description | Selected |
|--------|-------------|----------|
| 用户ID+角色+部门+权限 | Token 载荷包含完整权限信息 | ✓ |
| 仅用户ID | Token 仅含用户ID | |

**User's choice:** 用户ID+角色+部门+权限

---

## Audit Log, File Proxy & Backup

| Option | Description | Selected |
|--------|-------------|----------|
| 所有写操作 | 登录/登出/CUD 操作 | ✓ |
| 全部操作包括查询 | 含查看操作 | |
| 仅关键操作 | 仅高风险操作 | |

**User's choice:** 所有写操作

| Option | Description | Selected |
|--------|-------------|----------|
| MySQL 独立表+按月分区 | 独立 audit_log 表+按月分区 | ✓ |
| 文件日志 | 滚动写入日志文件 | |
| 独立数据库 | 独立 MySQL/ES 实例 | |

**User's choice:** MySQL 独立表+按月分区

| Option | Description | Selected |
|--------|-------------|----------|
| 哈希链防篡改 | SHA-256 哈希链条+定期校验 | ✓ |
| 仅追加无哈希链 | INSERT ONLY 模式 | |

**User's choice:** 哈希链防篡改

| Option | Description | Selected |
|--------|-------------|----------|
| 年/月/类型分层 | uploads/YYYY/MM/type/ | ✓ |
| UUID 平铺 | 所有文件同级 UUID 命名 | |

**User's choice:** 年/月/类型分层

| Option | Description | Selected |
|--------|-------------|----------|
| 数据库+文件全量备份 | 每日全量备份 | ✓ |
| 仅数据库备份 | 文件依赖其他冗余 | |
| 数据库全量+文件增量 | 首次全量后仅增量 | |

**User's choice:** 数据库+文件全量备份

| Option | Description | Selected |
|--------|-------------|----------|
| 提供日志管理页面 | 表格展示+多维度筛选 | ✓ |
| 无界面直接查库 | 审计人员直接查数据库 | |

**User's choice:** 提供日志管理页面

| Option | Description | Selected |
|--------|-------------|----------|
| 50MB+常用文档类型 | PDF/Word/Excel/图片/压缩包 | ✓ |
| 20MB+PDF/图片 | 更严格的限制 | |

**User's choice:** 50MB+常用文档类型

---

## API Integration Framework

| Option | Description | Selected |
|--------|-------------|----------|
| 数据库存储+管理界面 | 在线配置，动态生效 | ✓ |
| 配置文件(YAML) | application.yml 管理 | |

**User's choice:** 数据库存储+管理界面

| Option | Description | Selected |
|--------|-------------|----------|
| 重试3次+指数退避 | 1s→2s→4s 退避 | ✓ |
| 重试2次+固定间隔 | 3s 固定间隔 | |

**User's choice:** 重试3次+指数退避

---

## Navigation Menu Structure

| Option | Description | Selected |
|--------|-------------|----------|
| 系统管理下6个子菜单 | 用户/角色/部门/字典/审计/API 配置 | ✓ |
| 平级菜单无分组 | 所有菜单同级展示 | |

**User's choice:** 系统管理下6个子菜单

| Option | Description | Selected |
|--------|-------------|----------|
| 欢迎+概览仪表盘 | 首页显示概览卡片 | ✓ |
| 按角色跳转业务页 | 根据角色直接跳转 | |

**User's choice:** 欢迎+概览仪表盘

---

## Login Page Design

| Option | Description | Selected |
|--------|-------------|----------|
| 展示品牌信息 | Logo+系统名称+研究院信息 | |
| 极简无品牌 | 仅输入框+按钮 | ✓ |

**User's choice:** 极简无品牌

---

## Classified Data Schema

| Option | Description | Selected |
|--------|-------------|----------|
| 同一实例独立 Schema | 同一 MySQL 独立 Schema | ✓ |
| 同表字段标记 | is_classified+classified_level 字段 | |
| 独立数据库实例 | 完全物理隔离 | |

**User's choice:** 同一实例独立 Schema

---

## Claude's Discretion

所有灰色地带均经用户明确决策。

## Deferred Ideas

None — 讨论均在 Phase 0 范围内。
