package com.weidey.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.weidey.community.domain.Article;

import java.util.List;

/**
 * 文章服务（资讯/评测/攻略）
 *
 * @author weidey
 */
public interface ArticleService extends IService<Article> {

    /**
     * 分页查询文章（前台/管理端通用：category + status + keyword）
     */
    IPage<Article> pageArticles(IPage<Article> page, String category, String status, String keyword);

    /**
     * 管理端分页（关键词 / 分类 / 状态）
     */
    IPage<Article> pageAdminArticles(IPage<Article> page, String keyword, String category, String status);

    /**
     * 文章详情（含作者昵称与头像）
     */
    Article getArticleDetail(Long articleId);

    /**
     * 发布文章（初始为待审核）
     */
    Article publish(Article article);

    /**
     * 管理端新增（可指定状态）
     */
    Article saveArticle(Article article);

    /**
     * 管理端修改
     */
    Article updateArticle(Article article);

    /**
     * 管理端删除（级联删除评论）
     */
    void deleteArticles(List<Long> articleIds);

    /**
     * 修改审核状态（0待审核 / 1通过 / 2拒绝）
     */
    void updateStatus(Long articleId, String status);

    /**
     * 浏览量 +1
     */
    void incrementView(Long articleId);
}
