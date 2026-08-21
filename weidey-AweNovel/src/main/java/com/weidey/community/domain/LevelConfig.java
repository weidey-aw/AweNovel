package com.weidey.community.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 等级配置（B站式 0-6 级） gal_level_config
 *
 * @author weidey
 */
@Data
@TableName("gal_level_config")
public class LevelConfig {

    /** 等级（0-6） */
    @TableId(value = "level")
    private Integer level;

    /** 等级名称 */
    private String name;

    /** 升级所需累计经验 */
    private Long minExp;
}
