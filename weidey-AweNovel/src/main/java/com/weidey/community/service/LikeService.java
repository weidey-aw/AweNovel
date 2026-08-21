package com.weidey.community.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weidey.community.domain.UserLike;

/**
 * 点赞服务
 *
 * @author weidey
 */
public interface LikeService extends IService<UserLike> {

    void like(Long userId, String targetType, Long targetId);

    void unlike(Long userId, String targetType, Long targetId);

    boolean isLiked(Long userId, String targetType, Long targetId);

    long countLikes(String targetType, Long targetId);
}
