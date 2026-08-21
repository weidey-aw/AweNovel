# SQL 脚本说明

| 文件 | 说明 |
|---|---|
| `awe_novel.sql` | 全新安装初始化脚本：建库 `awe_novel` + 全部建表 + 初始数据（管理员、角色、菜单、字典、参数、等级配置等） |
| `migrate_to_awe_novel.sql` | 旧库迁移脚本：将已有旧库（默认 `sp_awblog`）全量复制到 `awe_novel`（结构+数据+索引+自增，含 Flowable 表） |

## 使用方式

```bash
# 全新安装：建库 + 建表 + 初始数据
mysql -uroot -p < sql/awe_novel.sql

# 已有旧库（如 sp_awblog）需保留数据时：全量迁移到 awe_novel
mysql -uroot -p --default-character-set=utf8mb4 < sql/migrate_to_awe_novel.sql
```

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
