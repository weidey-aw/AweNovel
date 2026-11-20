package com.weidey.community.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 站内消息 gal_message
 *
 * @author weidey
 */
@Data
@TableName("gal_message")
public class Message {

    /** 消息ID */
    @TableId(value = "message_id", type = IdType.AUTO)
    private Long messageId;

    /** 接收者用户ID */
    private Long userId;

    /** 发送者用户ID（系统消息为空） */
    private Long senderId;

    /** 消息类型（comment/like/audit/system/feedback） */
    private String type;

    /** 业务类型（game/article/resource/feedback） */
    private String bizType;

    /** 业务ID */
    private Long bizId;

    /** 消息内容 */
    private String content;

    /** 是否已读（0未读 1已读） */
    private String isRead;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /* ---------------- 非表字段 ---------------- */

    /** 发送人昵称 */
    @TableField(exist = false)
    private String senderName;

    /** 发送人头像 */
    @TableField(exist = false)
    private String senderAvatar;
}
