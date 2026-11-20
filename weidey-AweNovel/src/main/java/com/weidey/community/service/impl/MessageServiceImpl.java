package com.weidey.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weidey.common.core.domain.entity.SysUser;
import com.weidey.common.utils.StringUtils;
import com.weidey.community.domain.Message;
import com.weidey.community.mapper.MessageMapper;
import com.weidey.community.service.MessageService;
import com.weidey.system.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 站内消息服务实现
 *
 * @author weidey
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public void send(Long userId, Long senderId, String type, String content) {
        send(userId, senderId, type, null, null, content);
    }

    @Override
    public void send(Long userId, Long senderId, String type, String bizType, Long bizId, String content) {
        Message message = new Message();
        message.setUserId(userId);
        message.setSenderId(senderId);
        message.setType(type);
        message.setBizType(bizType);
        message.setBizId(bizId);
        message.setContent(content);
        message.setIsRead("0");
        message.setCreateTime(new Date());
        save(message);
    }

    @Override
    public IPage<Message> pageMessages(IPage<Message> page, Long userId) {
        Page<Message> result = (Page<Message>) baseMapper.selectPage((Page<Message>) page,
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getUserId, userId)
                        .orderByDesc(Message::getMessageId));
        fillSenders(result.getRecords());
        return result;
    }

    /** 补全发送人昵称与头像 */
    private void fillSenders(List<Message> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> senderIds = rows.stream().map(Message::getSenderId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = senderIds.isEmpty() ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(senderIds).stream()
                .collect(Collectors.toMap(SysUser::getUserId, u -> u, (a, b) -> a));
        for (Message message : rows) {
            SysUser sender = message.getSenderId() == null ? null : userMap.get(message.getSenderId());
            if (sender != null) {
                message.setSenderName(StringUtils.isNotBlank(sender.getNickName()) ? sender.getNickName() : sender.getUserName());
                message.setSenderAvatar(sender.getAvatar());
            }
        }
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
