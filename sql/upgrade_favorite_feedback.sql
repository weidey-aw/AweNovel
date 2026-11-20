-- ============================================================
-- 收藏增强 + 资源反馈
-- 可重复执行
-- ============================================================

-- 1) 站内消息补充业务关联字段（用于跳转/展示来源）
SET @has_biz_type := (SELECT COUNT(*) FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'gal_message' AND COLUMN_NAME = 'biz_type');
SET @ddl := IF(@has_biz_type = 0,
  'ALTER TABLE `gal_message` ADD COLUMN `biz_type` varchar(20) DEFAULT '''' COMMENT ''业务类型（game/article/resource/feedback）'' AFTER `type`',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_biz_id := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'gal_message' AND COLUMN_NAME = 'biz_id');
SET @ddl := IF(@has_biz_id = 0,
  'ALTER TABLE `gal_message` ADD COLUMN `biz_id` bigint(20) DEFAULT NULL COMMENT ''业务ID'' AFTER `biz_type`',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 资源反馈表
CREATE TABLE IF NOT EXISTS `gal_resource_feedback` (
  `feedback_id` bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
  `resource_id` bigint(20)   NOT NULL                COMMENT '资源ID',
  `user_id`     bigint(20)   NOT NULL                COMMENT '反馈人',
  `type`        tinyint(4)   NOT NULL                COMMENT '反馈类型（1资源失效 2无法运行 3缺少补丁 4其他）',
  `content`     varchar(500) DEFAULT ''              COMMENT '反馈内容',
  `status`      char(1)      DEFAULT '0'             COMMENT '状态（0未读 1已读）',
  `create_time` datetime                             COMMENT '创建时间',
  PRIMARY KEY (`feedback_id`),
  KEY `idx_feedback_resource` (`resource_id`),
  KEY `idx_feedback_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源反馈表';

SELECT COUNT(*) AS feedback_table FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'gal_resource_feedback';
