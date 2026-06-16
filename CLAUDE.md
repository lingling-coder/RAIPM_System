<!-- GSD:project-start source:PROJECT.md -->
## Project

**科研成果与知识产权管理系统**

科研成果与知识产权一体化全生命周期管理系统，覆盖研究院的**论文、专利、软件著作权**三大类成果从登记、审核、缴费、维护、转化、统计到注销的闭环管理。系统依托自动化接口减少人工录入，配置多级预警机制规避逾期风险，提供多维度数据看板支撑管理决策，并与院内现有业务系统（统一身份认证/HR、财务、文献库）对接，打通数据孤岛。

**Core Value:** **确保科研成果与知识产权的全生命周期可追溯、费用不逾期、数据可分析。** —— 解决人工Excel台账管理带来的成果遗漏、缴费逾期、底数不清、数据汇总耗时等核心痛点。

### Constraints

- **技术栈**: Java + Spring Boot（后端）, Vue3（前端）, MySQL + Redis（数据库与缓存）
- **架构**: 前后端分离（RESTful API + SPA）
- **部署**: 院内服务器（内网环境）
- **认证**: 一期自建账号体系，预留LDAP/OAuth统一认证对接能力
- **权限模型**: RBAC + 部门数据隔离 + 涉密数据独立权限
- **浏览器兼容**: Chrome、Edge、360浏览器
- **操作系统兼容**: Windows、macOS
- **RTO**: ≤4小时，**RPO**: ≤30分钟
- **接口规范**: 统一配置中心管理密钥，支持重试（最多3次）、降级方案、调用监控
<!-- GSD:project-end -->

<!-- GSD:stack-start source:research/STACK.md -->
## Technology Stack

## 核心技术栈
| 层面 | 推荐方案 | 版本 | 原因 |
|------|---------|------|------|
| **后端框架** | Spring Boot | 4.1.x | Jakarta EE 11，支持虚拟线程，长期支持 |
| **JDK** | JDK 21 LTS | 21 | 支持虚拟线程，截止2031年，适合I/O密集型操作 |
| **ORM** | MyBatis-Plus | 3.5.x | 中国企业项目标准，复杂查询灵活，手写SQL可控 |
| **API文档** | SpringDoc OpenAPI | 3.0.x | Spring Boot 4兼容，社区活跃 |
| **安全框架** | Spring Security + JWT | — | 与Spring Boot深度集成，OAuth2/LDAP扩展性好 |
| **前端框架** | Vue 3 + Element Plus | Vue 3.4+ / Element Plus 2.9+ | Element Plus是中国企业级中后台最成熟UI库 |
| **状态管理** | Pinia | 3.x | 比Vuex更轻量，TypeScript支持更好 |
| **图表** | ECharts 5.5 + vue-echarts 7 | 5.5.x | 国产、功能强大、文档齐全 |
| **构建工具** | Vite | 5.x | 远超Webpack的开发体验和构建速度 |
| **数据库** | MySQL 8.0+ | 8.4 LTS | 成熟稳定，与Spring Boot生态兼容 |
| **缓存** | Redis 7.x | 7.x | 会话管理、缓存加速、分布式锁 |
| **搜索** | MySQL ngram（Phase 1）→ Elasticsearch 8.x（Phase 2） | — | <5万条：MySQL全文索引；≥5万：ES + IK分词器 |
| **Excel处理** | EasyExcel | 4.0+ | 流式写入，内存仅为Apache POI的1/10 |
| **DTO映射** | MapStruct | 1.6.x | 编译时代码生成，性能远优于ModelMapper |
| **任务调度** | Spring @Scheduled（Phase 1）/ XXL-JOB（Phase 2） | — | 单实例用@Scheduled+Redis锁；多实例用XXL-JOB |
| **文件存储** | 本地文件系统（Phase 1）/ MinIO（Phase 2） | — | MinIO注意AGPL授权变更，准备SeaweedFS备选 |
| **限流/重试** | Resilience4j | Spring管理 | 需正确设置切面顺序：CircuitBreaker在外，Retry在内 |
| **前端模板** | Vue Pure Admin | 最新 | 使用Element Plus的中后台模板 |
## 版本选择理由
### Spring Boot 4.1.x（非3.x）
- Spring Boot 3.5.x OSS支持即将结束
- Spring Boot 4使用Jakarta EE 11和Jackson 3
- 支持虚拟线程（Virtual Threads），对多并发API调用场景有利
### MyBatis-Plus（非JPA/Hibernate）
- 国内科研管理系统多使用MyBatis-Plus
- 复杂多表关联查询（成果↔费用↔转化）需手写SQL优化
- JPA的自动SQL在复杂权限过滤场景下性能不可控
### Element Plus（非Ant Design Vue）
- Element Plus在企业中后台领域最成熟稳定
- Ant Design Vue开源维护已停滞
- 与Vue Pure Admin模板匹配
## 不建议使用的库
| 库 | 替代方案 | 原因 |
|---|---------|------|
| Apache Shiro | Spring Security | Shiro维护减弱，与Spring Boot集成不如Security深入 |
| JPA/Hibernate | MyBatis-Plus | 复杂查询性能不可控，学习曲线陡峭 |
| Apache POI | EasyExcel | POI内存消耗大，大数据量导出OOM风险 |
| ModelMapper | MapStruct | ModelMapper慢10-500倍，运行时反射 |
| Quartz | XXL-JOB | Quartz缺乏分布式管理界面 |
| SpringFox | SpringDoc OpenAPI | SpringFox已停止维护 |
| Webpack | Vite | Webpack构建慢，配置复杂 |
| Moment.js | dayjs | Moment.js包体积大，已进入维护模式 |
## 阶段化演进策略
### Phase 1（单服务器架构）
- 单一Spring Boot JAR包
- MySQL + Redis
- Nginx反向代理
- 本地文件存储
- MySQL ngram全文索引（满足<5万条记录）
- Spring @Scheduled + Redis分布式锁
- 无需Docker编排、ES、MinIO
### Phase 2（多服务器/全功能架构）
- Elasticsearch 8.x + IK分词器 + 领域词典
- MinIO集群或SeaweedFS
- XXL-JOB分布式调度中心
- Redis Sentinel集群
- PostgreSQL逻辑复制或Canal binlog同步
- Prometheus + Grafana监控大盘
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

Conventions not yet established. Will populate as patterns emerge during development.
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

Architecture not yet mapped. Follow existing patterns found in the codebase.
<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->
## Project Skills

No project skills found. Add skills to any of: `.claude/skills/`, `.agents/skills/`, `.cursor/skills/`, `.github/skills/`, or `.codex/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd-debug` for investigation and bug fixing
- `/gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->



<!-- GSD:profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
