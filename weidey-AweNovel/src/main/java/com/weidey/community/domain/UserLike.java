package com.weidey.community.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户点赞 gal_user_like
 *
 * @author weidey
 */
@Data
@TableName("gal_user_like")
public class UserLike {

    /** ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 目标类型（G游戏 A文章 R资源 C评论） */
    private String targetType;

    /** 目标ID */
    private Long targetId;

    /** 创建时间 */
    private Date createTime;
}
