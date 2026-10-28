package com.weidey.community.controller;

import com.weidey.common.core.controller.BaseController;
import com.weidey.common.core.domain.AjaxResult;
import com.weidey.common.core.page.TableDataInfo;
import com.weidey.common.utils.StringUtils;
import com.weidey.community.domain.Article;
import com.weidey.community.service.ArticleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * 文章接口（资讯/评测/攻略）
 *
 * <p>审核状态：0 待审核 / 1 通过 / 2 拒绝；前台列表仅返回 1。</p>
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

    /* ==================== 前台 ==================== */

    /** 文章列表（公开，仅审核通过） */
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String category,
                              @RequestParam(required = false) String keyword) {
        return getDataTable(articleService.pageArticles(startPage(), category, "1", keyword));
    }

    /** 文章详情（公开，浏览量 +1） */
    @GetMapping("/{articleId}")
    public AjaxResult detail(@PathVariable Long articleId) {
        Article article = articleService.getArticleDetail(articleId);
        if (article == null) {
            return error("文章不存在");
        }
        articleService.incrementView(articleId);
        return success(article);
    }

    /** 发布文章（登录，进入待审核） */
    @PostMapping
    public AjaxResult publish(@RequestBody Article article) {
        if (StringUtils.isBlank(article.getTitle())) {
            return error("标题不能为空");
        }
        article.setUserId(getUserId());
        return success(articleService.publish(article));
    }

    /* ==================== 管理端 ==================== */

    /** 管理端文章列表 */
    @PreAuthorize("@ss.hasPermi('community:article:list')")
    @GetMapping("/admin/list")
    public TableDataInfo adminList(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) String category,
                                   @RequestParam(required = false) String status) {
        return getDataTable(articleService.pageAdminArticles(startPage(), keyword, category, status));
    }

    /** 管理端文章详情 */
    @PreAuthorize("@ss.hasPermi('community:article:list')")
    @GetMapping("/admin/{articleId}")
    public AjaxResult adminDetail(@PathVariable Long articleId) {
        Article article = articleService.getArticleDetail(articleId);
        if (article == null) {
            return error("文章不存在");
        }
        return success(article);
    }

    /** 管理端新增文章 */
    @PreAuthorize("@ss.hasPermi('community:article:add')")
    @PostMapping("/admin")
    public AjaxResult adminAdd(@RequestBody Article article) {
        if (StringUtils.isBlank(article.getTitle())) {
            return error("标题不能为空");
        }
        if (article.getUserId() == null) {
            article.setUserId(getUserId());
        }
        return success(articleService.saveArticle(article));
    }

    /** 管理端修改文章 */
    @PreAuthorize("@ss.hasPermi('community:article:edit')")
    @PutMapping("/admin")
    public AjaxResult adminEdit(@RequestBody Article article) {
        if (article.getArticleId() == null) {
            return error("文章ID不能为空");
        }
        return success(articleService.updateArticle(article));
    }

    /** 管理端修改审核状态 */
    @PreAuthorize("@ss.hasPermi('community:article:edit')")
    @PutMapping("/admin/status")
    public AjaxResult adminStatus(@RequestBody Article article) {
        if (article.getArticleId() == null || StringUtils.isBlank(article.getStatus())) {
            return error("参数不完整");
        }
        articleService.updateStatus(article.getArticleId(), article.getStatus());
        return success();
    }

    /** 管理端删除文章（级联删除评论） */
    @PreAuthorize("@ss.hasPermi('community:article:remove')")
    @DeleteMapping("/admin/{articleIds}")
    public AjaxResult adminRemove(@PathVariable Long[] articleIds) {
        articleService.deleteArticles(Arrays.asList(articleIds));
        return success("删除成功（已同步删除文章评论）");
    }
}
