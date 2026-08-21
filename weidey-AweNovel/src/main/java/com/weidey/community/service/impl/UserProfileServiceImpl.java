package com.weidey.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weidey.community.domain.LevelConfig;
import com.weidey.community.domain.PointLog;
import com.weidey.community.domain.UserProfile;
import com.weidey.community.mapper.LevelConfigMapper;
import com.weidey.community.mapper.PointLogMapper;
import com.weidey.community.mapper.UserProfileMapper;
import com.weidey.community.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 用户社区画像服务实现
 *
 * @author weidey
 */
@Service
public class UserProfileServiceImpl extends ServiceImpl<UserProfileMapper, UserProfile> implements UserProfileService {

    @Autowired
    private PointLogMapper pointLogMapper;

    @Autowired
    private LevelConfigMapper levelConfigMapper;

    @Override
    public UserProfile getOrCreate(Long userId) {
        UserProfile profile = getById(userId);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setPoints(0L);
            profile.setExp(0L);
            profile.setLevel(0);
            profile.setSignStreak(0);
            save(profile);
        }
        return profile;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPoints(Long userId, int amount, String changeType, String bizType, Long bizId, String remark) {
        if (amount <= 0) {
            return;
        }
        UserProfile profile = getOrCreate(userId);
        update(new LambdaUpdateWrapper<UserProfile>()
                .setSql("points = points + " + amount)
                .eq(UserProfile::getUserId, userId));
        insertPointLog(userId, changeType, amount, profile.getPoints() + amount, bizType, bizId, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean spendPoints(Long userId, int amount, String changeType, String bizType, Long bizId, String remark) {
        if (amount <= 0) {
            return true;
        }
        getOrCreate(userId);
        boolean updated = update(new LambdaUpdateWrapper<UserProfile>()
                .setSql("points = points - " + amount)
                .eq(UserProfile::getUserId, userId)
                .ge(UserProfile::getPoints, amount));
        if (!updated) {
            return false;
        }
        UserProfile profile = getById(userId);
        insertPointLog(userId, changeType, -amount, profile.getPoints(), bizType, bizId, remark);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addExp(Long userId, long amount) {
        if (amount <= 0) {
            return;
        }
        UserProfile profile = getOrCreate(userId);
        update(new LambdaUpdateWrapper<UserProfile>()
                .setSql("exp = exp + " + amount)
                .eq(UserProfile::getUserId, userId));
        long newExp = profile.getExp() + amount;
        int level = computeLevel(newExp);
        update(new LambdaUpdateWrapper<UserProfile>()
                .set(UserProfile::getLevel, level)
                .eq(UserProfile::getUserId, userId));
    }

    @Override
    public int computeLevel(long exp) {
        List<LevelConfig> configs = levelConfigMapper.selectList(
                new LambdaQueryWrapper<LevelConfig>().orderByAsc(LevelConfig::getLevel));
        int level = 0;
        for (LevelConfig config : configs) {
            if (exp >= config.getMinExp()) {
                level = config.getLevel();
            } else {
                break;
            }
        }
        return level;
    }

    @Override
    public IPage<PointLog> pagePointLogs(IPage<PointLog> page, Long userId) {
        return pointLogMapper.selectPage((Page<PointLog>) page,
                new LambdaQueryWrapper<PointLog>()
                        .eq(PointLog::getUserId, userId)
                        .orderByDesc(PointLog::getLogId));
    }

    private void insertPointLog(Long userId, String changeType, int amount, long balanceAfter,
                                String bizType, Long bizId, String remark) {
        PointLog log = new PointLog();
        log.setUserId(userId);
        log.setChangeType(changeType);
        log.setChangeAmount(amount);
        log.setBalanceAfter(balanceAfter);
        log.setBizType(bizType);
        log.setBizId(bizId);
        log.setRemark(remark);
        log.setCreateTime(new Date());
        pointLogMapper.insert(log);
    }
}
