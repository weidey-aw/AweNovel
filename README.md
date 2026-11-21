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
├── doc/                    # 开发文档（本地维护，不入仓库）
└── pom.xml                 # 后端 Maven 父工程（com.weidey:AweNovel）
```

> 说明：`weidey-*` 模块名与 `com.weidey` 包名保留开发者标识 `weidey`，项目对外统一命名为 **AweNovel**。

## 后端模块说明
- **weidey-AweNovel**（社区业务）：
  - `com.weidey.community`：游戏条目、制作会社、标签、资源、文章、评论、评分、积分、签到、关注/收藏/点赞、消息、内容审核。
  - `com.weidey.ai`：看板娘 AI 聊天（DeepSeek）。

## 核心功能

- **游戏库**：游戏条目（原名/译名/会社/标签/原画/剧本/声优/评分），关键词与标签筛选。
- **资源下载**：资源发布 → Flowable 审核 → 通过后按所需积分下载。
- **用户积分体系**：B 站式 0-6 等级（经验驱动）、每日签到 +5 积分 +5 经验、下载/AI 聊天消耗积分、积分流水审计。
- **内容审核**：Flowable 工作流（提交 → 审核 → 通过/拒绝）。
- **看板娘 AI**：**DeepSeek 鲸鱼娘**（常驻前台，DeepSeek 流式聊天，支持多轮上下文），每次消耗 1 积分；支持 **Live2D 动态立绘**（自动注入待机动作），无模型时自动降级为鲸鱼娘静态形象。
- **主题切换**：`白蓝`（默认）/ `暗夜色`，前台与管理后台统一生效，顶栏一键切换并记忆。
- **社区互动**：评论（楼中楼）、评分（1-10）、点赞/关注、站内消息。
- **收藏**：游戏与文章一键收藏，用户中心「我的收藏」按类型筛选。
- **资源反馈**：资源失效 / 无法运行 / 缺少补丁 / 其他，附文字说明，直接以站内消息通知资源上传者（未读/已读）。
- **Markdown 增强**：代码块语法高亮（highlight.js），管理端编辑器支持代码块插入与分屏实时预览。

## 快速开始

详细部署步骤见本地 `doc/DEPLOYMENT.md`（开发文档不入仓库），此处为本地开发速览。

### Windows 一键测试包（推荐体验方式）

```powershell
# 在仓库根目录执行：构建前端 → 打进 jar → 生成一键启动包与 zip
.\scripts\build-test-package.ps1
```

产物在 `build\test-package\AweNovel-test\`（以及 `build\AweNovel-test-<时间>.zip`）：

```
AweNovel-test\
├── AweNovel.jar          # 后端 + 前台页面（/） + 管理后台（/admin/）
├── start.bat             # 一键启动（校验 Java/MySQL/Redis，自动打开浏览器）
├── stop.bat              # 停止
├── init-db.bat           # 首次使用：初始化数据库
├── config\               # application-test.yml / db.bat（本机凭据，勿公开）
├── sql\init.sql          # 数据库初始化脚本
├── upload\ logs\         # 上传文件与日志
└── README.txt            # 使用说明
```

启动后：前台 <http://localhost:8088/>，后台 <http://localhost:8088/admin/>（`admin` / `admin123`）。

> 运行时需要本机已启动 **MySQL 与 Redis**；首次使用先执行 `init-db.bat`。

### 环境要求

- JDK 17+、Maven 3.9+、Node 20+、pnpm 8+
- MySQL 8、Redis

### 数据库

1. 全新安装：创建数据库并导入初始化脚本（含建表与初始数据）：

```bash
mysql -uroot -p < sql/awe_novel.sql
```

2. 已有旧库（如 `sp_awblog`）需要保留历史数据时，改用迁移脚本全量复制到 `awe_novel`：

```bash
mysql -uroot -p --default-character-set=utf8mb4 < sql/migrate_to_awe_novel.sql
```

3. Flowable 的 `ACT_*` 表会在应用首次启动时自动创建。

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

## 接口文档

完整后端接口文档按模块维护在本地 `doc/api/`（含前端对接状态与优先级建议）。
为保持仓库干净，**开发文档已加入 `.gitignore`，不随仓库分发**；需要时在本地查看即可。

## 主要接口速览

- 认证：`POST /login`、`POST /register`、`GET /captchaImage`
- 社区公开：`GET /community/game/list`、`GET /community/game/{id}`、`GET /community/article/*`、`GET /community/comment/list`、`GET /community/resource/list`
- 社区登录：`POST /community/sign`、`POST /community/resource/download/{id}`、`GET /community/user/profile` 等
- 审核（管理）：`GET /community/review/tasks`、`POST /community/review/approve/{pid}`、`POST /community/review/reject/{pid}`
- 看板娘：`POST /ai/chat`（SSE，带历史上下文）、`POST /ai/chat/sync`（兜底）

## 部署与运维

- 部署教程：本地 `doc/DEPLOYMENT.md`（开发文档不入仓库）
- 一键测试包（Windows）：见 [`scripts/build-test-package.ps1`](scripts/build-test-package.ps1) 与 `build/test-package/AweNovel-test/`
- 数据库初始化：[`sql/awe_novel.sql`](sql/awe_novel.sql)
- 旧库迁移：[`sql/migrate_to_awe_novel.sql`](sql/migrate_to_awe_novel.sql)

## 开源协议与致谢

- 本项目基于 [RuoYi](https://gitee.com/y_project/RuoYi)（MIT License）重构，保留了框架的归属注释。
- 前端基于 Vue 3 / Element Plus 生态构建。
- 内容仅供学习交流，请支持正版游戏。
