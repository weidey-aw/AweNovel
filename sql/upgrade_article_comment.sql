-- ============================================================
-- 文章 / 评论 管理权限升级
-- 可重复执行
-- ============================================================

-- 评论表补齐 update_time（实体基类会写入，缺失会导致发表评论报 Unknown column）
SET @has_update_time := (SELECT COUNT(*) FROM information_schema.COLUMNS
                         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'gal_comment' AND COLUMN_NAME = 'update_time');
SET @ddl := IF(@has_update_time = 0,
  'ALTER TABLE `gal_comment` ADD COLUMN `update_time` datetime NULL COMMENT ''更新时间'' AFTER `create_time`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 文章管理（204）按钮
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2020, '文章新增', 204, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'community:article:add', '#', 'admin', sysdate(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2020);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2021, '文章修改', 204, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'community:article:edit', '#', 'admin', sysdate(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2021);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2022, '文章删除', 204, 3, '', NULL, '', 1, 0, 'F', '0', '0', 'community:article:remove', '#', 'admin', sysdate(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2022);

-- 评论管理（205）按钮
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2023, '评论删除', 205, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'community:comment:remove', '#', 'admin', sysdate(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2023);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2024, '评论隐藏', 205, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'community:comment:edit', '#', 'admin', sysdate(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2024);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 2020), (1, 2021), (1, 2022), (1, 2023), (1, 2024);

SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id BETWEEN 2020 AND 2024;
