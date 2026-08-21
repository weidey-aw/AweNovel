package com.weidey.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.weidey.community.domain.Comment;

/**
 * 评论服务
 *
 * @author weidey
 */
public interface CommentService extends IService<Comment> {

    /**
     * 分页查询某目标的评论
     */
    IPage<Comment> pageComments(IPage<Comment> page, String targetType, Long targetId);

    /**
     * 发表评论
     */
    Comment addComment(Comment comment);
}
