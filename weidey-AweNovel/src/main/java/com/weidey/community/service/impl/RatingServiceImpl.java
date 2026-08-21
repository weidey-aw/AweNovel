package com.weidey.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weidey.common.exception.ServiceException;
import com.weidey.community.domain.Game;
import com.weidey.community.domain.Rating;
import com.weidey.community.mapper.GameMapper;
import com.weidey.community.mapper.RatingMapper;
import com.weidey.community.service.RatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/**
 * 评分服务实现
 *
 * @author weidey
 */
@Service
public class RatingServiceImpl extends ServiceImpl<RatingMapper, Rating> implements RatingService {

    @Autowired
    private GameMapper gameMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Rating rate(Long userId, Long gameId, int score) {
        if (score < 1 || score > 10) {
            throw new ServiceException("评分需在 1-10 之间");
        }
        Rating rating = getOne(new LambdaQueryWrapper<Rating>()
                .eq(Rating::getUserId, userId)
                .eq(Rating::getGameId, gameId), false);
        if (rating == null) {
            rating = new Rating();
            rating.setUserId(userId);
            rating.setGameId(gameId);
            rating.setScore(score);
            rating.setCreateTime(new Date());
            save(rating);
        } else {
            rating.setScore(score);
            updateById(rating);
        }
        recalcGameRating(gameId);
        return rating;
    }

    @Override
    public Rating getUserRating(Long userId, Long gameId) {
        return getOne(new LambdaQueryWrapper<Rating>()
                .eq(Rating::getUserId, userId)
                .eq(Rating::getGameId, gameId), false);
    }

    /**
     * 重算游戏平均分与评分人数
     */
    private void recalcGameRating(Long gameId) {
        List<Rating> ratings = list(new LambdaQueryWrapper<Rating>().eq(Rating::getGameId, gameId));
        if (ratings == null || ratings.isEmpty()) {
            return;
        }
        int total = ratings.stream().mapToInt(Rating::getScore).sum();
        BigDecimal avg = BigDecimal.valueOf(total).divide(BigDecimal.valueOf(ratings.size()), 1, RoundingMode.HALF_UP);
        gameMapper.update(null, new LambdaUpdateWrapper<Game>()
                .set(Game::getRatingAvg, avg)
                .set(Game::getRatingCount, ratings.size())
                .eq(Game::getGameId, gameId));
    }
}
