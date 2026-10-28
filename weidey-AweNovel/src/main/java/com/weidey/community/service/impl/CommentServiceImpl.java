package com.weidey.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weidey.common.core.domain.entity.SysUser;
import com.weidey.common.utils.StringUtils;
import com.weidey.community.domain.Article;
import com.weidey.community.domain.Comment;
import com.weidey.community.mapper.ArticleMapper;
import com.weidey.community.mapper.CommentMapper;
import com.weidey.community.service.CommentService;
import com.weidey.system.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 评论服务实现
 *
 * @author weidey
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public IPage<Comment> pageComments(IPage<Comment> page, String targetType, Long targetId) {
        Page<Comment> result = (Page<Comment>) baseMapper.selectPage((Page<Comment>) page,
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getTargetType, targetType)
                        .eq(Comment::getTargetId, targetId)
                        .eq(Comment::getStatus, "1")
                        .orderByAsc(Comment::getCommentId));
        fillUsers(result.getRecords());
        return result;
    }

    @Override
    public IPage<Comment> pageAdminComments(IPage<Comment> page, String keyword, String targetType, Long targetId, String status) {
        Page<Comment> result = (Page<Comment>) baseMapper.selectPage((Page<Comment>) page,
                new LambdaQueryWrapper<Comment>()
                        .like(StringUtils.isNotBlank(keyword), Comment::getContent, keyword)
                        .eq(StringUtils.isNotBlank(targetType), Comment::getTargetType, targetType)
                        .eq(targetId != null, Comment::getTargetId, targetId)
                        .eq(StringUtils.isNotBlank(status), Comment::getStatus, status)
                        .orderByDesc(Comment::getCommentId));
        fillUsers(result.getRecords());
        return result;
    }

    @Override
    public Comment getCommentDetail(Long commentId) {
        Comment comment = getById(commentId);
        if (comment != null) {
            fillUsers(Collections.singletonList(comment));
        }
        return comment;
    }

    /** 批量填充用户昵称与头像 */
    private void fillUsers(List<Comment> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> userIds = rows.stream().map(Comment::getUserId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getUserId, u -> u, (a, b) -> a));
        for (Comment comment : rows) {
            SysUser user = comment.getUserId() == null ? null : userMap.get(comment.getUserId());
            if (user != null) {
                comment.setNickName(StringUtils.isNotBlank(user.getNickName()) ? user.getNickName() : user.getUserName());
                comment.setAvatar(user.getAvatar());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Comment addComment(Comment comment) {
        if (comment.getPid() == null) {
            comment.setPid(0L);
        }
        if (comment.getLikeCount() == null) {
            comment.setLikeCount(0);
        }
        comment.setStatus("1");
        save(comment);
        // 文章评论数 +1
        if ("A".equals(comment.getTargetType())) {
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .setSql("comment_count = comment_count + 1")
                    .eq(Article::getArticleId, comment.getTargetId()));
        }
        return comment;
    }

    @Override
    public void updateStatus(Long commentId, String status) {
        update(new LambdaUpdateWrapper<Comment>()
                .set(Comment::getStatus, status)
                .eq(Comment::getCommentId, commentId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComments(List<Long> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return;
        }
        // 连同子回复一起删除
        List<Comment> children = list(new LambdaQueryWrapper<Comment>().in(Comment::getPid, commentIds));
        List<Long> allIds = new ArrayList<>(commentIds);
        for (Comment child : children) {
            if (!allIds.contains(child.getCommentId())) {
                allIds.add(child.getCommentId());
            }
        }
        // 统计受影响文章，同步扣减评论数
        List<Comment> affected = list(new LambdaQueryWrapper<Comment>()
                .in(Comment::getCommentId, allIds)
                .eq(Comment::getTargetType, "A"));
        Map<Long, Long> articleCounts = affected.stream()
                .filter(c -> c.getTargetId() != null)
                .collect(Collectors.groupingBy(Comment::getTargetId, Collectors.counting()));
        removeByIds(allIds);
        for (Map.Entry<Long, Long> entry : articleCounts.entrySet()) {
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .setSql("comment_count = GREATEST(comment_count - " + entry.getValue() + ", 0)")
                    .eq(Article::getArticleId, entry.getKey()));
        }
    }
}
