package com.weidey.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.weidey.community.domain.Message;

/**
 * 站内消息服务
 *
 * @author weidey
 */
public interface MessageService extends IService<Message> {

    /**
     * 发送消息（无业务关联）
     */
    void send(Long userId, Long senderId, String type, String content);

    /**
     * 发送消息（带业务关联，便于前端跳转）
     *
     * @param bizType 业务类型：game / article / resource / feedback
     * @param bizId   业务ID
     */
    void send(Long userId, Long senderId, String type, String bizType, Long bizId, String content);

    /**
     * 分页查询用户消息（补全发送人昵称/头像）
     */
    IPage<Message> pageMessages(IPage<Message> page, Long userId);

    /**
     * 未读消息数
     */
    long countUnread(Long userId);

    /**
     * 标记已读
     */
    void markRead(Long userId, Long messageId);
}
