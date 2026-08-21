package com.weidey.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weidey.common.utils.StringUtils;
import com.weidey.community.domain.Article;
import com.weidey.community.mapper.ArticleMapper;
import com.weidey.community.service.ArticleService;
import com.weidey.community.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 文章服务实现
 *
 * @author weidey
 */
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Autowired
    private ReviewService reviewService;

    @Override
    public IPage<Article> pageArticles(IPage<Article> page, String category, String status, String keyword) {
        return baseMapper.selectPage((Page<Article>) page,
                new LambdaQueryWrapper<Article>()
                        .eq(StringUtils.isNotBlank(category), Article::getCategory, category)
                        .eq(StringUtils.isNotBlank(status), Article::getStatus, status)
                        .and(StringUtils.isNotBlank(keyword), w -> w.like(Article::getTitle, keyword))
                        .orderByDesc(Article::getArticleId));
    }

    @Override
    public Article publish(Article article) {
        article.setStatus("0");
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);
        save(article);
        // 启动 Flowable 审核流程
        article.setProcessInstanceId(reviewService.startReview("article", article.getArticleId()));
        updateById(article);
        return article;
    }

    @Override
    public void incrementView(Long articleId) {
        update(new LambdaUpdateWrapper<Article>()
                .setSql("view_count = view_count + 1")
                .eq(Article::getArticleId, articleId));
    }
}
