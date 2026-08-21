package com.weidey.community.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 签到记录 gal_sign_record
 *
 * @author weidey
 */
@Data
@TableName("gal_sign_record")
public class SignRecord {

    /** ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 签到日期 */
    private Date signDate;

    /** 连续签到天数 */
    private Integer continuousDays;

    /** 积分奖励 */
    private Integer pointsAward;

    /** 经验奖励 */
    private Integer expAward;

    /** 创建时间 */
    private Date createTime;
}
