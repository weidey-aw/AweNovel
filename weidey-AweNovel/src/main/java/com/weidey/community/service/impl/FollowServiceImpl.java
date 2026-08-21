package com.weidey.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weidey.community.domain.UserFollow;
import com.weidey.community.mapper.UserFollowMapper;
import com.weidey.community.service.FollowService;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 关注服务实现
 *
 * @author weidey
 */
@Service
public class FollowServiceImpl extends ServiceImpl<UserFollowMapper, UserFollow> implements FollowService {

    @Override
    public void follow(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId) || isFollowing(userId, targetUserId)) {
            return;
        }
        UserFollow follow = new UserFollow();
        follow.setUserId(userId);
        follow.setFollowUserId(targetUserId);
        follow.setCreateTime(new Date());
        save(follow);
    }

    @Override
    public void unfollow(Long userId, Long targetUserId) {
        remove(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getUserId, userId)
                .eq(UserFollow::getFollowUserId, targetUserId));
    }

    @Override
    public boolean isFollowing(Long userId, Long targetUserId) {
        return count(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getUserId, userId)
                .eq(UserFollow::getFollowUserId, targetUserId)) > 0;
    }

    @Override
    public IPage<UserFollow> pageFollowing(IPage<UserFollow> page, Long userId) {
        return baseMapper.selectPage((Page<UserFollow>) page,
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getUserId, userId)
                        .orderByDesc(UserFollow::getId));
    }

    @Override
    public IPage<UserFollow> pageFollowers(IPage<UserFollow> page, Long userId) {
        return baseMapper.selectPage((Page<UserFollow>) page,
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowUserId, userId)
                        .orderByDesc(UserFollow::getId));
    }
}
