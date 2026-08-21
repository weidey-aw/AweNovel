package com.weidey.community.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户关注 gal_user_follow
 *
 * @author weidey
 */
@Data
@TableName("gal_user_follow")
public class UserFollow {

    /** ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID（关注者） */
    private Long userId;

    /** 被关注用户ID */
    private Long followUserId;

    /** 创建时间 */
    private Date createTime;
}
