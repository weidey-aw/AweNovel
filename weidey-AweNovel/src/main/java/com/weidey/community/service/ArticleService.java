package com.weidey.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.weidey.community.domain.Article;

/**
 * 文章服务（资讯/评测/攻略）
 *
 * @author weidey
 */
public interface ArticleService extends IService<Article> {

    /**
     * 分页查询文章
     */
    IPage<Article> pageArticles(IPage<Article> page, String category, String status, String keyword);

    /**
     * 发布文章（初始为待审核）
     */
    Article publish(Article article);

    /**
     * 浏览量 +1
     */
    void incrementView(Long articleId);
}
