package com.weidey.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.weidey.community.domain.Comment;

import java.util.List;

/**
 * 评论服务
 *
 * @author weidey
 */
public interface CommentService extends IService<Comment> {

    /**
     * 分页查询某目标的评论（公开，仅正常状态）
     */
    IPage<Comment> pageComments(IPage<Comment> page, String targetType, Long targetId);

    /**
     * 管理端分页（关键词 / 目标类型 / 目标ID / 状态）
     */
    IPage<Comment> pageAdminComments(IPage<Comment> page, String keyword, String targetType, Long targetId, String status);

    /**
     * 评论详情（含用户昵称头像）
     */
    Comment getCommentDetail(Long commentId);

    /**
     * 发表评论（评论数 +1）
     */
    Comment addComment(Comment comment);

    /**
     * 修改评论状态（1 正常 / 0 隐藏）
     */
    void updateStatus(Long commentId, String status);

    /**
     * 删除评论（含子回复），同步扣减文章评论数
     */
    void deleteComments(List<Long> commentIds);
}
