# 架构设计 — 科研成果与知识产权管理系统

**Generated:** 2026-06-16
**Confidence:** HIGH

## 一、整体架构模式

### Phase 1：单体应用架构

```
┌─────────────────────────────────────────────────────┐
│                   Nginx (反向代理)                     │
├─────────────────────────────────────────────────────┤
│              Vue3 SPA (Element Plus)                   │
├─────────────────────────────────────────────────────┤
│              Spring Boot 4.1 (单体JAR)                  │
│  ┌──────────┬──────────┬──────────┬─────────────┐    │
│  │成果登记模块│ 转化跟踪模块 │ 费用管理模块 │  统计分析模块  │    │
│  ├──────────┼──────────┼──────────┼─────────────┤    │
│  │申报提醒模块│ 全文检索模块 │ 系统管理模块 │  外部API集成  │    │
│  └──────────┴──────────┴──────────┴─────────────┘    │
├──────────────────┬──────────────────────────────────┤
│     MySQL 8.4    │          Redis 7.x                  │
└──────────────────┴──────────────────────────────────┘
```

### Phase 2：分布式架构

```
Nginx → Vue3 SPA → Spring Cloud Gateway
                          ↓
           ┌─────┬─────┬─────┬─────┬─────┐
           │登记 │转化  │费用  │统计  │系统  │  ← Nacos注册中心
           └──┬──┴──┬──┴──┬──┴──┬──┴──┬──┘
              │     │     │     │     │
        ┌─────┴─────┴─────┴─────┴─────┴─────┐
        │           MySQL 集群                │
        ├────────────────────────────────────┤
        │    Elasticsearch 8.x + IK分词器      │
        ├────────────────────────────────────┤
        │   MinIO/SeaweedFS (文件存储)         │
        ├────────────────────────────────────┤
        │   XXL-JOB (分布式调度)              │
        └────────────────────────────────────┘
```

## 二、后端模块划分

### Maven多模块结构

```
achievement-ip-management/
├── achievement-api/           # 对外API接口层
├── achievement-common/        # 通用工具类、常量、异常
├── achievement-framework/     # 框架核心：安全、权限、日志、缓存
├── achievement-module/
│   ├── achievement-paper/     # 论文模块
│   ├── achievement-patent/    # 专利模块
│   ├── achievement-copyright/ # 软著模块
│   ├── achievement-fee/       # 费用管理模块
│   ├── achievement-transform/ # 转化跟踪模块
│   ├── achievement-report/    # 统计分析模块
│   ├── achievement-remind/    # 申报提醒模块
│   ├── achievement-search/    # 全文检索模块
│   └── achievement-system/    # 系统管理（用户/角色/部门/字典/日志）
├── achievement-integration/   # 外部API集成（Crossref/CNIPA/邮件）
├── achievement-job/           # 定时任务
├── achievement-starter/       # 启动入口
└── pom.xml
```

### 核心模块职责

| 模块 | 职责 | 关键类 |
|------|------|--------|
| paper | 论文CRUD、DOI自动补全、检索、引用统计 | PaperController, PaperService, PaperMapper, PaperDTO |
| patent | 专利CRUD、法律状态同步、费用联动 | PatentController, PatentService, PatentMapper |
| copyright | 软著CRUD、登记信息管理 | CopyrightController, CopyrightService |
| fee | 费用台账、缴费计划、预警计算、审批流 | FeeService, FeePlanService, FeeAlertService |
| transform | 转化登记、节点跟踪、效益台账 | TransformService, TransformTracker |
| report | 统计看板、自定义报表、引文分析 | ReportService, CitationAnalysisService |
| remind | 提醒规则、任务生成、触达路由 | RemindService, NotificationRouter |
| search | 全文检索、高级组合检索、权限过滤 | SearchService, SearchIndexBuilder |
| integration | 外部API调用、重试、降级、映射 | ApiGatewayService, RetryHandler, DataMapper |
| system | 用户、角色、部门、字典、审计日志 | UserService, RoleService, AuditLogService |

## 三、数据架构

### 多态关系设计（关键决策）

**不采用** `relation_type` + `relation_id` 通用多态模式（会导致N+1查询性能问题）。

**采用 Exclusive Arc 模式：** 费用表、转化表、附件表使用多列外键，其中仅一列非空：

```sql
-- 推荐方案：Exclusive Arc
CREATE TABLE fee_record (
    id BIGINT PRIMARY KEY,
    paper_id BIGINT NULL,      -- 关联论文
    patent_id BIGINT NULL,     -- 关联专利
    copyright_id BIGINT NULL,  -- 关联软著
    fee_type VARCHAR(50),
    amount DECIMAL(12,2),
    due_date DATE,
    -- CONSTRAINT check_only_one_nonnull: 三列中仅一列非空
    created_time DATETIME,
    INDEX idx_paper (paper_id),
    INDEX idx_patent (patent_id),
    INDEX idx_copyright (copyright_id)
);
```

### 数据库分表策略

- **审计日志表**: 按月分区（从Phase 1开始），异步写入
- **附件表**: 独立拆分，支持版本管理（version字段递增）
- **数据字典表**: 统一管理，支持业务人员自主维护

### 索引设计要点

- 论文：`DOI`唯一索引，`dept_id`+`publish_year`复合索引
- 专利：`application_no`唯一索引，`legal_status`+`next_fee_date`复合索引
- 软著：`registration_no`唯一索引
- 全表：`dept_id`+`is_deleted`复合索引（数据隔离+软删除）

## 四、安全架构

### 三维权限模型

```
RBAC角色权限 → 部门数据隔离 → 涉密数据隔离
     ↓              ↓               ↓
角色决定功能菜单   SQL注入部门过滤   独立Schema/实例+加密
```

### 权限拦截执行链

```
Request → JWT Filter → 角色鉴权 → 部门数据过滤 → 密级检查 → Controller
```

1. **JWT Filter**: 验证Token，提取用户ID、角色、部门
2. **角色鉴权**: 使用Spring Security @PreAuthorize注解
3. **部门数据过滤**: MyBatis-Plus拦截器自动注入dept_id条件
4. **密级检查**: 对涉密数据单独判断权限

### 数据加密

- 密码：BCrypt加密
- 敏感配置：AES-256（密钥存储在配置中心）
- 涉密附件：AES-256加密存储 + 访问审计
- 传输：全部HTTPS

## 五、集成架构

### 外部API设计模式

```
Controller → Service → ApiGatewayService → RetryHandler → RateLimiter → HttpClient
                                                ↓
                                          FallbackHandler → 返回降级结果
                                                ↓
                                          FailureLogger → Admin Alert
```

### 接口治理规范

- **配置化**: 所有接口地址、密钥、超时时间通过配置中心管理
- **重试**: Resilience4j Retry（最多3次，指数退避）
- **限流**: Resilience4j RateLimiter（避免CNIPA超限封禁）
- **降级**: 手工录入降级方案，接口异常时业务不中断
- **监控**: 接口调用量、成功率、平均耗时实时监控大盘

### 异步任务

- **批量操作**（批量缴费单生成、报表生成）：Spring Async + 线程池
- **数据同步**（专利状态、引文数据）：Spring @Scheduled + Redis锁
- **消息通知**（邮件、站内消息）：异步队列

## 六、前端架构

### 组件层次

```
App.vue
├── Layout (Vue Pure Admin)
│   ├── Sidebar (角色动态菜单)
│   ├── Header (通知、用户信息)
│   └── Content
│       ├── Achievement (通用成果组件)
│       │   ├── PaperAchievement (论文专用)
│       │   ├── PatentAchievement (专利专用)
│       │   └── CopyrightAchievement (软著专用)
│       ├── FeeManagement (费用管理)
│       ├── TransformTracking (转化跟踪)
│       ├── Dashboard (统计看板)
│       ├── Search (全文检索)
│       └── System (系统管理)
```

### 路由设计

- `/dashboard` — 统计看板（院领导/主管可见全院版）
- `/achievement/papers` — 论文管理
- `/achievement/patents` — 专利管理
- `/achievement/copyrights` — 软著管理
- `/fee` — 费用管理
- `/transform` — 转化跟踪
- `/remind` — 申报提醒
- `/search` — 全文检索
- `/system/*` — 系统管理（仅系统管理员）

### 状态管理（Pinia）

```
stores/
├── user.ts           # 用户/角色/权限
├── achievement.ts    # 成果通用状态
├── paper.ts          # 论文专属状态
├── patent.ts         # 专利专属状态
├── fee.ts            # 费用状态
├── transform.ts      # 转化状态
├── dashboard.ts      # 看板数据
└── app.ts            # 全局UI状态
```

## 七、构建顺序建议

| 顺序 | 模块 | 依赖 | 阶段 |
|------|------|------|------|
| 1 | 系统管理（用户/角色/部门/权限） | 无 | Phase 1 |
| 2 | 论文登记 + 审批流 | 系统管理 | Phase 1 |
| 3 | 专利登记 + 审批流 | 系统管理 | Phase 1 |
| 4 | 软著登记 + 审批流 | 系统管理 | Phase 1 |
| 5 | 费用台账 + 预警 | 专利 | Phase 1 |
| 6 | 基础统计看板 | 成果模块 | Phase 1 |
| 7 | 通用全文检索 | 成果模块 | Phase 1 |
| 8 | 申报提醒 | 系统管理 | Phase 1 |
| 9 | DOI自动补全集成 | 论文登记 | Phase 1 |
| 10 | 专利法律状态同步 | 专利登记 | Phase 1 |
| 11 | 转化跟踪 | 成果模块 | Phase 2 |
| 12 | 自定义报表 | 统计看板 | Phase 2 |
| 13 | 高级检索 + ES迁移 | 全文检索 | Phase 2 |
| 14 | 移动端适配 | 全部 | Phase 2 |
