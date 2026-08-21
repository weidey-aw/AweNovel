package com.weidey.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weidey.community.domain.Message;
import com.weidey.community.mapper.MessageMapper;
import com.weidey.community.service.MessageService;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 站内消息服务实现
 *
 * @author weidey
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    @Override
    public void send(Long userId, Long senderId, String type, String content) {
        Message message = new Message();
        message.setUserId(userId);
        message.setSenderId(senderId);
        message.setType(type);
        message.setContent(content);
        message.setIsRead("0");
        message.setCreateTime(new Date());
        save(message);
    }

    @Override
    public IPage<Message> pageMessages(IPage<Message> page, Long userId) {
        return baseMapper.selectPage((Page<Message>) page,
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getUserId, userId)
                        .orderByDesc(Message::getMessageId));
    }

    @Override
    public long countUnread(Long userId) {
        return count(new LambdaQueryWrapper<Message>()
                .eq(Message::getUserId, userId)
                .eq(Message::getIsRead, "0"));
    }

    @Override
    public void markRead(Long userId, Long messageId) {
        update(new LambdaUpdateWrapper<Message>()
                .set(Message::getIsRead, "1")
                .eq(Message::getUserId, userId)
                .eq(Message::getMessageId, messageId));
    }
}
