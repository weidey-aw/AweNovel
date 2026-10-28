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
import com.weidey.community.service.ArticleService;
import com.weidey.community.service.ReviewService;
import com.weidey.system.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 文章服务实现
 *
 * @author weidey
 */
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public IPage<Article> pageArticles(IPage<Article> page, String category, String status, String keyword) {
        Page<Article> result = (Page<Article>) baseMapper.selectPage((Page<Article>) page,
                new LambdaQueryWrapper<Article>()
                        .eq(StringUtils.isNotBlank(category), Article::getCategory, category)
                        .eq(StringUtils.isNotBlank(status), Article::getStatus, status)
                        .like(StringUtils.isNotBlank(keyword), Article::getTitle, keyword)
                        .orderByDesc(Article::getArticleId));
        fillAuthors(result.getRecords());
        return result;
    }

    @Override
    public IPage<Article> pageAdminArticles(IPage<Article> page, String keyword, String category, String status) {
        return pageArticles(page, category, status, keyword);
    }

    @Override
    public Article getArticleDetail(Long articleId) {
        Article article = getById(articleId);
        if (article != null) {
            fillAuthors(Collections.singletonList(article));
        }
        return article;
    }

    /** 批量填充作者昵称与头像 */
    private void fillAuthors(List<Article> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> userIds = rows.stream().map(Article::getUserId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getUserId, u -> u, (a, b) -> a));
        for (Article article : rows) {
            SysUser user = article.getUserId() == null ? null : userMap.get(article.getUserId());
            if (user != null) {
                article.setUserName(StringUtils.isNotBlank(user.getNickName()) ? user.getNickName() : user.getUserName());
                article.setUserAvatar(user.getAvatar());
            }
        }
    }

    @Override
    public Article publish(Article article) {
        article.setStatus("0");
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);
        article.setArticleId(null);
        save(article);
        // 启动 Flowable 审核流程
        article.setProcessInstanceId(reviewService.startReview("article", article.getArticleId()));
        updateById(article);
        return article;
    }

    @Override
    public Article saveArticle(Article article) {
        if (StringUtils.isBlank(article.getStatus())) {
            article.setStatus("1");
        }
        article.setArticleId(null);
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);
        save(article);
        return article;
    }

    @Override
    public Article updateArticle(Article article) {
        updateById(article);
        return article;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticles(List<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return;
        }
        // 级联删除文章评论
        commentMapper.delete(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getTargetType, "A")
                .in(Comment::getTargetId, articleIds));
        removeByIds(articleIds);
    }

    @Override
    public void updateStatus(Long articleId, String status) {
        update(new LambdaUpdateWrapper<Article>()
                .set(Article::getStatus, status)
                .eq(Article::getArticleId, articleId));
    }

    @Override
    public void incrementView(Long articleId) {
        update(new LambdaUpdateWrapper<Article>()
                .setSql("view_count = view_count + 1")
                .eq(Article::getArticleId, articleId));
    }
}
