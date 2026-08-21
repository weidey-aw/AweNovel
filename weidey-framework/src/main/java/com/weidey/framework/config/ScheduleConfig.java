package com.weidey.framework.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 定时任务配置（替代原若依 quartz，使用 Spring @Scheduled）
 *
 * @author weidey
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {

    private static final Logger log = LoggerFactory.getLogger(ScheduleConfig.class);

    /**
     * 示例定时任务：每小时执行一次（如需每日统计、清理等，在此类新增 @Scheduled 方法即可）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void hourlyHeartbeat() {
        log.debug("定时任务心跳：每小时执行");
    }
}
