-- ============================================================================
-- AweNovel 旧库迁移脚本：将旧数据库（默认 sp_awblog）全量迁移到新库 awe_novel
-- ----------------------------------------------------------------------------
-- 适用场景：
--   应用连接地址已改为 jdbc:mysql://.../awe_novel，但旧数据仍在 sp_awblog 中，
--   MySQL 不支持 RENAME DATABASE，故本脚本逐表复制（结构 + 数据 + 索引 + 自增值）。
--
-- 特点：
--   * 动态读取源库全部基表，无需列出表名；保留 Flowable 的 ACT_*/FLW_* 表；
--   * 只读源库、只写目标库；目标表已存在时先 DROP 再重建，脚本可重复执行；
--   * 同一台 MySQL 服务器内迁移，不依赖 mysqldump / 文件传输。
--
-- 用法（修改下方 @FROM_DB / @TO_DB 后执行）：
--   mysql -uroot -p --default-character-set=utf8mb4 < sql/migrate_to_awe_novel.sql
-- ============================================================================

SET NAMES utf8mb4;

-- ============ 1. 配置：按实际库名修改 ============
SET @FROM_DB = 'sp_awblog';   -- 旧数据库（源，只读）
SET @TO_DB   = 'awe_novel';   -- 新数据库（目标，即应用当前连接地址中的库名）

-- ============ 2. 创建目标库并切换为默认库 ============
SET @sql = CONCAT('CREATE DATABASE IF NOT EXISTS `', @TO_DB,
                  '` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 切换到目标库作为默认库（USE 不支持 PREPARE，此处固定为目标库名，须与 @TO_DB 一致）
USE `awe_novel`;

-- ============ 3. 迁移存储过程 ============
DROP PROCEDURE IF EXISTS migrate_db;
DELIMITER $$
CREATE PROCEDURE migrate_db()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE tbl VARCHAR(64);
    DECLARE cur CURSOR FOR
        SELECT TABLE_NAME
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = @FROM_DB
          AND TABLE_TYPE = 'BASE TABLE'
        ORDER BY TABLE_NAME;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO tbl;
        IF done THEN LEAVE read_loop; END IF;

        -- 3.1 删除目标库同名表（保证可重复执行）
        SET @s = CONCAT('DROP TABLE IF EXISTS `', @TO_DB, '`.`', tbl, '`');
        PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

        -- 3.2 按源表结构建表（含索引、自增值）
        SET @s = CONCAT('CREATE TABLE `', @TO_DB, '`.`', tbl, '` LIKE `', @FROM_DB, '`.`', tbl, '`');
        PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

        -- 3.3 复制全部数据
        SET @s = CONCAT('INSERT INTO `', @TO_DB, '`.`', tbl, '` SELECT * FROM `', @FROM_DB, '`.`', tbl, '`');
        PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;

CALL migrate_db();
DROP PROCEDURE IF EXISTS migrate_db;

-- ============ 4. 校验：两库表数量与部分行数对比 ============
SELECT '源库表数' AS 项目, COUNT(*) AS 数值
FROM information_schema.TABLES WHERE TABLE_SCHEMA = @FROM_DB AND TABLE_TYPE = 'BASE TABLE'
UNION ALL
SELECT '目标库表数', COUNT(*)
FROM information_schema.TABLES WHERE TABLE_SCHEMA = @TO_DB AND TABLE_TYPE = 'BASE TABLE'
UNION ALL
SELECT '目标库-游戏数', (SELECT COUNT(*) FROM `awe_novel`.`gal_game`)
UNION ALL
SELECT '目标库-用户数', (SELECT COUNT(*) FROM `awe_novel`.`sys_user`)
UNION ALL
SELECT '目标库-菜单数', (SELECT COUNT(*) FROM `awe_novel`.`sys_menu`);

-- ============================================================================
-- 迁移完成。若应用仍在运行，请重启后端使连接生效：
--   systemctl restart awenovel     （systemd 部署）
--   或重新 java -jar weidey-admin.jar
-- ============================================================================
