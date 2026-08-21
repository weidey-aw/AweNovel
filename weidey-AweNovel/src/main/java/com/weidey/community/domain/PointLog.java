package com.weidey.community.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 积分流水（不可变） gal_point_log
 *
 * @author weidey
 */
@Data
@TableName("gal_point_log")
public class PointLog {

    /** 流水ID */
    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;

    /** 用户ID */
    private Long userId;

    /** 变化类型（signin/register/download/publish/comment/rating/ai_chat/admin） */
    private String changeType;

    /** 变化量（正加负减） */
    private Integer changeAmount;

    /** 变化后余额 */
    private Long balanceAfter;

    /** 业务类型 */
    private String bizType;

    /** 业务ID */
    private Long bizId;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private Date createTime;
}
