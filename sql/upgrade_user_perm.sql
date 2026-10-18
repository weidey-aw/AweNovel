-- ============================================================================
-- AweNovel 权限增量升级：新增「手动重置密码」按钮权限
-- ----------------------------------------------------------------------------
-- 背景：重置密码拆分为两种模式
--   * 系统自动重置  PUT /system/user/resetPwd        权限 system:user:resetPwd
--   * 管理员手动重置 PUT /system/user/resetPwdManual  权限 system:user:resetPwdManual（本脚本新增）
--
-- 用法：mysql -uroot -p --default-character-set=utf8mb4 < sql/upgrade_user_perm.sql
-- 可重复执行。
-- ============================================================================

USE `awe_novel`;

SET NAMES utf8mb4;

START TRANSACTION;

DELETE FROM `sys_menu` WHERE `menu_id` = 1007;

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`,
   `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES
  (1007, '手动重置密码', 100, 8, '', NULL, '', 1, 0, 'F', '0', '0',
   'system:user:resetPwdManual', '#', 'admin', NOW(), '', NULL, '管理员指定新密码进行重置');

-- 授权给超级管理员角色（菜单管理里可再调整）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 1007);

COMMIT;

SELECT m.menu_id, m.menu_name, m.perms
FROM `sys_menu` m
WHERE m.menu_id = 1007;
