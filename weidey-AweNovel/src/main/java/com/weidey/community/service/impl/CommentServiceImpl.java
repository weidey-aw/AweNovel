package com.weidey.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weidey.community.domain.Article;
import com.weidey.community.domain.Comment;
import com.weidey.community.mapper.ArticleMapper;
import com.weidey.community.mapper.CommentMapper;
import com.weidey.community.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 评论服务实现
 *
 * @author weidey
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public IPage<Comment> pageComments(IPage<Comment> page, String targetType, Long targetId) {
        return baseMapper.selectPage((Page<Comment>) page,
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getTargetType, targetType)
                        .eq(Comment::getTargetId, targetId)
                        .eq(Comment::getStatus, "1")
                        .orderByAsc(Comment::getCommentId));
    }

    @Override
    public Comment addComment(Comment comment) {
        if (comment.getPid() == null) {
            comment.setPid(0L);
        }
        if (comment.getLikeCount() == null) {
            comment.setLikeCount(0);
        }
        comment.setStatus("1");
        save(comment);
        // 若评论目标是文章，评论数 +1
        if ("A".equals(comment.getTargetType())) {
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .setSql("comment_count = comment_count + 1")
                    .eq(Article::getArticleId, comment.getTargetId()));
        }
        return comment;
    }
}
