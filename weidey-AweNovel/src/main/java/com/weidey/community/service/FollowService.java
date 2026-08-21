package com.weidey.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.weidey.community.domain.UserFollow;

/**
 * 关注服务
 *
 * @author weidey
 */
public interface FollowService extends IService<UserFollow> {

    void follow(Long userId, Long targetUserId);

    void unfollow(Long userId, Long targetUserId);

    boolean isFollowing(Long userId, Long targetUserId);

    IPage<UserFollow> pageFollowing(IPage<UserFollow> page, Long userId);

    IPage<UserFollow> pageFollowers(IPage<UserFollow> page, Long userId);
}
