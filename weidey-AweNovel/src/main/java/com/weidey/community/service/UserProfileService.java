package com.weidey.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.weidey.community.domain.PointLog;
import com.weidey.community.domain.UserProfile;

/**
 * 用户社区画像（积分/经验/等级）服务
 *
 * @author weidey
 */
public interface UserProfileService extends IService<UserProfile> {

    /**
     * 获取用户画像，不存在则初始化
     */
    UserProfile getOrCreate(Long userId);

    /**
     * 增加积分（含流水记录）
     */
    void addPoints(Long userId, int amount, String changeType, String bizType, Long bizId, String remark);

    /**
     * 扣减积分（余额不足返回 false，含流水记录）
     */
    boolean spendPoints(Long userId, int amount, String changeType, String bizType, Long bizId, String remark);

    /**
     * 增加经验并重算等级
     */
    void addExp(Long userId, long amount);

    /**
     * 根据累计经验计算等级（0-6）
     */
    int computeLevel(long exp);

    /**
     * 积分流水分页
     */
    IPage<PointLog> pagePointLogs(IPage<PointLog> page, Long userId);
}
