-- ============================================================================
-- AweNovel —— Galgame 社区网站 数据库初始化脚本
-- ----------------------------------------------------------------------------
-- 版本: 3.8.7
-- 数据库: MySQL 8.0+（utf8mb4）
-- 说明: 一键建库、建表、写入初始数据。
--       Flowable 工作流表（ACT_*）由应用首次启动自动创建，无需手动导入。
--       管理员初始账号: admin / admin123（部署后请立即修改密码）
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. 创建数据库
-- ----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `awe_novel` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `awe_novel`;

-- 如使用独立数据库账号（推荐），取消下面注释并按需修改：
-- CREATE USER IF NOT EXISTS 'awenovel'@'%' IDENTIFIED BY 'your-password';
-- GRANT ALL PRIVILEGES ON `awe_novel`.* TO 'awenovel'@'%';
-- FLUSH PRIVILEGES;

SET NAMES utf8mb4;

-- ============================================================================
-- 2. 系统表（RBAC / 字典 / 配置 / 日志）
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 2.1 用户信息表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `user_id`     bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `user_name`   varchar(30)  NOT NULL                COMMENT '用户账号',
  `nick_name`   varchar(30)  NOT NULL                COMMENT '用户昵称',
  `email`       varchar(50)  DEFAULT ''              COMMENT '用户邮箱',
  `phonenumber` varchar(11)  DEFAULT ''              COMMENT '手机号码',
  `sex`         char(1)      DEFAULT '0'             COMMENT '用户性别（0男 1女 2未知）',
  `avatar`      varchar(100) DEFAULT ''              COMMENT '头像地址',
  `password`    varchar(100) DEFAULT ''              COMMENT '密码',
  `status`      char(1)      DEFAULT '0'             COMMENT '帐号状态（0正常 1停用）',
  `del_flag`    char(1)      DEFAULT '0'             COMMENT '删除标志（0代表存在 2代表删除）',
  `login_ip`    varchar(128) DEFAULT ''              COMMENT '最后登录IP',
  `login_date`  datetime                             COMMENT '最后登录时间',
  `create_by`   varchar(64)  DEFAULT ''              COMMENT '创建者',
  `create_time` datetime                             COMMENT '创建时间',
  `update_by`   varchar(64)  DEFAULT ''              COMMENT '更新者',
  `update_time` datetime                             COMMENT '更新时间',
  `remark`      varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- ----------------------------------------------------------------------------
-- 2.2 角色信息表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `role_id`             bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name`           varchar(30) NOT NULL                COMMENT '角色名称',
  `role_key`            varchar(100) NOT NULL               COMMENT '角色权限字符串',
  `role_sort`           int(4)      NOT NULL                COMMENT '显示顺序',
  `data_scope`          char(1)     DEFAULT '1'             COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  `menu_check_strictly` tinyint(1)  DEFAULT 1               COMMENT '菜单树选择项是否关联显示',
  `dept_check_strictly` tinyint(1)  DEFAULT 1               COMMENT '部门树选择项是否关联显示',
  `status`              char(1)     DEFAULT '0'             COMMENT '角色状态（0正常 1停用）',
  `del_flag`            char(1)     DEFAULT '0'             COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by`           varchar(64) DEFAULT ''              COMMENT '创建者',
  `create_time`         datetime                            COMMENT '创建时间',
  `update_by`           varchar(64) DEFAULT ''              COMMENT '更新者',
  `update_time`         datetime                            COMMENT '更新时间',
  `remark`              varchar(500) DEFAULT NULL           COMMENT '备注',
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='角色信息表';

-- ----------------------------------------------------------------------------
-- 2.3 菜单权限表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `menu_id`     bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name`   varchar(50) NOT NULL                COMMENT '菜单名称',
  `parent_id`   bigint(20)  DEFAULT 0               COMMENT '父菜单ID',
  `order_num`   int(4)      DEFAULT 0               COMMENT '显示顺序',
  `path`        varchar(200) DEFAULT ''             COMMENT '路由地址',
  `component`   varchar(255) DEFAULT NULL           COMMENT '组件路径',
  `query`       varchar(255) DEFAULT NULL           COMMENT '路由参数',
  `is_frame`    int(1)      DEFAULT 1               COMMENT '是否为外链（0是 1否）',
  `is_cache`    int(1)      DEFAULT 0               COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type`   char(1)     DEFAULT ''              COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible`     char(1)     DEFAULT '0'             COMMENT '菜单状态（0显示 1隐藏）',
  `status`      char(1)     DEFAULT '0'             COMMENT '菜单状态（0正常 1停用）',
  `perms`       varchar(100) DEFAULT NULL           COMMENT '权限标识',
  `icon`        varchar(100) DEFAULT '#'            COMMENT '菜单图标',
  `create_by`   varchar(64) DEFAULT ''              COMMENT '创建者',
  `create_time` datetime                            COMMENT '创建时间',
  `update_by`   varchar(64) DEFAULT ''              COMMENT '更新者',
  `update_time` datetime                            COMMENT '更新时间',
  `remark`      varchar(500) DEFAULT ''             COMMENT '备注',
  PRIMARY KEY (`menu_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2004 DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

-- ----------------------------------------------------------------------------
-- 2.4 用户-角色关联表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户和角色关联表';

-- ----------------------------------------------------------------------------
-- 2.5 角色-菜单关联表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色和菜单关联表';

-- ----------------------------------------------------------------------------
-- 2.6 字典类型表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `dict_id`     bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `dict_name`   varchar(100) DEFAULT ''              COMMENT '字典名称',
  `dict_type`   varchar(100) DEFAULT ''              COMMENT '字典类型',
  `status`      char(1)     DEFAULT '0'              COMMENT '状态（0正常 1停用）',
  `create_by`   varchar(64) DEFAULT ''               COMMENT '创建者',
  `create_time` datetime                             COMMENT '创建时间',
  `update_by`   varchar(64) DEFAULT ''               COMMENT '更新者',
  `update_time` datetime                             COMMENT '更新时间',
  `remark`      varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `dict_type` (`dict_type`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

-- ----------------------------------------------------------------------------
-- 2.7 字典数据表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `dict_code`   bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `dict_sort`   int(4)      DEFAULT 0                COMMENT '字典排序',
  `dict_label`  varchar(100) DEFAULT ''              COMMENT '字典标签',
  `dict_value`  varchar(100) DEFAULT ''              COMMENT '字典键值',
  `dict_type`   varchar(100) DEFAULT ''              COMMENT '字典类型',
  `css_class`   varchar(100) DEFAULT NULL            COMMENT '样式属性（其他样式扩展）',
  `list_class`  varchar(100) DEFAULT NULL            COMMENT '表格回显样式',
  `is_default`  char(1)     DEFAULT 'N'              COMMENT '是否默认（Y是 N否）',
  `status`      char(1)     DEFAULT '0'              COMMENT '状态（0正常 1停用）',
  `create_by`   varchar(64) DEFAULT ''               COMMENT '创建者',
  `create_time` datetime                             COMMENT '创建时间',
  `update_by`   varchar(64) DEFAULT ''               COMMENT '更新者',
  `update_time` datetime                             COMMENT '更新时间',
  `remark`      varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`dict_code`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

-- ----------------------------------------------------------------------------
-- 2.8 参数配置表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `config_id`    bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `config_name`  varchar(100) DEFAULT ''               COMMENT '参数名称',
  `config_key`   varchar(100) DEFAULT ''               COMMENT '参数键名',
  `config_value` varchar(500) DEFAULT ''               COMMENT '参数键值',
  `config_type`  char(1)      DEFAULT 'N'              COMMENT '系统内置（Y是 N否）',
  `create_by`    varchar(64)  DEFAULT ''               COMMENT '创建者',
  `create_time`  datetime                              COMMENT '创建时间',
  `update_by`    varchar(64)  DEFAULT ''               COMMENT '更新者',
  `update_time`  datetime                              COMMENT '更新时间',
  `remark`       varchar(500) DEFAULT NULL             COMMENT '备注',
  PRIMARY KEY (`config_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='参数配置表';

-- ----------------------------------------------------------------------------
-- 2.9 通知公告表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
  `notice_id`      bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `notice_title`   varchar(50)  NOT NULL                COMMENT '公告标题',
  `notice_type`    char(1)      NOT NULL                COMMENT '公告类型（1通知 2公告）',
  `notice_content` longblob                              COMMENT '公告内容',
  `status`         char(1)      DEFAULT '0'             COMMENT '公告状态（0正常 1关闭）',
  `create_by`      varchar(64)  DEFAULT ''              COMMENT '创建者',
  `create_time`    datetime                             COMMENT '创建时间',
  `update_by`      varchar(64)  DEFAULT ''              COMMENT '更新者',
  `update_time`    datetime                             COMMENT '更新时间',
  `remark`         varchar(255) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='通知公告表';

-- ----------------------------------------------------------------------------
-- 2.10 操作日志记录表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
  `oper_id`        bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `title`          varchar(50)  DEFAULT ''              COMMENT '模块标题',
  `business_type`  int(2)       DEFAULT 0               COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method`         varchar(100) DEFAULT ''              COMMENT '方法名称',
  `request_method` varchar(10)  DEFAULT ''              COMMENT '请求方式',
  `operator_type`  int(1)       DEFAULT 0               COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name`      varchar(50)  DEFAULT ''              COMMENT '操作人员',
  `dept_name`      varchar(50)  DEFAULT ''              COMMENT '部门名称',
  `oper_url`       varchar(255) DEFAULT ''              COMMENT '请求URL',
  `oper_ip`        varchar(128) DEFAULT ''              COMMENT '主机地址',
  `oper_location`  varchar(255) DEFAULT ''              COMMENT '操作地点',
  `oper_param`     varchar(2000) DEFAULT ''             COMMENT '请求参数',
  `json_result`    varchar(2000) DEFAULT ''             COMMENT '返回参数',
  `status`         int(1)       DEFAULT 0               COMMENT '操作状态（0正常 1异常）',
  `error_msg`      varchar(2000) DEFAULT ''             COMMENT '错误消息',
  `oper_time`      datetime                             COMMENT '操作时间',
  `cost_time`      bigint(20)   DEFAULT 0               COMMENT '消耗时间',
  PRIMARY KEY (`oper_id`),
  KEY `idx_sys_oper_log_bt` (`business_type`),
  KEY `idx_sys_oper_log_s` (`status`),
  KEY `idx_sys_oper_log_ot` (`oper_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='操作日志记录';

-- ----------------------------------------------------------------------------
-- 2.11 登录日志表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_logininfor`;
CREATE TABLE `sys_logininfor` (
  `info_id`        bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `user_name`      varchar(50)  DEFAULT ''              COMMENT '用户账号',
  `ipaddr`         varchar(128) DEFAULT ''              COMMENT '登录IP地址',
  `login_location` varchar(255) DEFAULT ''              COMMENT '登录地点',
  `browser`        varchar(50)  DEFAULT ''              COMMENT '浏览器类型',
  `os`             varchar(50)  DEFAULT ''              COMMENT '操作系统',
  `status`         char(1)      DEFAULT '0'             COMMENT '登录状态（0成功 1失败）',
  `msg`            varchar(255) DEFAULT ''              COMMENT '提示消息',
  `login_time`     datetime                             COMMENT '访问时间',
  PRIMARY KEY (`info_id`),
  KEY `idx_sys_logininfor_s` (`status`),
  KEY `idx_sys_logininfor_lt` (`login_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='系统访问记录';

-- ----------------------------------------------------------------------------
-- 2.12 在线用户记录表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user_online`;
CREATE TABLE `sys_user_online` (
  `sessionId`         varchar(50)  DEFAULT '' COMMENT '用户会话id',
  `login_name`        varchar(50)  DEFAULT '' COMMENT '登录账号',
  `dept_name`         varchar(50)  DEFAULT '' COMMENT '部门名称',
  `ipaddr`            varchar(128) DEFAULT '' COMMENT '登录IP地址',
  `login_location`    varchar(255) DEFAULT '' COMMENT '登录地点',
  `browser`           varchar(50)  DEFAULT '' COMMENT '浏览器类型',
  `os`                varchar(50)  DEFAULT '' COMMENT '操作系统',
  `status`            varchar(10)  DEFAULT '' COMMENT '在线状态（on_line在线 off_line离线）',
  `start_timestamp`   datetime     DEFAULT NULL COMMENT 'session创建时间',
  `last_access_time`  datetime     DEFAULT NULL COMMENT 'session最后访问时间',
  `expire_time`       int(20)      DEFAULT 0 COMMENT '超时时间，单位为分钟',
  PRIMARY KEY (`sessionId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='在线用户记录';

-- ============================================================================
-- 3. 社区业务表（gal_*）
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 3.1 制作会社（品牌）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `gal_brand`;
CREATE TABLE `gal_brand` (
  `brand_id`    bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '会社ID',
  `name`        varchar(100) NOT NULL                COMMENT '会社名称',
  `logo`        varchar(255) DEFAULT ''              COMMENT 'Logo',
  `description` text                                 COMMENT '会社简介',
  `del_flag`    char(1)      DEFAULT '0'             COMMENT '删除标志（0存在 2删除）',
  `create_time` datetime                             COMMENT '创建时间',
  `update_time` datetime                             COMMENT '更新时间',
  PRIMARY KEY (`brand_id`),
  KEY `idx_gal_brand_del` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='制作会社表';

-- ----------------------------------------------------------------------------
-- 3.2 游戏条目
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `gal_game`;
CREATE TABLE `gal_game` (
  `game_id`       bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '游戏ID',
  `brand_id`      bigint(20)    DEFAULT NULL            COMMENT '制作会社ID',
  `title`         varchar(255)  NOT NULL                COMMENT '游戏原名',
  `title_cn`      varchar(255)  DEFAULT ''              COMMENT '游戏译名',
  `cover`         varchar(255)  DEFAULT ''              COMMENT '封面图',
  `release_date`  date                                  COMMENT '发售日期',
  `summary`       text                                  COMMENT '简介',
  `staff_paint`   varchar(500)  DEFAULT ''              COMMENT '原画',
  `staff_scenario` varchar(500) DEFAULT ''              COMMENT '剧本',
  `staff_voice`   varchar(500)  DEFAULT ''              COMMENT '主要声优',
  `rating_avg`    decimal(3,1)  DEFAULT NULL            COMMENT '平均评分（冗余）',
  `rating_count`  int(11)       DEFAULT 0               COMMENT '评分人数（冗余）',
  `view_count`    int(11)       DEFAULT 0               COMMENT '浏览量',
  `status`        char(1)       DEFAULT '1'             COMMENT '状态（1上架 0下架）',
  `del_flag`      char(1)       DEFAULT '0'             COMMENT '删除标志（0存在 2删除）',
  `create_time`   datetime                              COMMENT '创建时间',
  `update_time`   datetime                              COMMENT '更新时间',
  PRIMARY KEY (`game_id`),
  KEY `idx_gal_game_status` (`status`),
  KEY `idx_gal_game_brand` (`brand_id`),
  KEY `idx_gal_game_title` (`title`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='游戏条目表';

-- ----------------------------------------------------------------------------
-- 3.3 标签
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `gal_tag`;
CREATE TABLE `gal_tag` (
  `tag_id`      bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name`        varchar(50) NOT NULL                COMMENT '标签名称',
  `type`        varchar(20) DEFAULT 'theme'         COMMENT '标签类型（theme题材/play玩法/type类型）',
  `create_time` datetime                            COMMENT '创建时间',
  PRIMARY KEY (`tag_id`),
  UNIQUE KEY `uk_gal_tag_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

-- ----------------------------------------------------------------------------
-- 3.4 游戏-标签关联表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `gal_game_tag`;
CREATE TABLE `gal_game_tag` (
  `game_id` bigint(20) NOT NULL COMMENT '游戏ID',
  `tag_id`  bigint(20) NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`game_id`, `tag_id`),
  KEY `idx_gal_game_tag_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏和标签关联表';

-- ----------------------------------------------------------------------------
-- 3.5 文章（资讯/评测/攻略）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `gal_article`;
CREATE TABLE `gal_article` (
  `article_id`         bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `user_id`            bigint(20)   NOT NULL                COMMENT '作者ID',
  `title`              varchar(200) NOT NULL                COMMENT '标题',
  `summary`            varchar(500) DEFAULT ''              COMMENT '简介',
  `content`            longtext                             COMMENT '内容',
  `cover`              varchar(255) DEFAULT ''              COMMENT '封面图',
  `category`           varchar(20)  DEFAULT 'news'          COMMENT '分类（news资讯/review评测/guide攻略）',
  `status`             char(1)      DEFAULT '0'             COMMENT '审核状态（0待审核 1通过 2拒绝）',
  `process_instance_id` varchar(64) DEFAULT ''              COMMENT 'Flowable流程实例ID',
  `view_count`         int(11)      DEFAULT 0               COMMENT '浏览量',
  `like_count`         int(11)      DEFAULT 0               COMMENT '点赞数',
  `comment_count`      int(11)      DEFAULT 0               COMMENT '评论数',
  `del_flag`           char(1)      DEFAULT '0'             COMMENT '删除标志（0存在 2删除）',
  `create_time`        datetime                             COMMENT '创建时间',
  `update_time`        datetime                             COMMENT '更新时间',
  PRIMARY KEY (`article_id`),
  KEY `idx_gal_article_status` (`status`),
  KEY `idx_gal_article_user` (`user_id`),
  KEY `idx_gal_article_category` (`category`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='文章表';

-- ----------------------------------------------------------------------------
-- 3.6 资源（下载内容）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `gal_resource`;
CREATE TABLE `gal_resource` (
  `resource_id`        bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '资源ID',
  `game_id`            bigint(20)   NOT NULL                COMMENT '游戏ID',
  `user_id`            bigint(20)   NOT NULL                COMMENT '发布者ID',
  `title`              varchar(200) NOT NULL                COMMENT '资源标题',
  `type`               varchar(20)  DEFAULT 'netdisk'       COMMENT '类型（netdisk网盘/magnet磁力/torrent种子）',
  `url`                text                                 COMMENT '链接',
  `version`            varchar(50)  DEFAULT ''              COMMENT '版本',
  `size`               varchar(50)  DEFAULT ''              COMMENT '大小',
  `extract_pwd`        varchar(50)  DEFAULT ''              COMMENT '解压密码',
  `checksum`           varchar(64)  DEFAULT ''              COMMENT '校验码',
  `points`             int(11)      DEFAULT 0               COMMENT '下载所需积分',
  `download_count`     int(11)      DEFAULT 0               COMMENT '下载次数',
  `report_count`       int(11)      DEFAULT 0               COMMENT '失效举报数',
  `status`             char(1)      DEFAULT '0'             COMMENT '审核状态（0待审核 1通过 2拒绝）',
  `process_instance_id` varchar(64) DEFAULT ''              COMMENT 'Flowable流程实例ID',
  `del_flag`           char(1)      DEFAULT '0'             COMMENT '删除标志（0存在 2删除）',
  `create_time`        datetime                             COMMENT '创建时间',
  `update_time`        datetime                             COMMENT '更新时间',
  PRIMARY KEY (`resource_id`),
  KEY `idx_gal_resource_game` (`game_id`),
  KEY `idx_gal_resource_status` (`status`),
  KEY `idx_gal_resource_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='资源表';

-- ----------------------------------------------------------------------------
-- 3.7 评论
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `gal_comment`;
CREATE TABLE `gal_comment` (
  `comment_id`  bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `target_type` char(1)       NOT NULL                COMMENT '目标类型（G游戏 A文章 R资源）',
  `target_id`   bigint(20)    NOT NULL                COMMENT '目标ID',
  `user_id`     bigint(20)    NOT NULL                COMMENT '用户ID',
  `pid`         bigint(20)    DEFAULT 0               COMMENT '父评论ID（0为顶层）',
  `content`     varchar(1000) NOT NULL                COMMENT '评论内容',
  `like_count`  int(11)       DEFAULT 0               COMMENT '点赞数',
  `status`      char(1)       DEFAULT '1'             COMMENT '状态（1正常 0隐藏）',
  `del_flag`    char(1)       DEFAULT '0'             COMMENT '删除标志（0存在 2删除）',
  `create_time` datetime                              COMMENT '创建时间',
  `update_time` datetime                              COMMENT '更新时间',
  PRIMARY KEY (`comment_id`),
  KEY `idx_gal_comment_target` (`target_type`, `target_id`),
  KEY `idx_gal_comment_user` (`user_id`),
  KEY `idx_gal_comment_pid` (`pid`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- ----------------------------------------------------------------------------
-- 3.8 评分
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `gal_rating`;
CREATE TABLE `gal_rating` (
  `rating_id`   bigint(20) NOT NULL AUTO_INCREMENT COMMENT '评分ID',
  `user_id`     bigint(20) NOT NULL                COMMENT '用户ID',
  `game_id`     bigint(20) NOT NULL                COMMENT '游戏ID',
  `score`       int(11)    NOT NULL                COMMENT '评分（1-10）',
  `create_time` datetime                           COMMENT '创建时间',
  PRIMARY KEY (`rating_id`),
  UNIQUE KEY `uk_gal_rating_user_game` (`user_id`, `game_id`),
  KEY `idx_gal_rating_game` (`game_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='评分表';

-- ----------------------------------------------------------------------------
-- 3.9 签到记录
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `gal_sign_record`;
CREATE TABLE `gal_sign_record` (
  `id`               bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id`          bigint(20) NOT NULL                COMMENT '用户ID',
  `sign_date`        date       NOT NULL                COMMENT '签到日期',
  `continuous_days`  int(11)    DEFAULT 1               COMMENT '连续签到天数',
  `points_award`     int(11)    DEFAULT 0               COMMENT '积分奖励',
  `exp_award`        int(11)    DEFAULT 0               COMMENT '经验奖励',
  `create_time`      datetime                           COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_gal_sign_user_date` (`user_id`, `sign_date`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='签到记录表';

-- ----------------------------------------------------------------------------
-- 3.10 积分流水（不可变）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `gal_point_log`;
CREATE TABLE `gal_point_log` (
  `log_id`        bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `user_id`       bigint(20)  NOT NULL                COMMENT '用户ID',
  `change_type`   varchar(20) NOT NULL                COMMENT '变化类型（signin/register/download/publish/comment/rating/ai_chat/admin）',
  `change_amount` int(11)     NOT NULL                COMMENT '变化量（正加负减）',
  `balance_after` bigint(20)  DEFAULT 0               COMMENT '变化后余额',
  `biz_type`      varchar(50) DEFAULT ''              COMMENT '业务类型',
  `biz_id`        bigint(20)  DEFAULT NULL            COMMENT '业务ID',
  `remark`        varchar(255) DEFAULT ''             COMMENT '备注',
  `create_time`   datetime                            COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_gal_point_log_user` (`user_id`),
  KEY `idx_gal_point_log_type` (`change_type`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='积分流水表';

-- ----------------------------------------------------------------------------
-- 3.11 用户社区画像（积分/经验/等级）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `gal_user_profile`;
CREATE TABLE `gal_user_profile` (
  `user_id`     bigint(20) NOT NULL                COMMENT '用户ID（关联sys_user.user_id）',
  `points`      bigint(20) DEFAULT 0               COMMENT '积分余额',
  `exp`         bigint(20) DEFAULT 0               COMMENT '累计经验',
  `level`       int(11)    DEFAULT 0               COMMENT '等级（0-6）',
  `sign_streak` int(11)    DEFAULT 0               COMMENT '连续签到天数',
  `create_time` datetime                           COMMENT '创建时间',
  `update_time` datetime                           COMMENT '更新时间',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户社区画像表';

-- ----------------------------------------------------------------------------
-- 3.12 用户收藏
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `gal_user_favorite`;
CREATE TABLE `gal_user_favorite` (
  `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id`     bigint(20) NOT NULL                COMMENT '用户ID',
  `target_type` char(1)    NOT NULL                COMMENT '目标类型（G游戏 A文章 R资源）',
  `target_id`   bigint(20) NOT NULL                COMMENT '目标ID',
  `create_time` datetime                           COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_gal_fav_user_target` (`user_id`, `target_type`, `target_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

-- ----------------------------------------------------------------------------
-- 3.13 用户关注
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `gal_user_follow`;
CREATE TABLE `gal_user_follow` (
  `id`             bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id`        bigint(20) NOT NULL                COMMENT '用户ID（关注者）',
  `follow_user_id` bigint(20) NOT NULL                COMMENT '被关注用户ID',
  `create_time`    datetime                           COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_gal_follow_user` (`user_id`, `follow_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='用户关注表';

-- ----------------------------------------------------------------------------
-- 3.14 用户点赞
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `gal_user_like`;
CREATE TABLE `gal_user_like` (
  `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id`     bigint(20) NOT NULL                COMMENT '用户ID',
  `target_type` char(1)    NOT NULL                COMMENT '目标类型（G游戏 A文章 R资源 C评论）',
  `target_id`   bigint(20) NOT NULL                COMMENT '目标ID',
  `create_time` datetime                           COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_gal_like_user_target` (`user_id`, `target_type`, `target_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='用户点赞表';

-- ----------------------------------------------------------------------------
-- 3.15 站内消息
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `gal_message`;
CREATE TABLE `gal_message` (
  `message_id` bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `user_id`    bigint(20)  NOT NULL                COMMENT '接收者用户ID',
  `sender_id`  bigint(20)  DEFAULT NULL            COMMENT '发送者用户ID（系统消息为空）',
  `type`       varchar(20) DEFAULT 'system'        COMMENT '消息类型（comment/like/audit/system）',
  `content`    varchar(500) NOT NULL               COMMENT '消息内容',
  `is_read`    char(1)     DEFAULT '0'             COMMENT '是否已读（0未读 1已读）',
  `create_time` datetime                           COMMENT '创建时间',
  PRIMARY KEY (`message_id`),
  KEY `idx_gal_message_user` (`user_id`),
  KEY `idx_gal_message_read` (`is_read`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';

-- ----------------------------------------------------------------------------
-- 3.16 等级配置（B站式 0-6 级）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `gal_level_config`;
CREATE TABLE `gal_level_config` (
  `level`   int(11)    NOT NULL COMMENT '等级（0-6）',
  `name`    varchar(20) DEFAULT '' COMMENT '等级名称',
  `min_exp` bigint(20) DEFAULT 0  COMMENT '升级所需累计经验',
  PRIMARY KEY (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='等级配置表';

-- ============================================================================
-- 4. 初始数据
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 4.1 用户：管理员 admin / admin123
-- ----------------------------------------------------------------------------
INSERT INTO `sys_user` VALUES
(1, 'admin', 'AweNovel管理员', '', '', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', '2024-01-01 00:00:00', 'admin', '2024-01-01 00:00:00', '', NULL, '管理员');

-- ----------------------------------------------------------------------------
-- 4.2 角色
-- ----------------------------------------------------------------------------
INSERT INTO `sys_role` VALUES
(1, '超级管理员', 'admin',  1, 1, 1, 1, '0', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '超级管理员'),
(2, '普通角色',   'common', 2, 2, 1, 1, '0', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '普通角色');

-- ----------------------------------------------------------------------------
-- 4.3 菜单
-- ----------------------------------------------------------------------------
INSERT INTO `sys_menu` VALUES
-- 系统管理（目录）
(1,   '系统管理', 0,    1, 'system',          NULL,            '', 1, 0, 'M', '0', '0', '',                'system',          'admin', '2024-01-01 00:00:00', '', NULL, '系统管理目录'),
(100, '用户管理', 1,    1, 'user',            'system/user/index', '', 1, 0, 'C', '0', '0', 'system:user:list',    'user',            'admin', '2024-01-01 00:00:00', '', NULL, '用户管理菜单'),
(101, '角色管理', 1,    2, 'role',            'system/role/index', '', 1, 0, 'C', '0', '0', 'system:role:list',    'peoples',         'admin', '2024-01-01 00:00:00', '', NULL, '角色管理菜单'),
(102, '菜单管理', 1,    3, 'menu',            'system/menu/index', '', 1, 0, 'C', '0', '0', 'system:menu:list',    'tree-table',      'admin', '2024-01-01 00:00:00', '', NULL, '菜单管理菜单'),
(103, '字典管理', 1,    4, 'dict',            'system/dict/index', '', 1, 0, 'C', '0', '0', 'system:dict:list',    'dict',            'admin', '2024-01-01 00:00:00', '', NULL, '字典管理菜单'),
(104, '参数设置', 1,    5, 'config',          'system/config/index', '', 1, 0, 'C', '0', '0', 'system:config:list',  'edit',            'admin', '2024-01-01 00:00:00', '', NULL, '参数设置菜单'),
(105, '通知公告', 1,    6, 'notice',          'system/notice/index', '', 1, 0, 'C', '0', '0', 'system:notice:list',  'message',         'admin', '2024-01-01 00:00:00', '', NULL, '通知公告菜单'),
(106, '日志管理', 1,    7, 'log',             NULL,            '', 1, 0, 'M', '0', '0', '',                    'log',             'admin', '2024-01-01 00:00:00', '', NULL, '日志管理目录'),
(107, '操作日志', 106,  1, 'operlog',         'monitor/operlog/index', '', 1, 0, 'C', '0', '0', 'monitor:operlog:list', 'form',            'admin', '2024-01-01 00:00:00', '', NULL, '操作日志菜单'),
(108, '登录日志', 106,  2, 'logininfor',      'monitor/logininfor/index', '', 1, 0, 'C', '0', '0', 'monitor:logininfor:list', 'logininfor',     'admin', '2024-01-01 00:00:00', '', NULL, '登录日志菜单'),
(109, '在线用户', 106,  3, 'online',          'monitor/online/index', '', 1, 0, 'C', '0', '0', 'monitor:online:list',    'online',         'admin', '2024-01-01 00:00:00', '', NULL, '在线用户菜单'),
-- 系统管理-用户管理按钮
(1000, '用户查询', 100, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'system:user:query',  '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
(1001, '用户新增', 100, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'system:user:add',    '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
(1002, '用户修改', 100, 3, '', NULL, '', 1, 0, 'F', '0', '0', 'system:user:edit',   '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
(1003, '用户删除', 100, 4, '', NULL, '', 1, 0, 'F', '0', '0', 'system:user:remove', '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
(1004, '重置密码', 100, 5, '', NULL, '', 1, 0, 'F', '0', '0', 'system:user:resetPwd', '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
-- 系统管理-角色管理按钮
(1010, '角色查询', 101, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'system:role:query',  '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
(1011, '角色新增', 101, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'system:role:add',    '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
(1012, '角色修改', 101, 3, '', NULL, '', 1, 0, 'F', '0', '0', 'system:role:edit',   '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
(1013, '角色删除', 101, 4, '', NULL, '', 1, 0, 'F', '0', '0', 'system:role:remove', '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
-- 系统管理-菜单管理按钮
(1020, '菜单查询', 102, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'system:menu:query',  '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
(1021, '菜单新增', 102, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'system:menu:add',    '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
(1022, '菜单修改', 102, 3, '', NULL, '', 1, 0, 'F', '0', '0', 'system:menu:edit',   '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
(1023, '菜单删除', 102, 4, '', NULL, '', 1, 0, 'F', '0', '0', 'system:menu:remove', '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
-- 系统管理-字典管理按钮
(1030, '字典查询', 103, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'system:dict:query',  '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
(1031, '字典新增', 103, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'system:dict:add',    '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
(1032, '字典修改', 103, 3, '', NULL, '', 1, 0, 'F', '0', '0', 'system:dict:edit',   '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
(1033, '字典删除', 103, 4, '', NULL, '', 1, 0, 'F', '0', '0', 'system:dict:remove', '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
-- 系统管理-参数设置按钮
(1040, '参数查询', 104, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'system:config:query',  '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
(1041, '参数新增', 104, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'system:config:add',    '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
(1042, '参数修改', 104, 3, '', NULL, '', 1, 0, 'F', '0', '0', 'system:config:edit',   '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
(1043, '参数删除', 104, 4, '', NULL, '', 1, 0, 'F', '0', '0', 'system:config:remove', '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
-- 系统管理-通知公告按钮
(1050, '公告查询', 105, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'system:notice:query',  '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
(1051, '公告新增', 105, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'system:notice:add',    '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
(1052, '公告修改', 105, 3, '', NULL, '', 1, 0, 'F', '0', '0', 'system:notice:edit',   '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
(1053, '公告删除', 105, 4, '', NULL, '', 1, 0, 'F', '0', '0', 'system:notice:remove', '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
-- 社区管理（目录 + 内容审核）
(2000, '社区管理', 0,   2, 'community',      NULL,            '', 1, 0, 'M', '0', '0', '',                    'star',            'admin', '2024-01-01 00:00:00', '', NULL, '社区管理目录'),
(2001, '内容审核', 2000, 1, 'review',        'community/review/index', '', 1, 0, 'C', '0', '0', 'community:review:list', 'finished',      'admin', '2024-01-01 00:00:00', '', NULL, '内容审核菜单'),
(2002, '审核通过', 2001, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'community:review:edit', '#', 'admin', '2024-01-01 00:00:00', '', NULL, ''),
(2003, '审核拒绝', 2001, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'community:review:edit', '#', 'admin', '2024-01-01 00:00:00', '', NULL, '');

-- ----------------------------------------------------------------------------
-- 4.4 用户-角色 / 角色-菜单 关联
-- ----------------------------------------------------------------------------
INSERT INTO `sys_user_role` VALUES (1, 1);

INSERT INTO `sys_role_menu` VALUES
(1, 1), (1, 100), (1, 101), (1, 102), (1, 103), (1, 104), (1, 105), (1, 106), (1, 107), (1, 108), (1, 109),
(1, 1000), (1, 1001), (1, 1002), (1, 1003), (1, 1004),
(1, 1010), (1, 1011), (1, 1012), (1, 1013),
(1, 1020), (1, 1021), (1, 1022), (1, 1023),
(1, 1030), (1, 1031), (1, 1032), (1, 1033),
(1, 1040), (1, 1041), (1, 1042), (1, 1043),
(1, 1050), (1, 1051), (1, 1052), (1, 1053),
(1, 2000), (1, 2001), (1, 2002), (1, 2003),
(2, 2000), (2, 2001);

-- ----------------------------------------------------------------------------
-- 4.5 字典类型
-- ----------------------------------------------------------------------------
INSERT INTO `sys_dict_type` VALUES
(1,  '用户性别', 'sys_user_sex',    '0', 'admin', '2024-01-01 00:00:00', '', NULL, '用户性别列表'),
(2,  '菜单状态', 'sys_show_hide',   '0', 'admin', '2024-01-01 00:00:00', '', NULL, '菜单状态列表'),
(3,  '系统开关', 'sys_normal_disable', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '系统开关列表'),
(4,  '任务状态', 'sys_job_status',  '0', 'admin', '2024-01-01 00:00:00', '', NULL, '任务状态列表'),
(5,  '系统是否', 'sys_yes_no',      '0', 'admin', '2024-01-01 00:00:00', '', NULL, '系统是否列表'),
(6,  '通知类型', 'sys_notice_type', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '通知类型列表'),
(7,  '通知状态', 'sys_notice_status', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '通知状态列表'),
(8,  '操作类型', 'sys_oper_type',   '0', 'admin', '2024-01-01 00:00:00', '', NULL, '操作类型列表'),
(9,  '系统状态', 'sys_common_status', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '登录状态列表');

-- ----------------------------------------------------------------------------
-- 4.6 字典数据
-- ----------------------------------------------------------------------------
INSERT INTO `sys_dict_data` VALUES
-- 用户性别
(1,  1, '男', '0', 'sys_user_sex', '', '', 'Y', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '性别男'),
(2,  2, '女', '1', 'sys_user_sex', '', '', 'N', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '性别女'),
(3,  3, '未知', '2', 'sys_user_sex', '', '', 'N', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '性别未知'),
-- 菜单状态
(4,  1, '显示', '0', 'sys_show_hide', '', 'primary', 'Y', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '显示菜单'),
(5,  2, '隐藏', '1', 'sys_show_hide', '', 'danger',  'N', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '隐藏菜单'),
-- 系统开关
(6,  1, '正常', '0', 'sys_normal_disable', '', 'primary', 'Y', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '正常状态'),
(7,  2, '停用', '1', 'sys_normal_disable', '', 'danger',  'N', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '停用状态'),
-- 系统是否
(8,  1, '是', 'Y', 'sys_yes_no', '', 'primary', 'Y', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '系统默认是'),
(9,  2, '否', 'N', 'sys_yes_no', '', 'danger',  'N', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '系统默认否'),
-- 通知类型
(10, 1, '通知', '1', 'sys_notice_type', '', 'warning', 'Y', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '通知'),
(11, 2, '公告', '2', 'sys_notice_type', '', 'success', 'N', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '公告'),
-- 通知状态
(12, 1, '正常', '0', 'sys_notice_status', '', 'primary', 'Y', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '正常'),
(13, 2, '关闭', '1', 'sys_notice_status', '', 'danger',  'N', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '关闭'),
-- 操作类型
(14, 1, '新增', '1', 'sys_oper_type', '', 'info',    'N', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '新增操作'),
(15, 2, '修改', '2', 'sys_oper_type', '', 'info',    'N', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '修改操作'),
(16, 3, '删除', '3', 'sys_oper_type', '', 'danger',  'N', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '删除操作'),
(17, 4, '授权', '4', 'sys_oper_type', '', 'primary', 'N', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '授权操作'),
(18, 5, '导出', '5', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '导出操作'),
(19, 6, '导入', '6', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '导入操作'),
(20, 7, '强退', '7', 'sys_oper_type', '', 'danger',  'N', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '强退操作'),
(21, 8, '清空数据', '9', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '清空数据操作'),
-- 系统状态（登录）
(22, 1, '成功', '0', 'sys_common_status', '', 'primary', 'N', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '成功状态'),
(23, 2, '失败', '1', 'sys_common_status', '', 'danger',  'N', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '失败状态');

-- ----------------------------------------------------------------------------
-- 4.7 系统参数配置
-- ----------------------------------------------------------------------------
INSERT INTO `sys_config` VALUES
(1,  '主框架页-默认皮肤样式名称', 'sys.index.skinName',        'skin-blue',     'Y', 'admin', '2024-01-01 00:00:00', '', NULL, '默认皮肤样式'),
(2,  '用户管理-账号初始密码',     'sys.user.initPassword',     '123456',        'Y', 'admin', '2024-01-01 00:00:00', '', NULL, '初始化密码 123456'),
(3,  '主框架页-侧边栏主题',       'sys.index.sideTheme',       'theme-dark',    'Y', 'admin', '2024-01-01 00:00:00', '', NULL, '侧边栏主题'),
(4,  '账号自助-验证码开关',       'sys.account.captchaEnabled', 'true',          'Y', 'admin', '2024-01-01 00:00:00', '', NULL, '是否开启验证码功能'),
(5,  '账号自助-注册开关',         'sys.account.registerUser',   'true',          'Y', 'admin', '2024-01-01 00:00:00', '', NULL, '是否开启注册功能'),
(6,  '账号自助-邮箱功能开关',     'sys.account.enable.Email',   'true',          'Y', 'admin', '2024-01-01 00:00:00', '', NULL, '是否开启邮箱验证码功能'),
(7,  '用户登录-黑名单列表',       'sys.login.blackIPList',      '',              'Y', 'admin', '2024-01-01 00:00:00', '', NULL, '设置登录IP黑名单，多个用逗号分隔');

-- ----------------------------------------------------------------------------
-- 4.8 通知公告
-- ----------------------------------------------------------------------------
INSERT INTO `sys_notice` VALUES
(1, '欢迎来到 AweNovel', '2', '欢迎使用 AweNovel —— Galgame 社区网站。本站内容仅供学习交流，请支持正版。', '0', 'admin', '2024-01-01 00:00:00', '', NULL, '系统欢迎公告');

-- ----------------------------------------------------------------------------
-- 4.9 等级配置（0-6 级，经验驱动）
-- ----------------------------------------------------------------------------
INSERT INTO `gal_level_config` (`level`, `name`, `min_exp`) VALUES
(0, '萌新',   0),
(1, '初级玩家', 100),
(2, '中级玩家', 500),
(3, '高级玩家', 1500),
(4, '资深玩家', 4000),
(5, '达人玩家', 10000),
(6, '传奇玩家', 20000);

-- ----------------------------------------------------------------------------
-- 4.10 管理员社区画像（初始积分 1000，便于体验下载/AI 等功能）
-- ----------------------------------------------------------------------------
INSERT INTO `gal_user_profile` (`user_id`, `points`, `exp`, `level`, `sign_streak`, `create_time`, `update_time`) VALUES
(1, 1000, 0, 0, 0, '2024-01-01 00:00:00', '2024-01-01 00:00:00');

-- ----------------------------------------------------------------------------
-- 4.11 示例标签（可在管理端继续维护）
-- ----------------------------------------------------------------------------
INSERT INTO `gal_tag` (`tag_id`, `name`, `type`, `create_time`) VALUES
(1, '校园',   'theme', '2024-01-01 00:00:00'),
(2, '恋爱',   'theme', '2024-01-01 00:00:00'),
(3, '悬疑',   'theme', '2024-01-01 00:00:00'),
(4, '科幻',   'theme', '2024-01-01 00:00:00'),
(5, '奇幻',   'theme', '2024-01-01 00:00:00'),
(6, '日常',   'theme', '2024-01-01 00:00:00'),
(7, '治愈',   'theme', '2024-01-01 00:00:00'),
(8, '视觉小说', 'type', '2024-01-01 00:00:00'),
(9, 'AVG',    'type',  '2024-01-01 00:00:00'),
(10, 'SLG',   'type',  '2024-01-01 00:00:00');

-- ============================================================================
-- 5. 完成提示
-- ============================================================================
-- 数据库初始化完成！
--   * 管理员账号：admin / admin123（首次登录后请立即修改密码）
--   * Flowable 工作流表（ACT_*）由应用首次启动自动创建
--   * 详细部署步骤见 doc/DEPLOYMENT.md
-- ============================================================================
