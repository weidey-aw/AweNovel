-- ============================================================
-- 资源管理升级（资源来源拆分 + 正版资源 + 资源类型字典）
-- 可重复执行
-- ============================================================

-- 1) 资源来源表：一个资源可有多条来源（链接/提取码/解压码）
CREATE TABLE IF NOT EXISTS `gal_resource_source` (
  `source_id`   bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '来源ID',
  `resource_id` bigint(20)   NOT NULL                COMMENT '资源ID',
  `source_type` varchar(20)  DEFAULT 'netdisk'       COMMENT '来源类型（netdisk网盘/magnet磁力/direct直链/torrent种子/other其他）',
  `url`         text                                 COMMENT '资源链接',
  `extract_pwd` varchar(100) DEFAULT ''              COMMENT '提取码',
  `unzip_pwd`   varchar(100) DEFAULT ''              COMMENT '解压码',
  `remark`      varchar(255) DEFAULT ''              COMMENT '备注',
  `sort`        int(11)      DEFAULT 0               COMMENT '排序',
  `create_time` datetime                             COMMENT '创建时间',
  PRIMARY KEY (`source_id`),
  KEY `idx_gal_resource_source_resource` (`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏资源来源表';

-- 2) 正版资源表：一个游戏可有多条正版来源，不关联用户
CREATE TABLE IF NOT EXISTS `gal_official_resource` (
  `official_id` bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '正版资源ID',
  `game_id`     bigint(20)   NOT NULL                COMMENT '游戏ID',
  `title`       varchar(255) NOT NULL                COMMENT '标题',
  `source_type` varchar(20)  DEFAULT 'official'      COMMENT '类型（official官网/store商店/steam/dlsite/other）',
  `url`         varchar(500) NOT NULL                COMMENT '官方链接',
  `icon`        varchar(255) DEFAULT ''              COMMENT '小图标（相对路径或图标名）',
  `sort`        int(11)      DEFAULT 0               COMMENT '排序',
  `create_time` datetime                             COMMENT '创建时间',
  PRIMARY KEY (`official_id`),
  KEY `idx_gal_official_game` (`game_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏正版资源表';

-- 3) 旧数据迁移：gal_resource.url / extract_pwd → 资源来源表（幂等）
INSERT INTO `gal_resource_source` (`resource_id`, `source_type`, `url`, `extract_pwd`, `unzip_pwd`, `remark`, `sort`, `create_time`)
SELECT r.`resource_id`, IFNULL(NULLIF(r.`type`, ''), 'netdisk'), r.`url`, IFNULL(r.`extract_pwd`, ''), '', '历史数据迁移', 0, r.`create_time`
FROM `gal_resource` r
WHERE r.`url` IS NOT NULL AND r.`url` <> ''
  AND NOT EXISTS (SELECT 1 FROM `gal_resource_source` s WHERE s.`resource_id` = r.`resource_id`);

-- 4) 字典：游戏资源类型
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`)
SELECT * FROM (
  SELECT '游戏资源类型' AS dict_name, 'game_resource_type' AS dict_type, '0' AS status, 'admin' AS create_by, sysdate() AS create_time, '社区资源来源类型' AS remark
) t WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_type` d WHERE d.`dict_type` = t.`dict_type`);

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT * FROM (
            SELECT 1 AS dict_sort, '网盘' AS dict_label, 'netdisk' AS dict_value, 'game_resource_type' AS dict_type, '' AS css_class, 'primary' AS list_class, 'N' AS is_default, '0' AS status, 'admin' AS create_by, sysdate() AS create_time, '网盘分享' AS remark
  UNION ALL SELECT 2, '磁力链接', 'magnet',  'game_resource_type', '', 'warning', 'N', '0', 'admin', sysdate(), '磁力/种子'
  UNION ALL SELECT 3, '直链下载', 'direct',  'game_resource_type', '', 'success', 'N', '0', 'admin', sysdate(), '直链/离线下载'
  UNION ALL SELECT 4, '种子文件', 'torrent', 'game_resource_type', '', 'info',    'N', '0', 'admin', sysdate(), 'BT 种子'
  UNION ALL SELECT 5, '其他',     'other',   'game_resource_type', '', 'info',    'N', '0', 'admin', sysdate(), '其他来源'
) t WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` d WHERE d.`dict_type` = t.`dict_type` AND d.`dict_value` = t.`dict_value`);

-- 5) 字典：正版资源类型
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`)
SELECT * FROM (
  SELECT '正版资源类型' AS dict_name, 'game_official_type' AS dict_type, '0' AS status, 'admin' AS create_by, sysdate() AS create_time, '正版来源类型' AS remark
) t WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_type` d WHERE d.`dict_type` = t.`dict_type`);

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT * FROM (
            SELECT 1 AS dict_sort, '官方网站' AS dict_label, 'official' AS dict_value, 'game_official_type' AS dict_type, '' AS css_class, 'primary' AS list_class, 'N' AS is_default, '0' AS status, 'admin' AS create_by, sysdate() AS create_time, '官方/发行商官网' AS remark
  UNION ALL SELECT 2, '官方商店', 'store',  'game_official_type', '', 'success', 'N', '0', 'admin', sysdate(), '官方商店'
  UNION ALL SELECT 3, 'Steam',   'steam',  'game_official_type', '', 'info',    'N', '0', 'admin', sysdate(), 'Steam 商店页'
  UNION ALL SELECT 4, 'DLsite',  'dlsite', 'game_official_type', '', 'warning', 'N', '0', 'admin', sysdate(), 'DLsite'
  UNION ALL SELECT 5, '其他',    'other',  'game_official_type', '', 'info',    'N', '0', 'admin', sysdate(), '其他正版渠道'
) t WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` d WHERE d.`dict_type` = t.`dict_type` AND d.`dict_value` = t.`dict_value`);

-- 6) 资源管理按钮权限
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2017, '资源新增', 203, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'community:resource:add', '#', 'admin', sysdate(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2017);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2018, '资源修改', 203, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'community:resource:edit', '#', 'admin', sysdate(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2018);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2019, '资源删除', 203, 3, '', NULL, '', 1, 0, 'F', '0', '0', 'community:resource:remove', '#', 'admin', sysdate(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2019);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 2017), (1, 2018), (1, 2019);

-- 7) 校验
SELECT COUNT(*) AS source_table FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'gal_resource_source';
SELECT COUNT(*) AS official_table FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'gal_official_resource';
SELECT dict_type, COUNT(*) AS items FROM sys_dict_data WHERE dict_type IN ('game_resource_type', 'game_official_type') GROUP BY dict_type;
