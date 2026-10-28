-- ============================================================
-- 会社 / 标签 管理升级
--   1) gal_brand 增加官方网站字段 website（可空）
--   2) 会社、标签的按钮权限（挂到「会社管理」201 /「标签管理」202）
-- 可重复执行
-- ============================================================

-- 1) 会社官网外链
SET @has_website := (SELECT COUNT(*) FROM information_schema.COLUMNS
                     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'gal_brand' AND COLUMN_NAME = 'website');
SET @sql := IF(@has_website = 0,
  'ALTER TABLE gal_brand ADD COLUMN website varchar(255) NULL COMMENT ''官方网站'' AFTER logo',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 会社管理按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2011, '会社新增', 201, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'community:brand:add', '#', 'admin', sysdate(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2011);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2012, '会社修改', 201, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'community:brand:edit', '#', 'admin', sysdate(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2012);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2013, '会社删除', 201, 3, '', NULL, '', 1, 0, 'F', '0', '0', 'community:brand:remove', '#', 'admin', sysdate(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2013);

-- 3) 标签管理按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2014, '标签新增', 202, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'community:tag:add', '#', 'admin', sysdate(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2014);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2015, '标签修改', 202, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'community:tag:edit', '#', 'admin', sysdate(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2015);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2016, '标签删除', 202, 3, '', NULL, '', 1, 0, 'F', '0', '0', 'community:tag:remove', '#', 'admin', sysdate(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2016);

-- 4) 授权给超级管理员角色
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 2011), (1, 2012), (1, 2013), (1, 2014), (1, 2015), (1, 2016);

-- 5) 校验
SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id BETWEEN 2011 AND 2016;
SELECT brand_id, name, website FROM gal_brand LIMIT 3;
