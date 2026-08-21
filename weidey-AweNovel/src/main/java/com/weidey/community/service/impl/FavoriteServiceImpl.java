package com.weidey.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weidey.community.domain.UserFavorite;
import com.weidey.community.mapper.UserFavoriteMapper;
import com.weidey.community.service.FavoriteService;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 收藏服务实现
 *
 * @author weidey
 */
@Service
public class FavoriteServiceImpl extends ServiceImpl<UserFavoriteMapper, UserFavorite> implements FavoriteService {

    @Override
    public void favorite(Long userId, String targetType, Long targetId) {
        if (isFavorited(userId, targetType, targetId)) {
            return;
        }
        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setTargetType(targetType);
        favorite.setTargetId(targetId);
        favorite.setCreateTime(new Date());
        save(favorite);
    }

    @Override
    public void unfavorite(Long userId, String targetType, Long targetId) {
        remove(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getTargetType, targetType)
                .eq(UserFavorite::getTargetId, targetId));
    }

    @Override
    public boolean isFavorited(Long userId, String targetType, Long targetId) {
        return count(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getTargetType, targetType)
                .eq(UserFavorite::getTargetId, targetId)) > 0;
    }

    @Override
    public IPage<UserFavorite> pageFavorites(IPage<UserFavorite> page, Long userId) {
        return baseMapper.selectPage((Page<UserFavorite>) page,
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .orderByDesc(UserFavorite::getId));
    }
}
