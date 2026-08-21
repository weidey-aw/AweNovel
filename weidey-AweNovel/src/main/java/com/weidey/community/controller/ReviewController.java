package com.weidey.community.controller;

import com.weidey.common.core.controller.BaseController;
import com.weidey.common.core.domain.AjaxResult;
import com.weidey.community.service.ArticleService;
import com.weidey.community.service.ResourceService;
import com.weidey.community.service.ReviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 内容审核接口（管理端，Flowable）
 *
 * @author weidey
 */
@RestController
@RequestMapping("/community/review")
public class ReviewController extends BaseController {

    private final ReviewService reviewService;
    private final ResourceService resourceService;
    private final ArticleService articleService;

    public ReviewController(ReviewService reviewService, ResourceService resourceService, ArticleService articleService) {
        this.reviewService = reviewService;
        this.resourceService = resourceService;
        this.articleService = articleService;
    }

    /** 待审核任务列表 */
    @PreAuthorize("@ss.hasPermi('community:review:list')")
    @GetMapping("/tasks")
    public AjaxResult tasks() {
        return success(reviewService.listPendingTasks());
    }

    /** 审核通过 */
    @PreAuthorize("@ss.hasPermi('community:review:edit')")
    @PostMapping("/approve/{processInstanceId}")
    public AjaxResult approve(@PathVariable String processInstanceId) {
        Map<String, Object> biz = reviewService.completeReview(processInstanceId, true);
        updateStatus(biz, true);
        return success();
    }

    /** 审核拒绝 */
    @PreAuthorize("@ss.hasPermi('community:review:edit')")
    @PostMapping("/reject/{processInstanceId}")
    public AjaxResult reject(@PathVariable String processInstanceId) {
        Map<String, Object> biz = reviewService.completeReview(processInstanceId, false);
        updateStatus(biz, false);
        return success();
    }

    private void updateStatus(Map<String, Object> biz, boolean approved) {
        String bizType = (String) biz.get("bizType");
        Long bizId = biz.get("bizId") != null ? Long.valueOf(biz.get("bizId").toString()) : null;
        if (bizId == null) {
            return;
        }
        String status = approved ? "1" : "2";
        if ("resource".equals(bizType)) {
            resourceService.lambdaUpdate()
                    .set(com.weidey.community.domain.Resource::getStatus, status)
                    .eq(com.weidey.community.domain.Resource::getResourceId, bizId)
                    .update();
        } else if ("article".equals(bizType)) {
            articleService.lambdaUpdate()
                    .set(com.weidey.community.domain.Article::getStatus, status)
                    .eq(com.weidey.community.domain.Article::getArticleId, bizId)
                    .update();
        }
    }
}
