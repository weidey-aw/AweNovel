package com.weidey.community.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weidey.community.domain.Rating;

/**
 * 评分服务
 *
 * @author weidey
 */
public interface RatingService extends IService<Rating> {

    /**
     * 用户评分（重复评分则更新，并重算游戏平均分）
     */
    Rating rate(Long userId, Long gameId, int score);

    /**
     * 查询用户对游戏的评分
     */
    Rating getUserRating(Long userId, Long gameId);
}
