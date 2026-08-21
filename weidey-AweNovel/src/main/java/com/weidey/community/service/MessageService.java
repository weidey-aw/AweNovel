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
     * 发送消息
     */
    void send(Long userId, Long senderId, String type, String content);

    /**
     * 分页查询用户消息
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
