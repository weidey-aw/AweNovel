# AweNovel 测试版（Windows 一键启动）

这是一个**本地测试用**的打包版本：单个 jar 同时提供后端接口、前台页面（`/`）与管理后台（`/admin/`）。

## 一、运行前提

1. Windows 10/11
2. **JDK 17 或更高**（设置好 `JAVA_HOME`，或把 JDK 放到本目录下的 `jdk\` 文件夹）
3. **MySQL 5.7 / 8.x** 已启动（默认 `127.0.0.1:3306`）
4. **Redis** 已启动（默认 `127.0.0.1:6379`，无密码）

> 打包时已按本机环境写好 `config\application-test.yml`（数据库账号、Redis、DeepSeek Key）。
> 换机器时请先修改该文件，或重新执行打包脚本生成。

## 二、首次使用

1. 确认 MySQL、Redis 已启动；
2. 双击 **`init-db.bat`**：导入 `sql\init.sql`（会**重建** `awe_novel` 库并写入演示数据）；
3. 双击 **`start.bat`**：等待控制台提示启动成功后会自动打开浏览器：
   - 前台：http://localhost:8088/
   - 后台：http://localhost:8088/admin/
4. 默认账号：`admin` / `admin123`

## 三、日常使用

- 启动：`start.bat`
- 停止：`stop.bat`（或关闭名为 “AweNovel Server” 的窗口）
- 日志：运行窗口内直接输出；`logs\` 目录下也有日志文件
- 上传文件保存在 `upload\` 目录

## 四、常见问题

| 现象 | 处理 |
|---|---|
| 启动窗口一闪而过 | 在 cmd 里执行 `start.bat` 查看报错；多为 Java 未安装或未设置 `JAVA_HOME` |
| 提示数据库连接失败 | MySQL 未启动 / `config\application-test.yml` 的账号密码不对 |
| 页面能打开但数据为空 | 先执行 `init-db.bat` 初始化数据库 |
| 端口 8088 被占用 | 修改 `config\application-test.yml` 的 `server.port`，并同步修改 `start.bat` 里的 `APP_PORT` |
| 看板娘不显示立绘 | `live2d` 模型目录不入库，测试包若未包含模型会自动降级为鲸鱼娘静态形象，属正常现象 |

## 五、安全提示

- 该测试包内含本机数据库口令与 DeepSeek Key（`config\` 目录），**请勿上传到公开仓库或对外分发**。
- 正式部署请使用环境变量注入凭据，参考项目内 `doc/DEPLOYMENT.md`（开发文档，不入仓库）。
