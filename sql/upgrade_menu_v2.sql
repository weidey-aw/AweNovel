-- ============================================================================
-- AweNovel 菜单结构升级脚本（管理端动态路由 v2）
-- ----------------------------------------------------------------------------
-- 目的：把 sys_menu 重建为「仪表盘 / 系统管理 / 系统监控 / 网站运营」四大模块，
--       使管理端「后端 /getRouters 驱动的动态路由」能渲染出模块化导航。
--
-- 说明：
--   * 仪表盘为前端静态路由（不在本表中），因此菜单从「系统管理」开始；
--   * component 字段与前端文件路径一一对应（如 system/user/index →
--     web-ui/admin/src/views/system/user/index.vue）；
--   * 脚本可重复执行：先清空 sys_menu 与 sys_role_menu 再重建。
--
-- 用法：mysql -uroot -p --default-character-set=utf8mb4 < sql/upgrade_menu_v2.sql
-- ============================================================================

USE `awe_novel`;

SET NAMES utf8mb4;

START TRANSACTION;

DELETE FROM `sys_role_menu`;
DELETE FROM `sys_menu`;
ALTER TABLE `sys_menu` AUTO_INCREMENT = 3002;

-- ----------------------------------------------------------------------------
-- 一级目录
-- ----------------------------------------------------------------------------
INSERT INTO `sys_menu` VALUES
(1, '系统管理', 0, 1, 'system',    NULL, '', 1, 0, 'M', '0', '0', '', 'system',    'admin', NOW(), '', NULL, '系统管理目录'),
(2, '系统监控', 0, 2, 'monitor',   NULL, '', 1, 0, 'M', '0', '0', '', 'monitor',   'admin', NOW(), '', NULL, '系统监控目录'),
(3, '网站运营', 0, 3, 'operation', NULL, '', 1, 0, 'M', '0', '0', '', 'operation', 'admin', NOW(), '', NULL, '网站运营目录'),

-- ----------------------------------------------------------------------------
-- 系统管理
-- ----------------------------------------------------------------------------
(100, '用户管理', 1, 1, 'user',   'system/user/index',   '', 1, 0, 'C', '0', '0', 'system:user:list',   'user',      'admin', NOW(), '', NULL, '用户管理菜单'),
(101, '角色管理', 1, 2, 'role',   'system/role/index',   '', 1, 0, 'C', '0', '0', 'system:role:list',   'user-filled','admin', NOW(), '', NULL, '角色管理菜单'),
(102, '菜单管理', 1, 3, 'menu',   'system/menu/index',   '', 1, 0, 'C', '0', '0', 'system:menu:list',   'menu',      'admin', NOW(), '', NULL, '菜单管理菜单'),
(103, '字典管理', 1, 4, 'dict',   'system/dict/index',   '', 1, 0, 'C', '0', '0', 'system:dict:list',   'notebook',  'admin', NOW(), '', NULL, '字典管理菜单'),
(104, '参数设置', 1, 5, 'config', 'system/config/index', '', 1, 0, 'C', '0', '0', 'system:config:list', 'edit',      'admin', NOW(), '', NULL, '参数设置菜单'),
(105, '通知管理', 1, 6, 'notice', 'system/notice/index', '', 1, 0, 'C', '0', '0', 'system:notice:list', 'message',   'admin', NOW(), '', NULL, '通知公告菜单'),
(106, '日志管理', 1, 7, 'log',    'system/log/index',    '', 1, 0, 'C', '0', '0', 'monitor:operlog:list','tickets',  'admin', NOW(), '', NULL, '操作日志与登录日志'),

-- ----------------------------------------------------------------------------
-- 系统监控
-- ----------------------------------------------------------------------------
(110, '在线用户', 2, 1, 'online', 'monitor/online/index', '', 1, 0, 'C', '0', '0', 'monitor:online:list', 'connection',  'admin', NOW(), '', NULL, '在线用户菜单'),
(111, '定时任务', 2, 2, 'job',    'monitor/job/index',    '', 1, 0, 'C', '0', '0', 'monitor:job:list',    'timer',       'admin', NOW(), '', NULL, '定时任务菜单'),
(112, '数据监控', 2, 3, 'druid',  'monitor/druid/index',  '', 1, 0, 'C', '0', '0', 'monitor:druid:list',  'trend-charts','admin', NOW(), '', NULL, 'Druid 数据监控'),
(113, '服务监控', 2, 4, 'server', 'monitor/server/index', '', 1, 0, 'C', '0', '0', 'monitor:server:list', 'cpu',         'admin', NOW(), '', NULL, '服务监控菜单'),
(114, '缓存监控', 2, 5, 'cache',  'monitor/cache/index',  '', 1, 0, 'C', '0', '0', 'monitor:cache:list',  'coin',        'admin', NOW(), '', NULL, '缓存监控菜单'),

-- ----------------------------------------------------------------------------
-- 网站运营
-- ----------------------------------------------------------------------------
(200, '游戏管理', 3, 1, 'game',     'operation/game/index',     '', 1, 0, 'C', '0', '0', 'community:game:list',     'video-play',    'admin', NOW(), '', NULL, '游戏条目管理'),
(201, '会社管理', 3, 2, 'brand',    'operation/brand/index',    '', 1, 0, 'C', '0', '0', 'community:brand:list',    'office-building','admin', NOW(), '', NULL, '制作会社管理'),
(202, '标签管理', 3, 3, 'tag',      'operation/tag/index',      '', 1, 0, 'C', '0', '0', 'community:tag:list',      'price-tag',     'admin', NOW(), '', NULL, '标签管理'),
(203, '资源管理', 3, 4, 'resource', 'operation/resource/index', '', 1, 0, 'C', '0', '0', 'community:resource:list', 'folder-opened', 'admin', NOW(), '', NULL, '下载资源与审核'),
(204, '文章管理', 3, 5, 'article',  'operation/article/index',  '', 1, 0, 'C', '0', '0', 'community:article:list',  'document',      'admin', NOW(), '', NULL, '文章与审核'),
(205, '评论管理', 3, 6, 'comment',  'operation/comment/index',  '', 1, 0, 'C', '0', '0', 'community:comment:list',  'chat-dot-round','admin', NOW(), '', NULL, '评论管理'),
(206, '内容审核', 3, 7, 'review',   'operation/review/index',   '', 1, 0, 'C', '0', '0', 'community:review:list',   'finished',      'admin', NOW(), '', NULL, 'Flowable 内容审核'),

-- ----------------------------------------------------------------------------
-- 按钮权限：系统管理
-- ----------------------------------------------------------------------------
(1000, '用户查询', 100, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'system:user:query',    '#', 'admin', NOW(), '', NULL, ''),
(1001, '用户新增', 100, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'system:user:add',      '#', 'admin', NOW(), '', NULL, ''),
(1002, '用户修改', 100, 3, '', NULL, '', 1, 0, 'F', '0', '0', 'system:user:edit',     '#', 'admin', NOW(), '', NULL, ''),
(1003, '用户删除', 100, 4, '', NULL, '', 1, 0, 'F', '0', '0', 'system:user:remove',   '#', 'admin', NOW(), '', NULL, ''),
(1004, '重置密码', 100, 5, '', NULL, '', 1, 0, 'F', '0', '0', 'system:user:resetPwd', '#', 'admin', NOW(), '', NULL, ''),
(1005, '用户导出', 100, 6, '', NULL, '', 1, 0, 'F', '0', '0', 'system:user:export',   '#', 'admin', NOW(), '', NULL, ''),
(1006, '用户导入', 100, 7, '', NULL, '', 1, 0, 'F', '0', '0', 'system:user:import',   '#', 'admin', NOW(), '', NULL, ''),
(1010, '角色查询', 101, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'system:role:query',    '#', 'admin', NOW(), '', NULL, ''),
(1011, '角色新增', 101, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'system:role:add',      '#', 'admin', NOW(), '', NULL, ''),
(1012, '角色修改', 101, 3, '', NULL, '', 1, 0, 'F', '0', '0', 'system:role:edit',     '#', 'admin', NOW(), '', NULL, ''),
(1013, '角色删除', 101, 4, '', NULL, '', 1, 0, 'F', '0', '0', 'system:role:remove',   '#', 'admin', NOW(), '', NULL, ''),
(1014, '角色导出', 101, 5, '', NULL, '', 1, 0, 'F', '0', '0', 'system:role:export',   '#', 'admin', NOW(), '', NULL, ''),
(1020, '菜单查询', 102, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'system:menu:query',    '#', 'admin', NOW(), '', NULL, ''),
(1021, '菜单新增', 102, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'system:menu:add',      '#', 'admin', NOW(), '', NULL, ''),
(1022, '菜单修改', 102, 3, '', NULL, '', 1, 0, 'F', '0', '0', 'system:menu:edit',     '#', 'admin', NOW(), '', NULL, ''),
(1023, '菜单删除', 102, 4, '', NULL, '', 1, 0, 'F', '0', '0', 'system:menu:remove',   '#', 'admin', NOW(), '', NULL, ''),
(1030, '字典查询', 103, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'system:dict:query',    '#', 'admin', NOW(), '', NULL, ''),
(1031, '字典新增', 103, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'system:dict:add',      '#', 'admin', NOW(), '', NULL, ''),
(1032, '字典修改', 103, 3, '', NULL, '', 1, 0, 'F', '0', '0', 'system:dict:edit',     '#', 'admin', NOW(), '', NULL, ''),
(1033, '字典删除', 103, 4, '', NULL, '', 1, 0, 'F', '0', '0', 'system:dict:remove',   '#', 'admin', NOW(), '', NULL, ''),
(1034, '字典导出', 103, 5, '', NULL, '', 1, 0, 'F', '0', '0', 'system:dict:export',   '#', 'admin', NOW(), '', NULL, ''),
(1040, '参数查询', 104, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'system:config:query',  '#', 'admin', NOW(), '', NULL, ''),
(1041, '参数新增', 104, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'system:config:add',    '#', 'admin', NOW(), '', NULL, ''),
(1042, '参数修改', 104, 3, '', NULL, '', 1, 0, 'F', '0', '0', 'system:config:edit',   '#', 'admin', NOW(), '', NULL, ''),
(1043, '参数删除', 104, 4, '', NULL, '', 1, 0, 'F', '0', '0', 'system:config:remove', '#', 'admin', NOW(), '', NULL, ''),
(1044, '参数导出', 104, 5, '', NULL, '', 1, 0, 'F', '0', '0', 'system:config:export', '#', 'admin', NOW(), '', NULL, ''),
(1050, '公告查询', 105, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'system:notice:query',  '#', 'admin', NOW(), '', NULL, ''),
(1051, '公告新增', 105, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'system:notice:add',    '#', 'admin', NOW(), '', NULL, ''),
(1052, '公告修改', 105, 3, '', NULL, '', 1, 0, 'F', '0', '0', 'system:notice:edit',   '#', 'admin', NOW(), '', NULL, ''),
(1053, '公告删除', 105, 4, '', NULL, '', 1, 0, 'F', '0', '0', 'system:notice:remove', '#', 'admin', NOW(), '', NULL, ''),
(1060, '操作日志删除', 106, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'monitor:operlog:remove',    '#', 'admin', NOW(), '', NULL, ''),
(1061, '操作日志导出', 106, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'monitor:operlog:export',    '#', 'admin', NOW(), '', NULL, ''),
(1062, '登录日志删除', 106, 3, '', NULL, '', 1, 0, 'F', '0', '0', 'monitor:logininfor:remove', '#', 'admin', NOW(), '', NULL, ''),
(1063, '登录日志导出', 106, 4, '', NULL, '', 1, 0, 'F', '0', '0', 'monitor:logininfor:export', '#', 'admin', NOW(), '', NULL, ''),
(1064, '账户解锁',     106, 5, '', NULL, '', 1, 0, 'F', '0', '0', 'monitor:logininfor:unlock', '#', 'admin', NOW(), '', NULL, ''),

-- ----------------------------------------------------------------------------
-- 按钮权限：系统监控 / 网站运营
-- ----------------------------------------------------------------------------
(1080, '强退用户', 110, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'monitor:online:forceLogout', '#', 'admin', NOW(), '', NULL, ''),
(3000, '审核通过', 206, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'community:review:edit',      '#', 'admin', NOW(), '', NULL, ''),
(3001, '审核拒绝', 206, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'community:review:edit',      '#', 'admin', NOW(), '', NULL, '');

-- ----------------------------------------------------------------------------
-- 角色菜单授权：角色1（超级管理员）拥有全部；角色2（普通角色）仅网站运营只读
-- ----------------------------------------------------------------------------
INSERT INTO `sys_role_menu` (role_id, menu_id) SELECT 1, menu_id FROM `sys_menu`;

INSERT INTO `sys_role_menu` (role_id, menu_id) VALUES
(2, 3), (2, 200), (2, 201), (2, 202), (2, 203), (2, 204), (2, 205), (2, 206);

COMMIT;

-- ----------------------------------------------------------------------------
-- 校验：查看菜单树
-- ----------------------------------------------------------------------------
SELECT m.menu_id, m.menu_name, m.parent_id, m.order_num, m.path, m.component, m.perms
FROM `sys_menu` m
WHERE m.menu_type IN ('M', 'C')
ORDER BY CASE WHEN m.parent_id = 0 THEN m.menu_id ELSE m.parent_id END, m.order_num;
