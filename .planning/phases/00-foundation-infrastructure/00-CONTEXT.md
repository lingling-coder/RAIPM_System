# Phase 0: Foundation & Infrastructure - Context

**Gathered:** 2026-06-16
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 0 delivers the system management infrastructure that every other phase depends on. This includes:
- Maven multi-module project structure initialization
- User/role/department CRUD with RBAC permission model
- JWT authentication with Spring Security
- SQL-layer data permission injection (MyBatis-Plus interceptor)
- Append-only audit log with hash chain and monthly partitioning
- File proxy service with UUID naming and layered directory storage
- API integration framework (configuration center, Resilience4j retry)
- Daily automated backup (database + files)
- Browser compatibility baseline (Chrome, Edge, 360)
- Login page, personal profile center, and system management UI

</domain>

<decisions>
## Implementation Decisions

### 项目模块架构
- **D-01:** 采用 4+1 模块结构：
  - `achievement-web` — Vue3 前端（基于 Vue Pure Admin）
  - `achievement-common` — 公共工具类、枚举、常量、异常
  - `achievement-framework` — 框架核心（安全/权限/审计/文件/API 集成）
  - `achievement-module/*` — 业务模块（按成果类型拆分子模块，Phase 1 起添加）
- **D-02:** 命名规范使用 `achievement-xxx` 风格，顶层包名 `com.institute.achievement.*`
- **D-03:** 开发环境使用 Maven Wrapper + Docker Compose（MySQL 8.4 + Redis 7.x）
- **D-04:** 前端和后端使用统一仓库 Monorepo 管理
- **D-05:** 前端基于 Vue Pure Admin 模板搭建

### 用户管理与 RBAC
- **D-06:** 用户管理页面使用表格 + 抽屉编辑风格
- **D-07:** RBAC 权限控制到菜单/路由级别
- **D-08:** 部门采用平级结构（无层级关系），数据隔离通过 dept_id 过滤
- **D-09:** CSV 人员导入采用覆盖更新策略（匹配工号则更新，不匹配则新增）
- **D-10:** 数据字典管理采用左树右表分组管理界面
- **D-11:** 角色分配在用户编辑界面通过多选方式完成
- **D-12:** 标准密码策略：最少8位，包含字母+数字
- **D-13:** 系统预置默认管理员账号（admin），首次登录强制修改密码
- **D-14:** 密码重置由系统管理员手动操作
- **D-15:** 连续5次登录失败后账户锁定30分钟
- **D-16:** 提供个人中心页面，用户可修改姓名、邮箱、手机号及修改密码（需验证旧密码）
- **D-17:** 支持一个用户拥有多个角色，导航栏展示所有角色的权限并集
- **D-18:** 独立角色管理页面，树形勾选菜单权限分配，支持新增/编辑/删除角色
- **D-19:** 用户有启用/停用状态控制，停用后无法登录但保留历史数据
- **D-20:** 用户删除采用逻辑删除（标记已删除状态，数据保留）

### JWT 认证与会话
- **D-21:** Access Token 有效期2小时 + Refresh Token 有效期7天，Refresh Token 存储在 httpOnly Cookie 中自动刷新
- **D-22:** 同一账号允许多设备同时登录，每个设备独立会话
- **D-23:** Access Token 存储在内存（Pinia 状态），Refresh Token 存储在 httpOnly Cookie
- **D-24:** JWT 载荷包含：用户ID、角色列表、部门ID、权限标识

### 审计日志
- **D-25:** 审计范围覆盖所有写操作（登录/登出、创建、编辑、删除），不记录查询
- **D-26:** 审计日志存储在 MySQL 独立表（audit_log）+ 按月分区（PARTITION BY RANGE）
- **D-27:** 实现哈希链防篡改（每条日志存储上一条的 SHA-256 哈希值，定期校验链条完整性）
- **D-28:** 提供审计日志管理页面，支持按操作人、时间范围、操作类型、操作对象筛选

### 文件代理服务
- **D-29:** 文件存储目录结构：uploads/YYYY/MM/type/（按年/月/成果类型分层）
- **D-30:** 文件通过代理 URL 访问（UUID 文件名），不暴露直接存储路径
- **D-31:** 单文件上限50MB，支持 PDF/Word/Excel/图片/压缩包

### 备份策略
- **D-32:** 每日凌晨全量备份（数据库 + 文件存储目录）
- **D-33:** 备份文件留存30天

### API 集成框架
- **D-34:** 外部 API 配置存储在数据库，提供系统管理界面在线配置（动态生效，无需重启）
- **D-35:** 默认重试策略：重试3次 + 指数退避（1s → 2s → 4s），连接超时5s，读取超时10s

### 导航与页面
- **D-36:** 系统管理菜单包含6个子菜单：用户管理 / 角色管理 / 部门管理 / 数据字典 / 审计日志 / API 集成配置
- **D-37:** 系统首页为欢迎页 + 概览仪表盘（各角色看到不同内容）
- **D-38:** 登录页面采用极简风格（仅输入框 + 登录按钮，无品牌信息展示）

### 涉密数据
- **D-39:** 涉密成果数据使用同一 MySQL 实例的独立 Schema 隔离

### Claude's Discretion
本次讨论已覆盖所有灰色地带，无未涉及领域。

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 项目级文档
- `.planning/REQUIREMENTS.md` — 完整需求定义（SYS-01~08, API-03~04, OPS-01~03 for Phase 0）
- `.planning/ROADMAP.md` — Phase 0 success criteria 和计划结构
- `.planning/PROJECT.md` — 项目概览、用户角色（7类）、架构约束、关键决策
- `.planning/STATE.md` — 项目当前状态

### 技术栈规范
- `CLAUDE.md` — 技术栈版本选择、架构约束、编码规范

</canonical_refs>

<code_context>
## Existing Code Insights

### 项目状态
项目为全新搭建（Greenfield），尚无代码存在。Phase 0 为第一个交付阶段。

### 关键依赖关系
- Phase 1（成果登记与审批）依赖于 Phase 0 提供的：用户/角色/权限系统、JWT 认证、SQL 层权限注入、审计日志框架、文件代理服务、API 集成框架

</code_context>

<specifics>
## Specific Ideas

采用标准中国企业管理系统的设计模式：效率优先（表格列表、批量操作）、安全可控（审计日志、权限树）、清晰的状态可见性（标签、徽标）。

</specifics>

<deferred>
## Deferred Ideas

None — 讨论均在 Phase 0 范围内。

</deferred>

---

*Phase: 00-Foundation & Infrastructure*
*Context gathered: 2026-06-16*
