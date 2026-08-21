package com.weidey.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.weidey.community.domain.UserFavorite;

/**
 * 收藏服务
 *
 * @author weidey
 */
public interface FavoriteService extends IService<UserFavorite> {

    void favorite(Long userId, String targetType, Long targetId);

    void unfavorite(Long userId, String targetType, Long targetId);

    boolean isFavorited(Long userId, String targetType, Long targetId);

    IPage<UserFavorite> pageFavorites(IPage<UserFavorite> page, Long userId);
}
