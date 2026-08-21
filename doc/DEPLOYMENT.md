# AweNovel 部署教程

本文档面向生产环境，讲解 AweNovel 的完整部署流程：环境准备、数据库初始化、后端构建部署、前端构建与 Nginx 配置、HTTPS、以及常见问题排查。

> 项目地址：`git@github.com:weidey-aw/AweNovel.git`（默认分支 `dev`）

---

## 1. 架构总览

```
                        ┌─────────────────────────────┐
   用户浏览器 ──HTTPS──▶ │ Nginx（443/80）             │
                        │  ├─ /                → web 用户端静态资源
                        │  ├─ /login /community … → 反向代理 :9090
                        │  └─ admin.域名 或独立端口 → admin 管理端静态资源
                        └─────────────┬───────────────┘
                                      │ HTTP 9090
                        ┌─────────────▼───────────────┐
                        │ Spring Boot (weidey-admin)  │
                        │  Java 17 / Spring Boot 3.3  │
                        └──┬──────────┬───────────┬───┘
                           │          │           │
                      ┌────▼───┐  ┌───▼────┐  ┌───▼─────────┐
                      │ MySQL 8│  │ Redis  │  │ 上传目录     │
                      │ awe_novel│ │ 6379   │  │ /home/awenovel│
                      └────────┘  └────────┘  └─────────────┘
```

- 后端：Spring Boot 3.3.7，端口 **9090**，context-path `/`。
- 前端：Vue 3 构建产物由 Nginx 托管（用户端 `web-ui/web/dist`，管理端 `web-ui/admin/dist`）。
- 数据库：MySQL 8，库名 `awe_novel`；Flowable 工作流表（`ACT_*`）首次启动自动创建。
- 缓存：Redis（验证码、登录令牌、配置缓存）。

---

## 2. 环境准备

| 组件 | 版本要求 | 说明 |
|---|---|---|
| JDK | 17+ | 运行后端 |
| Maven | 3.9+ | 构建后端 |
| Node.js | 20+ | 构建前端 |
| pnpm | 8+（建议 11.x） | 前端包管理 |
| MySQL | 8.0+ | 业务数据库 |
| Redis | 6+ | 缓存 |
| Nginx | 1.20+ | 静态资源与反向代理 |

以 Ubuntu 22.04 / Debian 12 为例：

```bash
# JDK 17
sudo apt update && sudo apt install -y openjdk-17-jdk
java -version

# Maven
sudo apt install -y maven   # 或手动安装，确保 mvn -version ≥ 3.9

# Node.js 20 + pnpm
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
sudo corepack enable
corepack prepare pnpm@11.7.0 --activate
node -v && pnpm -v

# MySQL 8 / Redis / Nginx
sudo apt install -y mysql-server redis-server nginx
sudo systemctl enable --now mysql redis-server nginx
```

---

## 3. 获取代码

```bash
git clone git@github.com:weidey-aw/AweNovel.git
cd AweNovel
git checkout dev        # 开发分支
```

---

## 4. 数据库初始化

```bash
# 使用 sql/awe_novel.sql 一键建库、建表、写入初始数据
mysql -uroot -p < sql/awe_novel.sql
```

脚本包含：

- 建库 `awe_novel`（utf8mb4）并授权说明；
- 系统表：`sys_user`、`sys_role`、`sys_menu`、`sys_user_role`、`sys_role_menu`、`sys_dict_type`、`sys_dict_data`、`sys_config`、`sys_notice`、`sys_oper_log`、`sys_logininfor`、`sys_user_online`；
- 社区表：`gal_game`、`gal_brand`、`gal_tag`、`gal_game_tag`、`gal_article`、`gal_resource`、`gal_comment`、`gal_rating`、`gal_sign_record`、`gal_point_log`、`gal_user_profile`、`gal_user_favorite`、`gal_user_follow`、`gal_user_like`、`gal_message`、`gal_level_config`；
- 初始数据：管理员账号 `admin/admin123`、超级管理员角色、系统菜单与字典、`sys_config` 系统参数、等级配置（0-6 级）、管理员社区画像。

> 注意：Flowable 的 `ACT_*` 表无需手动创建，应用首次启动会自动建表。
> 生产环境请立即修改 `admin` 密码与默认配置。

---

## 5. 后端构建与部署

### 5.1 构建

```bash
cd AweNovel
mvn clean package -DskipTests
# 产物：weidey-admin/target/weidey-admin.jar
```

### 5.2 创建运行目录与账号

```bash
sudo mkdir -p /home/awenovel/uploadPath /home/awenovel/logs
sudo chown -R $USER:$USER /home/awenovel
```

### 5.3 环境变量

所有敏感配置通过环境变量注入（见 [第 8 节](#8-环境变量清单)）。示例（写入 `/etc/awenovel/awenovel.env`）：

```bash
# 数据库（pro 环境无默认密码，必须设置）
DB_URL=jdbc:mysql://127.0.0.1:3306/awe_novel?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8
DB_USERNAME=root
DB_PASSWORD=你的数据库密码

# JWT 密钥（至少 32 字节，务必随机生成）
TOKEN_SECRET=$(openssl rand -base64 48)

# 邮箱（注册/找回密码验证码，QQ 邮箱示例）
MAIL_HOST=smtp.qq.com
MAIL_USERNAME=你的QQ邮箱
MAIL_PASSWORD=你的SMTP授权码

# DeepSeek（看板娘 AI）
DEEPSEEK_API_KEY=sk-你的DeepSeek密钥

# Druid 监控台（可选）
DRUID_USERNAME=admin
DRUID_PASSWORD=你的Druid密码

# Redis 密码（如有）
# REDIS_PASSWORD=xxx
```

### 5.4 使用 systemd 托管（推荐）

`/etc/systemd/system/awenovel.service`：

```ini
[Unit]
Description=AweNovel Spring Boot Service
After=network.target mysql.service redis-server.service

[Service]
User=awenovel
Group=awenovel
EnvironmentFile=/etc/awenovel/awenovel.env
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar /opt/awenovel/weidey-admin.jar --spring.profiles.active=pro
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo cp weidey-admin/target/weidey-admin.jar /opt/awenovel/
sudo systemctl daemon-reload
sudo systemctl enable --now awenovel
sudo systemctl status awenovel
```

### 5.5 验证后端

```bash
curl http://127.0.0.1:9090/
# 期望输出：欢迎使用AweNovel后台管理框架，当前版本：v3.8.7，请通过前端地址访问。
curl http://127.0.0.1:9090/captchaImage   # 返回 JSON（含验证码图片 base64）
```

---

## 6. 前端构建与部署

### 6.1 构建

```bash
cd AweNovel/web-ui
pnpm install
pnpm build
# 产物：web-ui/web/dist（用户端）、web-ui/admin/dist（管理端）
```

### 6.2 部署静态资源

```bash
sudo mkdir -p /var/www/awenovel/web /var/www/awenovel/admin
sudo cp -r web-ui/web/dist/* /var/www/awenovel/web/
sudo cp -r web-ui/admin/dist/* /var/www/awenovel/admin/
```

### 6.3 Nginx 配置

用户端（`/etc/nginx/sites-available/awenovel-web.conf`）：

```nginx
server {
    listen 80;
    server_name awe-novel.example.com;

    gzip on;
    gzip_types text/plain text/css application/javascript application/json image/svg+xml;

    # 用户端静态资源
    root /var/www/awenovel/web;
    index index.html;

    # 后端接口反向代理
    location ~ ^/(login|register|register/code|forget/code|forgetPwd|captchaImage|getInfo|getRouters|logout|community|ai|system|monitor|profile|common|code) {
        proxy_pass http://127.0.0.1:9090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        # SSE（看板娘流式聊天）必须关闭缓冲
        proxy_buffering off;
        proxy_read_timeout 300s;
    }

    # 前端路由（history 模式）
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

管理端（独立域名或子域名，`/etc/nginx/sites-available/awenovel-admin.conf`）：

```nginx
server {
    listen 80;
    server_name admin.awe-novel.example.com;

    root /var/www/awenovel/admin;
    index index.html;

    location ~ ^/(login|register|register/code|forget/code|forgetPwd|captchaImage|getInfo|getRouters|logout|community|ai|system|monitor|profile|common|code) {
        proxy_pass http://127.0.0.1:9090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/awenovel-web.conf /etc/nginx/sites-enabled/
sudo ln -s /etc/nginx/sites-available/awenovel-admin.conf /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

> 若管理端与用户端共用同一域名，可将管理端构建产物放到子路径下部署，需同步调整前端路由 base，本仓库默认按独立域名/端口方式部署。

---

## 7. HTTPS 配置（可选但推荐）

仓库**不包含**任何证书私钥（`src/main/resources/ssl/` 已加入 `.gitignore`）。推荐使用 Let's Encrypt：

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d awe-novel.example.com -d admin.awe-novel.example.com
```

如使用自有证书，请将证书放入 `weidey-admin/src/main/resources/ssl/` 并在 `application-pro.yml` 中取消注释 `server.ssl` 配置（密码通过 `SSL_KEY_STORE_PASSWORD` 环境变量注入），然后重新打包部署。

---

## 8. 环境变量清单

| 变量 | 必填 | 默认值 | 说明 |
|---|---|---|---|
| `DB_URL` | 生产必填 | `jdbc:mysql://127.0.0.1:3306/awe_novel?...` | 数据库连接串 |
| `DB_USERNAME` | 否 | `root` | 数据库用户名 |
| `DB_PASSWORD` | 生产必填 | 无（pro）/ `your-db-password`（dev） | 数据库密码 |
| `TOKEN_SECRET` | 生产必填 | 内置占位密钥 | JWT 密钥，至少 32 字节 |
| `MAIL_HOST` | 否 | `smtp.qq.com` | SMTP 服务器 |
| `MAIL_USERNAME` | 生产必填 | 占位 | 发件邮箱 |
| `MAIL_PASSWORD` | 生产必填 | 占位 | SMTP 授权码 |
| `DEEPSEEK_API_KEY` | 否（AI 不可用） | 占位 | DeepSeek API Key |
| `DEEPSEEK_BASE_URL` | 否 | `https://api.deepseek.com` | DeepSeek 接口地址 |
| `DEEPSEEK_MODEL` | 否 | `deepseek-chat` | 模型名 |
| `DEEPSEEK_PERSONA` | 否 | 伊卡洛斯人设 | 看板娘人设提示词 |
| `DRUID_USERNAME` / `DRUID_PASSWORD` | 否 | `admin` / `123456` | Druid 监控台账号（生产请修改） |
| `REDIS_PASSWORD` | 否 | 空 | Redis 密码（如有） |
| `SSL_KEY_STORE_PASSWORD` | 启用 HTTPS 时 | 空 | JKS 证书密码 |

---

## 9. 常见问题（FAQ）

**Q1：后端启动报数据库连接失败？**
检查 MySQL 是否启动、`DB_URL/DB_USERNAME/DB_PASSWORD` 是否正确、`awe_novel` 库是否已导入。首次启动请确认 `sql/awe_novel.sql` 已执行。

**Q2：验证码/登录 500？**
验证码与登录令牌依赖 Redis。确认 Redis 已启动且 `spring.data.redis` 配置（主机/端口/密码）与部署环境一致。

**Q3：`ACT_*` 表不存在？**
Flowable 会在首次启动自动建表，需要数据库账号具有建表权限；无需手动导入。

**Q4：注册/找回密码邮件发送失败？**
检查 `MAIL_USERNAME`、`MAIL_PASSWORD`（QQ 邮箱需使用 SMTP 授权码而非登录密码）以及 `sys.account.enable.Email` 配置是否为 `true`（`sys_config` 表）。

**Q5：看板娘无回复或报错？**
确认 `DEEPSEEK_API_KEY` 已配置且有效，用户有足够积分（每次对话消耗 1 积分），前端代理对 `/ai` 已启用 `proxy_buffering off`。

**Q6：上传图片 404？**
检查上传目录（pro 环境为 `/home/awenovel/uploadPath`）是否存在且对运行用户可写；Nginx 需将 `/profile` 路径代理到后端（后端通过 `ResourcesConfig` 将 `/profile/**` 映射到上传目录）。

**Q7：管理端登录后提示无权限？**
确认使用 `admin` 账号（角色 `admin`，权限 `*:*:*`）；如新建了管理角色，需在 `sys_menu`/`sys_role_menu` 中为其分配权限。

**Q8：端口被占用？**
后端默认 `9090`，可在 `application-dev.yml` / `application-pro.yml` 中修改 `server.port`。

---

## 10. 备份与升级建议

- **数据库备份**：定期执行 `mysqldump -uroot -p awe_novel > awe_novel_$(date +%F).sql`。
- **上传目录备份**：`/home/awenovel/uploadPath` 与数据库一起备份。
- **升级流程**：`git pull` → 执行增量 SQL（如有）→ `mvn clean package -DskipTests` → 替换 jar → `sudo systemctl restart awenovel`。
- **安全基线**：修改默认密码（`admin`、Druid）、更换 `TOKEN_SECRET`、定期轮换 `DEEPSEEK_API_KEY` 与 SMTP 授权码。
