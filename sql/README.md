# SQL 脚本说明

| 文件 | 说明 |
|---|---|
| `awe_novel.sql` | 全新安装初始化脚本：建库 `awe_novel` + 全部建表 + 初始数据（管理员、角色、菜单、字典、参数、等级配置等） |
| `migrate_to_awe_novel.sql` | 旧库迁移脚本：将已有旧库（默认 `sp_awblog`）全量复制到 `awe_novel`（结构+数据+索引+自增，含 Flowable 表） |
| `upgrade_menu_v2.sql` | **菜单结构升级脚本**：把 `sys_menu` / `sys_role_menu` 重建为「系统管理 / 系统监控 / 网站运营」三大模块，供管理端动态路由（`/getRouters`）使用；可重复执行 |

## 使用方式

```bash
# 全新安装：建库 + 建表 + 初始数据
mysql -uroot -p < sql/awe_novel.sql

# 已有旧库（如 sp_awblog）需保留数据时：全量迁移到 awe_novel
mysql -uroot -p --default-character-set=utf8mb4 < sql/migrate_to_awe_novel.sql

# 已有 awe_novel 库、仅需升级菜单结构（管理端动态导航）
mysql -uroot -p --default-character-set=utf8mb4 < sql/upgrade_menu_v2.sql
```

## 菜单与动态路由约定

管理端导航由后端 `GET /getRouters` 下发，`sys_menu.component` 与前端文件路径一一对应：

| sys_menu.component | 前端文件 |
|---|---|
| `system/user/index` | `web-ui/admin/src/views/system/user/index.vue` |
| `system/role/index` | `web-ui/admin/src/views/system/role/index.vue` |
| `system/menu/index` | `web-ui/admin/src/views/system/menu/index.vue` |
| `system/dict/index` | `web-ui/admin/src/views/system/dict/index.vue` |
| `system/config/index` | `web-ui/admin/src/views/system/config/index.vue` |
| `system/notice/index` | `web-ui/admin/src/views/system/notice/index.vue` |
| `system/log/index` | `web-ui/admin/src/views/system/log/index.vue` |
| `monitor/online/index` | `web-ui/admin/src/views/monitor/online/index.vue` |
| `monitor/job/index` | `web-ui/admin/src/views/monitor/job/index.vue` |
| `monitor/druid/index` | `web-ui/admin/src/views/monitor/druid/index.vue` |
| `monitor/server/index` | `web-ui/admin/src/views/monitor/server/index.vue` |
| `monitor/cache/index` | `web-ui/admin/src/views/monitor/cache/index.vue` |
| `operation/game/index` | `web-ui/admin/src/views/operation/game/index.vue` |
| `operation/brand/index` | `web-ui/admin/src/views/operation/brand/index.vue` |
| `operation/tag/index` | `web-ui/admin/src/views/operation/tag/index.vue` |
| `operation/resource/index` | `web-ui/admin/src/views/operation/resource/index.vue` |
| `operation/article/index` | `web-ui/admin/src/views/operation/article/index.vue` |
| `operation/comment/index` | `web-ui/admin/src/views/operation/comment/index.vue` |
| `operation/review/index` | `web-ui/admin/src/views/operation/review/index.vue` |

> 「仪表盘」为前端静态路由，不在 `sys_menu` 中，固定显示在导航第一位。
> 新增页面时：先在 `sys_menu` 插入菜单（`component` 填相对路径，不含 `.vue`），前端按同路径创建 `.vue` 文件即可。

## 脚本内容

- **系统表**（RBAC）：`sys_user`、`sys_role`、`sys_menu`、`sys_user_role`、`sys_role_menu`、`sys_dict_type`、`sys_dict_data`、`sys_config`、`sys_notice`、`sys_oper_log`、`sys_logininfor`、`sys_user_online`
- **社区表**（gal_*）：`gal_game`、`gal_brand`、`gal_tag`、`gal_game_tag`、`gal_article`、`gal_resource`、`gal_comment`、`gal_rating`、`gal_sign_record`、`gal_point_log`、`gal_user_profile`、`gal_user_favorite`、`gal_user_follow`、`gal_user_like`、`gal_message`、`gal_level_config`
- **初始数据**：
  - 管理员账号：`admin / admin123`（BCrypt 加密存储）
  - 角色：超级管理员（`admin`）、普通角色（`common`）
  - 系统菜单与按钮权限、字典类型与数据、系统参数（验证码/注册/邮箱开关等）
  - 等级配置（0-6 级经验阈值）、管理员社区画像、示例标签

## 注意事项

- Flowable 工作流表（`ACT_*`）由应用首次启动自动创建，无需手动导入。
- 生产环境导入后请立即修改 `admin` 密码与 `sys_config` 中的系统参数。
- 如使用独立数据库账号，请取消脚本头部注释的 `CREATE USER` / `GRANT` 语句并设置强密码。
