package com.weidey.community.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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

    /** 消息类型（comment/like/audit/system） */
    private String type;

    /** 消息内容 */
    private String content;

    /** 是否已读（0未读 1已读） */
    private String isRead;

    /** 创建时间 */
    private Date createTime;
}
