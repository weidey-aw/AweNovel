package com.weidey.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weidey.community.domain.UserLike;
import com.weidey.community.mapper.UserLikeMapper;
import com.weidey.community.service.LikeService;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 点赞服务实现
 *
 * @author weidey
 */
@Service
public class LikeServiceImpl extends ServiceImpl<UserLikeMapper, UserLike> implements LikeService {

    @Override
    public void like(Long userId, String targetType, Long targetId) {
        if (isLiked(userId, targetType, targetId)) {
            return;
        }
        UserLike like = new UserLike();
        like.setUserId(userId);
        like.setTargetType(targetType);
        like.setTargetId(targetId);
        like.setCreateTime(new Date());
        save(like);
    }

    @Override
    public void unlike(Long userId, String targetType, Long targetId) {
        remove(new LambdaQueryWrapper<UserLike>()
                .eq(UserLike::getUserId, userId)
                .eq(UserLike::getTargetType, targetType)
                .eq(UserLike::getTargetId, targetId));
    }

    @Override
    public boolean isLiked(Long userId, String targetType, Long targetId) {
        return count(new LambdaQueryWrapper<UserLike>()
                .eq(UserLike::getUserId, userId)
                .eq(UserLike::getTargetType, targetType)
                .eq(UserLike::getTargetId, targetId)) > 0;
    }

    @Override
    public long countLikes(String targetType, Long targetId) {
        return count(new LambdaQueryWrapper<UserLike>()
                .eq(UserLike::getTargetType, targetType)
                .eq(UserLike::getTargetId, targetId));
    }
}
