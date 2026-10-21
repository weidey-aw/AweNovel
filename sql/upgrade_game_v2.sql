-- ============================================================
-- 游戏模块升级（轮播图 + 审核状态 + 按钮权限）
-- 可重复执行；执行前请备份 gal_game
--
-- 状态语义（gal_game.status）：
--   0 已下架   1 已发布   2 待审核   3 审核驳回   4 草稿
-- 前台只展示 status = '1' 的游戏
-- ============================================================

-- 1) 轮播图字段（JSON 数组，最多 7 张，WebP）
SET @has_carousel := (SELECT COUNT(*) FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'gal_game' AND COLUMN_NAME = 'carousel');
SET @sql := IF(@has_carousel = 0,
  'ALTER TABLE gal_game ADD COLUMN carousel text NULL COMMENT ''轮播图(JSON数组，最多7张)'' AFTER cover',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 状态注释同步（默认草稿）
ALTER TABLE gal_game MODIFY COLUMN status char(1) NULL DEFAULT '4'
  COMMENT '状态（0下架 1已发布 2待审核 3审核驳回 4草稿）';

-- 3) 游戏按钮权限（挂在「游戏管理」菜单 200 下）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2007, '游戏新增', 200, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'community:game:add', '#', 'admin', sysdate(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2007);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2008, '游戏修改', 200, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'community:game:edit', '#', 'admin', sysdate(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2008);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2009, '游戏删除', 200, 3, '', NULL, '', 1, 0, 'F', '0', '0', 'community:game:remove', '#', 'admin', sysdate(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2009);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2010, '游戏发布', 200, 4, '', NULL, '', 1, 0, 'F', '0', '0', 'community:game:publish', '#', 'admin', sysdate(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2010);

-- 4) 授权给超级管理员角色
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 2007), (1, 2008), (1, 2009), (1, 2010);

-- 5) 校验
SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id BETWEEN 2007 AND 2010;
