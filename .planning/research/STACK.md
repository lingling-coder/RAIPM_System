# 技术栈推荐 — 科研成果与知识产权管理系统

**Generated:** 2026-06-16
**Confidence:** HIGH

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
