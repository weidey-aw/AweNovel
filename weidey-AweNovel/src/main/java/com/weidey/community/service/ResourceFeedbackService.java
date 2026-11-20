package com.weidey.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.weidey.community.domain.ResourceFeedback;

/**
 * 资源反馈服务
 *
 * @author weidey
 */
public interface ResourceFeedbackService extends IService<ResourceFeedback> {

    /**
     * 提交反馈：写入反馈表，并给资源上传者发送站内消息
     *
     * @return 反馈记录
     */
    ResourceFeedback submit(Long userId, Long resourceId, Integer type, String content);

    /**
     * 资源反馈列表（仅上传者 / 管理员可见）
     */
    IPage<ResourceFeedback> pageByResource(IPage<ResourceFeedback> page, Long resourceId, Long userId);

    /**
     * 我收到的反馈（按资源上传者维度）
     */
    IPage<ResourceFeedback> pageReceived(IPage<ResourceFeedback> page, Long userId);

    /**
     * 未读反馈数
     */
    long countUnread(Long userId);

    /**
     * 标记已读（仅资源上传者）
     */
    void markRead(Long userId, Long feedbackId);
}
