# AweNovel —— Galgame 社区网站

基于若依（RuoYi 3.8.7）重构的 **Galgame 社区网站**，从作者学生时代的个人博客系统（awblog）全面升级而来，现已正式命名为 **AweNovel**。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17、Spring Boot 3.3.7、Spring Security（JWT + RBAC）、MyBatis-Plus 3.5.7、MySQL 8、Redis、Druid、Flowable 7.1（内容审核工作流） |
| 前端 | Vue 3 + TypeScript + Vite + Element Plus + Pinia + Vue Router（pnpm workspace monorepo） |
| AI | DeepSeek（OpenAI 兼容，SSE 流式，看板娘聊天） |

## 目录结构

```
AweNovel/
├── weidey-admin/           # 启动入口 + 管理端 Controller
├── weidey-framework/       # 框架核心（安全/配置/AOP）
├── weidey-system/          # 系统模块（用户/角色/菜单/字典/配置/公告/日志）
├── weidey-common/          # 通用工具
├── weidey-AweNovel/        # 社区业务模块（Galgame 内容/用户/积分/AI/审核）
├── web-ui/                 # 前端（pnpm monorepo：web 用户端 + admin 管理端 + shared 共享包）
├── sql/                    # 数据库初始化脚本（awe_novel.sql 完整初始化）
├── doc/                    # 部署与运维文档（DEPLOYMENT.md 部署教程）
└── pom.xml                 # 后端 Maven 父工程（com.weidey:AweNovel）
```

> 说明：`weidey-*` 模块名与 `com.weidey` 包名保留开发者标识 `weidey`，项目对外统一命名为 **AweNovel**。

## 后端模块说明

- **weidey-system**：若依精简后的 RBAC（用户/角色/菜单/字典/配置/公告/日志）。已删除部门、岗位、数据权限、定时任务（quartz）、代码生成器等无关功能。
- **weidey-AweNovel**（社区业务）：
  - `com.weidey.community`：游戏条目、制作会社、标签、资源、文章、评论、评分、积分、签到、关注/收藏/点赞、消息、内容审核。
  - `com.weidey.ai`：看板娘 AI 聊天（DeepSeek）。

## 核心功能

- **游戏库**：游戏条目（原名/译名/会社/标签/原画/剧本/声优/评分），关键词与标签筛选。
- **资源下载**：资源发布 → Flowable 审核 → 通过后按所需积分下载。
- **用户积分体系**：B 站式 0-6 等级（经验驱动）、每日签到 +5 积分 +5 经验、下载/AI 聊天消耗积分、积分流水审计。
- **内容审核**：Flowable 工作流（提交 → 审核 → 通过/拒绝）。
- **看板娘 AI**：DeepSeek 流式聊天（SSE），每次消耗 1 积分。
- **社区互动**：评论（楼中楼）、评分（1-10）、点赞/收藏/关注、站内消息。

## 快速开始

详细部署步骤见 [`doc/DEPLOYMENT.md`](doc/DEPLOYMENT.md)，此处为本地开发速览。

### 环境要求

- JDK 17+、Maven 3.9+、Node 20+、pnpm 8+
- MySQL 8、Redis

### 数据库

1. 创建数据库并导入初始化脚本（含建表与初始数据）：

```bash
mysql -uroot -p < sql/awe_novel.sql
```

2. Flowable 的 `ACT_*` 表会在应用首次启动时自动创建。

### 后端

```bash
mvn clean package -DskipTests
java -jar weidey-admin/target/weidey-admin.jar
```

默认端口 9090（开发环境启用 HTTP；生产环境 HTTPS 配置见部署文档）。

### 前端

```bash
cd web-ui
pnpm install
pnpm dev:web      # 用户端 http://localhost:5173
pnpm dev:admin    # 管理端 http://localhost:5174
```

## 环境变量（敏感配置）

所有密钥通过环境变量注入，仓库中不包含任何真实密钥：

| 变量 | 说明 |
|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | 数据库连接（默认 `jdbc:mysql://127.0.0.1:3306/awe_novel`） |
| `TOKEN_SECRET` | JWT 密钥（至少 32 字节，生产必须覆盖） |
| `MAIL_HOST` / `MAIL_USERNAME` / `MAIL_PASSWORD` | 邮箱（注册/找回密码验证码） |
| `DEEPSEEK_API_KEY` | DeepSeek API Key（看板娘） |
| `DEEPSEEK_BASE_URL` / `DEEPSEEK_MODEL` / `DEEPSEEK_PERSONA` | DeepSeek 可选配置 |
| `DRUID_USERNAME` / `DRUID_PASSWORD` | Druid 监控台账号密码 |
| `REDIS_PASSWORD` | Redis 密码（如有） |

## 默认账号

初始化脚本内置管理员账号（请在部署后立即修改密码）：

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | `admin123` | 超级管理员 |

## 主要接口

- 认证：`POST /login`、`POST /register`、`GET /captchaImage`
- 社区公开：`GET /community/game/list`、`GET /community/game/{id}`、`GET /community/article/*`、`GET /community/comment/list`、`GET /community/resource/list`
- 社区登录：`POST /community/sign`、`POST /community/resource/download/{id}`、`GET /community/user/profile` 等
- 审核（管理）：`GET /community/review/tasks`、`POST /community/review/approve/{pid}`、`POST /community/review/reject/{pid}`
- 看板娘：`GET /ai/chat?message=xxx`（SSE）

## 部署与运维

- 部署教程：[`doc/DEPLOYMENT.md`](doc/DEPLOYMENT.md)
- 数据库初始化：[`sql/awe_novel.sql`](sql/awe_novel.sql)

## 开源协议与致谢

- 本项目基于 [RuoYi](https://gitee.com/y_project/RuoYi)（MIT License）重构，保留了框架的归属注释。
- 前端基于 Vue 3 / Element Plus 生态构建。
- 内容仅供学习交流，请支持正版游戏。
