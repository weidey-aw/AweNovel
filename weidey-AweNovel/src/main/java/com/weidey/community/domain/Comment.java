package com.weidey.community.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评论 gal_comment
 *
 * @author weidey
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gal_comment")
public class Comment extends BaseGalEntity {

    /** 评论ID */
    @TableId(value = "comment_id", type = IdType.AUTO)
    private Long commentId;

    /** 目标类型（G游戏 A文章 R资源） */
    private String targetType;

    /** 目标ID */
    private Long targetId;

    /** 用户ID */
    private Long userId;

    /** 父评论ID（0为顶层） */
    private Long pid;

    /** 评论内容 */
    private String content;

    /** 点赞数 */
    private Integer likeCount;

    /** 状态（1正常 0隐藏） */
    private String status;

    /** 用户昵称（非表字段） */
    @TableField(exist = false)
    private String nickName;

    /** 用户头像（非表字段） */
    @TableField(exist = false)
    private String avatar;
}
