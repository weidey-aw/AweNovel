package com.weidey.community.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户社区画像（积分/经验/等级） gal_user_profile
 *
 * @author weidey
 */
@Data
@TableName("gal_user_profile")
public class UserProfile {

    /** 用户ID（关联 sys_user.user_id） */
    @TableId(value = "user_id")
    private Long userId;

    /** 积分余额 */
    private Long points;

    /** 累计经验 */
    private Long exp;

    /** 等级（0-6） */
    private Integer level;

    /** 连续签到天数 */
    private Integer signStreak;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
