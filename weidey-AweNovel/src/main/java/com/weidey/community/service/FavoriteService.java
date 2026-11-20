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

    /**
     * 收藏 / 取消收藏（切换）
     *
     * @return true 表示当前已收藏
     */
    boolean toggle(Long userId, String targetType, Long targetId);

    boolean isFavorited(Long userId, String targetType, Long targetId);

    /**
     * 我的收藏（可按类型过滤，补全标题/封面/跳转地址）
     */
    IPage<UserFavorite> pageFavorites(IPage<UserFavorite> page, Long userId, String targetType);
}
