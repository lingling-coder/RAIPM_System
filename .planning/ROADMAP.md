# Roadmap: 科研成果与知识产权管理系统

## Overview

从替代Excel人工台账到全生命周期数字化管理，分为5个阶段交付。Phase 0建立系统管理基础设施（用户/角色/部门/权限/审计/文件/API框架），Phase 1交付核心成果登记与审批闭环，Phase 2交付费用管理与逾期预警，Phase 3交付统计看板与全文检索，Phase 4交付申报提醒与系统集成。每个阶段交付端到端用户能力，一期解决人工台账、缴费逾期、基础统计三大痛点。

## Phases

- [x] **Phase 0: Foundation & Infrastructure** (2026-06-16) - 系统管理基础设施：用户/角色/部门管理、JWT认证、RBAC权限、数据隔离、审计日志、文件代理、API集成框架
- [x] **Phase 1: Achievement Registration & Approval** (2026-06-16) - 论文/专利/软著三大成果的全流程登记、DOI自动补全、审批流转、附件管理、批量导入、成果作废与重复检测
- [x] **Phase 2: Fee Management & Alerts** - 费用台账与缴费计划、四级预警机制、二次催办、批量缴费单、费用统计 (completed 2026-06-16)
- [ ] **Phase 3: Dashboard & Search** - 标准可视化看板（4种图表）、Excel/PDF导出、全文检索（中文分词+权限过滤）
- [ ] **Phase 4: Reminders & System Integration** - 申报提醒全流程（6种类型）、模板管理、站内消息+邮件触达、回执确认与升级

## Phase Details

### Phase 0: Foundation & Infrastructure
**Mode**: mvp
**Goal**: 系统管理基础设施完整可用，支持用户/角色/部门管理、JWT认证、RBAC权限与数据隔离、审计日志、文件代理服务、API集成框架
**Depends on**: Nothing (first phase)
**Requirements**: SYS-01, SYS-02, SYS-03, SYS-04, SYS-05, SYS-06, SYS-07, SYS-08, API-03, API-04, OPS-01, OPS-02, OPS-03
**Success Criteria** (what must be TRUE):
  1. 系统管理员可以通过管理界面创建/管理用户、角色、部门和数据字典条目；7类角色的菜单权限按RBAC规则正确分配
  2. 用户可以使用用户名+密码登录系统，获取JWT令牌，在会话期间保持登录状态，并看到角色专属的导航菜单
  3. A部门的用户查询共享资源时只能看到A部门的数据，看不到B部门的数据（SQL层注入过滤验证通过）
  4. 所有用户操作（登录、创建、更新、删除）被记录在仅追加的审计日志中，包含操作人、时间、IP、变更前后内容；日志按月分区存储，不可删除或篡改
  5. 上传文件通过代理URL访问（UUID文件名），直接文件路径访问被阻止；备份任务每日自动执行，备份文件留存30天
**Plans**: 5 plans

Plans:
- [x] 00-01: Initialize Maven multi-module project structure with Spring Boot 4.1 + MyBatis-Plus + MySQL 8.4 + Redis 7.x + Vue 3 + Element Plus
- [x] 00-02: Implement user/role/department CRUD with RBAC permission model, data dictionary management, and personnel CSV import
- [x] 00-03: Implement JWT authentication with Spring Security, SQL-layer data permission interceptor (MyBatis-Plus), and classified data schema isolation
- [x] 00-04: Implement append-only audit log system with monthly partitioning (hash chain), file proxy service with UUID naming, and daily backup scheduler
- [x] 00-05: Implement API integration framework (configuration center for keys/timeouts, Resilience4j retry with exponential backoff, fallback handler) and browser compatibility testing

**UI hint**: yes

### Phase 1: Achievement Registration & Approval
**Mode**: mvp
**Goal**: 科研人员可以登记论文、专利、软著三大成果，使用DOI自动补全、批量导入、附件上传，并提交三步审批流程；审批人接收站内消息通知完成审核
**Depends on**: Phase 0
**Requirements**: REG-01, REG-02, REG-03, REG-04, REG-05, REG-06, REG-07, REG-08, REG-09, REG-10, APPR-01, APPR-02, APPR-03, API-01
**Success Criteria** (what must be TRUE):
  1. 科研人员可以登记论文（标题、作者、期刊、DOI、ISSN/CN、卷期页码、发表年份、收录情况、影响因子、分区、摘要），触发DOI自动补全后系统从Crossref/Scopus/OpenAlex预填字段并支持多源切换优先级
  2. 科研人员可以登记专利（专利名称、发明人、申请号、授权号、申请日、授权日、类型、国别、年费下次缴费日、法律状态）和软著（软著名称、著作权人、登记号、登记日期、版本号、软件类别）
  3. 提交的成果走通三步审批流（科研人员提交 -> 部门科研秘书初审 -> 系统管理员归档）；每一步审批操作都有完整日志记录，审批待办通过站内消息实时通知
  4. 相同DOI（论文）/申请号（专利）重复提交被拦截并提示错误；科研人员可以注销/作废成果并填写原因，作废记录留存审计
  5. 科研人员可以通过Excel/RIS模板批量导入成果，登记时关联所属课题/项目；涉密成果标记后仅授权人员可查看和下载附件
**Plans**: 5 plans

Plans:
- [x] 01-01: Paper registration with full metadata fields and DOI auto-complete (Crossref/OpenAlex multi-source with priority switching and auto-fallback)
- [x] 01-02: Patent and software copyright registration with unique constraint validation (application_no/registration_no) and duplicate detection
- [x] 01-03: 3-step approval workflow (submit -> department secretary review -> admin archive) with audit logging, notification polling, and left-right split approval UI
- [x] 01-04: Excel batch import with unified template, partial import with error report, attachment upload with download permission checks, and classified permission service
- [x] 01-05: Achievement invalidation with reason logging, classified marking with access control, submit-time duplicate detection with DuplicateDialog

**UI hint**: yes

### Phase 2: Fee Management & Alerts
**Mode**: mvp
**Goal**: 专利/软著费用台账完整可用，四级预警机制自动运行防止缴费逾期，支持缴费计划管理、批量缴费单生成、多维度费用统计
**Depends on**: Phase 1
**Requirements**: FEE-01, FEE-02, FEE-03, FEE-04, FEE-05, FEE-06
**Success Criteria** (what must be TRUE):
  1. 用户可以在费用台账中查看关联专利/软著的费用记录（费用类型、金额、缴费日期、截止日期、缴费状态），按部门/年份/专利类型/经费来源筛选汇总
  2. 系统按规则自动生成单次和周期性年费缴费计划；用户可以手动编辑金额和日期、暂停计划
  3. 费用预警引擎在截止日前30天/15天/7天和逾期日自动发送对应级别的预警通知，预警级别与紧急程度匹配
  4. 首次预警在配置期限内未处理时，系统自动追加提醒并通知部门负责人（二次催办）
  5. 管理员可以批量选择费用记录生成缴费单、批量标记缴费完成；费用统计按部门/年份/专利类型/经费来源多维展示
**Plans**: 5 plans

Plans:
- [x] 02-01: Implement fee ledger with CRUD for patent/software fees and multi-dimensional statistical queries
- [x] 02-02: Implement payment plan engine with single/recurring annual generation, manual edit, and pause functionality
- [x] 02-03: Implement versioned fee rule engine and 4-tier alert calculation with notification trigger
- [x] 02-04: Implement secondary escalation logic for unresolved first alerts and batch fee slip generation
- [x] 02-05: Implement fee statistics dashboard (by department, year, patent type, funding source) with data export

**UI hint**: yes

### Phase 3: Dashboard & Search
**Mode**: mvp
**Goal**: 院领导和主管可以通过可视化看板查看年度趋势、类型分布、部门排行和专利状态；全体用户可以通过全文检索快速定位成果，结果按权限自动过滤
**Depends on**: Phase 1
**Requirements**: STAT-01, STAT-02, SRCH-01, SRCH-02, SRCH-03, SRCH-04, OPS-04
**Success Criteria** (what must be TRUE):
  1. 看板展示4种标准图表：年度成果数量趋势（折线图）、成果类型分布（饼图）、部门成果排行（柱状图）、专利有效/失效占比；数据随成果登记实时更新
  2. 所有图表支持一键导出Excel和PDF格式
  3. 用户可以通过成果标题、摘要、作者/发明人、关键词进行检索；结果展示中文分词、模糊匹配、关键词高亮和按相关性排序
  4. 检索结果可按成果类型（论文/专利/软著）、所属部门、年份范围、密级筛选
  5. 检索结果自动过滤用户无权访问的成果（基于部门和涉密数据规则）；系统在50用户并发场景下响应正常
**Plans**: 5 plans

Plans:
- [ ] 03-01: Implement statistics dashboard backend (aggregation queries for 4 chart types) with Pinia state management
- [ ] 03-02: Implement 4 chart types (line/pie/bar/status) using ECharts with Excel and PDF export
- [ ] 03-03: Implement full-text search with MySQL ngram parser, Chinese word segmentation, fuzzy matching, and keyword highlighting
- [ ] 03-04: Implement search result filters (achievement type, department, year, classification level) and permission-filtered result delivery
- [ ] 03-05: Execute concurrency load testing for 50-user scenario and optimize performance bottlenecks

**UI hint**: yes

### Phase 4: Reminders & System Integration
**Mode**: mvp
**Goal**: 申报提醒系统覆盖6种提醒类型，支持模板配置、批量创建、站内消息+邮件触达、回执确认与二次升级；邮件集成完整可用
**Depends on**: Phase 0, Phase 2
**Requirements**: RMD-01, RMD-02, RMD-03, RMD-04, RMD-05, RMD-06, API-02
**Success Criteria** (what must be TRUE):
  1. 管理员可以配置6种提醒类型（项目申报、奖项申报、专利年费、软著维护、转化后评估、涉密核查），设置名称、截止日期、提前天数、责任人、紧急等级
  2. 管理员可以预设提醒模板（支持批量创建提醒任务），模板内容包括提醒标题、正文模板、默认紧急等级
  3. 用户通过站内消息和邮件（企业邮箱SMTP）接收提醒；未读提醒在导航栏显示计数徽标
  4. 用户可以点击确认回执标记已处理；超时未确认的提醒自动触发二次催办并通知部门负责人
  5. 紧急等级为"高"的提醒触发系统全局弹窗强提醒，用户需要手动关闭
**Plans**: 5 plans

Plans:
- [ ] 04-01: Implement reminder type configuration and template management with batch creation
- [ ] 04-02: Implement reminder task generation engine with deadline calculation, routing rules, and conflict detection
- [ ] 04-03: Implement in-app notification system with unread badge, read receipt, and confirmation workflow
- [ ] 04-04: Implement email integration (SMTP) with template rendering, configuration management, and fallback handling
- [ ] 04-05: Implement escalation rules engine, secondary notification logic, and high-urgency modal popup

**UI hint**: yes

## Progress

**Execution Order:**
Phases execute in numeric order: 0 -> 1 -> 2 -> 3 -> 4

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 0. Foundation & Infrastructure | 5/5 | Complete | 2026-06-16 |
| 1. Achievement Registration & Approval | 5/5 | Complete | 2026-06-16 |
| 2. Fee Management & Alerts | 6/5 | Complete   | 2026-06-16 |
| 3. Dashboard & Search | 0/5 | Not started | - |
| 4. Reminders & System Integration | 0/5 | Not started | - |
