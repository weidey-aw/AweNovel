package com.weidey.community.controller;

import com.weidey.common.core.controller.BaseController;
import com.weidey.common.core.domain.AjaxResult;
import com.weidey.community.domain.Rating;
import com.weidey.community.service.RatingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评分接口
 *
 * @author weidey
 */
@RestController
@RequestMapping("/community/rating")
public class RatingController extends BaseController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    /** 我的评分（公开，未登录返回 null） */
    @GetMapping("/{gameId}")
    public AjaxResult myRating(@PathVariable Long gameId) {
        Rating rating = ratingService.getUserRating(getUserId(), gameId);
        return success(rating);
    }

    /** 评分（需登录） */
    @PostMapping
    public AjaxResult rate(@RequestBody Rating rating) {
        return success(ratingService.rate(getUserId(), rating.getGameId(), rating.getScore()));
    }
}
