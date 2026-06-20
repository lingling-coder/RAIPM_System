# 科研成果与知识产权管理系统

> 科研成果与知识产权一体化全生命周期管理系统

## 项目简介

覆盖研究院的**论文、专利、软件著作权**三大类成果从登记、审核、缴费、维护、转化、统计到注销的闭环管理。系统依托自动化接口减少人工录入，配置多级预警机制规避逾期风险，提供多维度数据看板支撑管理决策。

**核心价值**：确保科研成果与知识产权的全生命周期可追溯、费用不逾期、数据可分析。

## 技术栈

| 层面 | 技术 | 版本 |
|------|------|------|
| **后端框架** | Spring Boot | 4.1.x |
| **JDK** | JDK 21 LTS | 21 |
| **ORM** | MyBatis-Plus | 3.5.x |
| **安全框架** | Spring Security + JWT | — |
| **API 文档** | SpringDoc OpenAPI | 3.0.x |
| **DTO 映射** | MapStruct | 1.6.x |
| **数据库** | MySQL | 8.4 LTS |
| **缓存** | Redis | 7.x |
| **前端框架** | Vue 3 + Element Plus | Vue 3.4+ / Element Plus 2.9+ |
| **状态管理** | Pinia | 3.x |
| **构建工具** | Vite | 5.x |
| **图表** | ECharts 5.5 + vue-echarts 7 | — |
| **Excel 处理** | EasyExcel | 4.0+ |

## 模块结构

```
achievement-parent
├── achievement-common          # 公共工具模块
├── achievement-framework       # 核心框架（安全、权限、配置）
├── achievement-module
│   ├── achievement-system      # 系统管理（用户、角色、部门、字典）
│   ├── achievement-paper       # 论文管理
│   ├── achievement-patent      # 专利管理
│   ├── achievement-copyright   # 软件著作权管理
│   ├── achievement-fee         # 费用管理（缴费、预警）
│   ├── achievement-reminder    # 提醒通知（邮件）
│   └── achievement-integration # 外部系统集成
└── achievement-web             # 前端（Vue 3 + Element Plus）
```

## 快速开始

### 环境要求

- JDK 21+
- MySQL 8.0+
- Redis 7.x
- Node.js 18+
- Maven 3.9+（或使用项目自带的 `mvnw`）

### 后端启动

```bash
# 1. 创建数据库并执行初始化脚本
#   执行 db/ 目录下的 SQL 脚本

# 2. 修改数据库配置
#   编辑 achievement-framework/src/main/resources/application-dev.yml

# 3. 编译并启动
./mvnw clean install -DskipTests
./mvnw spring-boot:run -pl achievement-framework
```

### 前端启动

```bash
cd achievement-web
npm install
npm run dev
```

访问地址：`http://localhost:5173`

## 主要功能

- **成果登记**：论文、专利、软件著作权的在线登记与审核
- **费用管理**：年费缴纳记录、逾期预警、多级提醒
- **权限管理**：RBAC 角色权限 + 部门数据隔离 + 涉密数据独立权限
- **数据看板**：多维度统计图表，支撑管理决策
- **系统集成**：对接统一身份认证、HR、财务等业务系统

## 浏览器兼容

| 浏览器 | 最低版本 |
|--------|---------|
| Google Chrome | 120+ |
| Microsoft Edge | 120+ |
| 360 安全浏览器 | 13+ |

## 项目状态

**当前版本**：v0.1.0-SNAPSHOT（开发阶段）

## 许可

本项目仅供学习和参考。
