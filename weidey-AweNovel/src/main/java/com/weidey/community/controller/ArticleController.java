package com.weidey.community.controller;

import com.weidey.common.core.controller.BaseController;
import com.weidey.common.core.domain.AjaxResult;
import com.weidey.common.core.page.TableDataInfo;
import com.weidey.community.domain.Article;
import com.weidey.community.service.ArticleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文章接口（资讯/评测/攻略）
 *
 * @author weidey
 */
@RestController
@RequestMapping("/community/article")
public class ArticleController extends BaseController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    /** 文章列表（公开，仅审核通过） */
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String category,
                              @RequestParam(required = false) String keyword) {
        return getDataTable(articleService.pageArticles(startPage(), category, "1", keyword));
    }

    /** 文章详情（公开） */
    @GetMapping("/{articleId}")
    public AjaxResult detail(@PathVariable Long articleId) {
        Article article = articleService.getById(articleId);
        if (article == null) {
            return error("文章不存在");
        }
        articleService.incrementView(articleId);
        return success(article);
    }

    /** 发布文章（需登录，初始待审核） */
    @PostMapping
    public AjaxResult publish(@RequestBody Article article) {
        article.setUserId(getUserId());
        return success(articleService.publish(article));
    }
}
