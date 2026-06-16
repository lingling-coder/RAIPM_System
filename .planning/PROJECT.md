# 科研成果与知识产权管理系统

## What This Is

科研成果与知识产权一体化全生命周期管理系统，覆盖研究院的**论文、专利、软件著作权**三大类成果从登记、审核、缴费、维护、转化、统计到注销的闭环管理。系统依托自动化接口减少人工录入，配置多级预警机制规避逾期风险，提供多维度数据看板支撑管理决策，并与院内现有业务系统（统一身份认证/HR、财务、文献库）对接，打通数据孤岛。

## Core Value

**确保科研成果与知识产权的全生命周期可追溯、费用不逾期、数据可分析。** —— 解决人工Excel台账管理带来的成果遗漏、缴费逾期、底数不清、数据汇总耗时等核心痛点。

## Requirements

### Validated

- [x] **论文、专利、软著三类成果的统一登记与审核** — Phase 1 (2026-06-16)
- [x] **可配置审批流程（按成果类型/密级差异化）** — Phase 1 (2026-06-16)
- [x] **成果注销/作废与历史归档** — Phase 1 (2026-06-16)
- [x] **全量审计日志与操作留痕** — Phase 0 (2026-06-16)
- [x] **费用台账与缴费计划（关联专利/软著）** — Phase 2 (2026-06-16)
- [x] **四级预警机制（30天/15天/7天/逾期）与二次催办** — Phase 2 (2026-06-16)
- [x] **多维度费用统计** — Phase 2 (2026-06-16)

### Active

- [ ] 三种转化模式（技术转让/许可使用/作价入股）的全生命周期跟踪
- [ ] 转化效益跟踪与后续提醒
- [ ] 线上缴费审批流程
- [ ] 标准可视化看板（年度趋势、类型分布、部门排行、专利状态、转化漏斗等）
- [ ] 引文分析（个人/部门/全院三级）
- [ ] 自定义报表与定时推送
- [ ] 申报提醒（项目申报/奖项申报/年费/维护/后评估/涉密核查）
- [ ] 多级提醒规则配置与触达（站内消息+邮件+短信预留）
- [ ] 全文检索（中文分词、模糊匹配、高级组合检索）
- [ ] 检索结果权限过滤与安全管控
- [ ] DOI自动补全（Crossref/Scopus/OpenAlex多源切换）
- [ ] 专利法律状态同步（CNIPA接口）
- [ ] 人员/部门数据同步（HR系统/LDAP）
- [ ] 邮件通知推送
- [ ] RBAC权限模型 + 部门数据隔离 + 涉密数据独立权限
- [ ] 数据每日自动备份（留存30天）
- [ ] 附件的版本管理、在线预览、加密存储、下载权限校验

### Out of Scope

- **短信通知接口** — 二期可选，一期预留接口但不实现
- **财务系统深度对接** — 二期可选，一期由用户手工录入凭证
- **移动端原生应用** — 二期规划，一期仅适配PC Web端
- **大数据量性能优化与分表方案** — 当历史数据达到十万级以上时二期处理

## Context

### 项目背景

研究院目前依赖人工Excel台账管理论文、专利、软著等科研成果，存在以下痛点：
- 成果信息易遗漏、汇总耗时
- 专利/软著缴费逾期风险高
- 全院成果底数不清
- 跨系统（人员、财务、文献库）数据重复录入，形成数据孤岛

### 用户角色体系（7类）

1. **科研人员** — 登记/维护个人成果，查看统计，接收提醒
2. **科研秘书（部门级）** — 审核本部门成果，管理部门费用与转化
3. **部门管理员** — 协助部门秘书管理权限与基础数据
4. **主管/院领导** — 查看全院统计看板与决策分析
5. **涉密成果管理员** — 专项管理涉密成果全流程
6. **内审/审计人员** — 查阅日志、台账、审批记录（只读）
7. **系统管理员** — 全局配置、运维、权限管理

### 一期与二期划分

**一期（基础刚需版）：** 核心成果登记、审批、费用台账、基础预警、通用检索、基础统计看板、基础权限与核心API对接。解决人工台账、缴费逾期、基础统计三大痛点。

**二期（全流程深化版）：** 完善成果转化全链路、费用线上审批、自定义报表、高级检索、移动端适配、第三方系统深度对接、大数据量性能优化与运维监控。

## Constraints

- **技术栈**: Java + Spring Boot（后端）, Vue3（前端）, MySQL + Redis（数据库与缓存）
- **架构**: 前后端分离（RESTful API + SPA）
- **部署**: 院内服务器（内网环境）
- **认证**: 一期自建账号体系，预留LDAP/OAuth统一认证对接能力
- **权限模型**: RBAC + 部门数据隔离 + 涉密数据独立权限
- **浏览器兼容**: Chrome、Edge、360浏览器
- **操作系统兼容**: Windows、macOS
- **RTO**: ≤4小时，**RPO**: ≤30分钟
- **接口规范**: 统一配置中心管理密钥，支持重试（最多3次）、降级方案、调用监控

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Java Spring Boot + Vue3 | 国内企业级主流方案，生态成熟，研究院技术团队熟悉度较高 | — Pending |
| 前后端分离架构 | 便于API复用、前后端并行开发、二期移动端对接 | — Pending |
| MySQL + Redis | MySQL满足结构化数据存储，Redis加速缓存与会话管理 | — Pending |
| 一期自建账号体系 | 快速推进一期开发，预留LDAP/OAuth扩展字段 | — Pending |
| PC Web端优先 | 一期聚焦核心业务功能，移动端二期支持 | — Pending |
| 院内服务器部署 | 内网安全可控，满足涉密数据保密要求 | — Pending |
| 一期范围严格按文档 | 聚焦基础刚需版，解决三大核心痛点 | — Pending |
| 费用记录使用Arc多态(owner_type+owner_id)关联专利/软著 | 避免多态表继承的复杂性，MyBatis手动JOIN更可控 | Phase 2 |
| 四级预警阈值硬编码(30/15/7/0天) | Phase 1无需DB配置，简化部署；Phase 2可迁移到配置表 | Phase 2 |
| 费用编号使用Redis INCR(FEE-YYYYMMDD-XXX) | 分布式唯一编号，2天TTL自动过期，避免DB自增锁 | Phase 2 |
| MyBatis `<choose>`实现维度安全GROUP BY | 防止统计查询中的SQL注入，比动态拼接更安全 | Phase 2 |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd:complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-06-16 after Phase 2 completion*
