package com.weidey.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weidey.community.domain.SignRecord;
import com.weidey.community.domain.UserProfile;
import com.weidey.community.mapper.SignRecordMapper;
import com.weidey.community.service.SignService;
import com.weidey.community.service.UserProfileService;
import com.weidey.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 签到服务实现
 *
 * @author weidey
 */
@Service
public class SignServiceImpl extends ServiceImpl<SignRecordMapper, SignRecord> implements SignService {

    /** 每日签到固定奖励 */
    private static final int SIGN_POINTS = 5;
    private static final int SIGN_EXP = 5;

    @Autowired
    private UserProfileService userProfileService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> doSign(Long userId) {
        LocalDate today = LocalDate.now();
        Date todayDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());

        // 今日是否已签到
        Long count = count(new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getUserId, userId)
                .eq(SignRecord::getSignDate, todayDate));
        if (count != null && count > 0) {
            throw new ServiceException("今日已签到");
        }

        // 连续签到天数（查昨日记录）
        int continuous = 1;
        LocalDate yesterday = today.minusDays(1);
        Date yesterdayDate = Date.from(yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant());
        SignRecord yesterdayRecord = getOne(new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getUserId, userId)
                .eq(SignRecord::getSignDate, yesterdayDate), false);
        if (yesterdayRecord != null) {
            continuous = yesterdayRecord.getContinuousDays() + 1;
        }

        // 记录签到
        SignRecord record = new SignRecord();
        record.setUserId(userId);
        record.setSignDate(todayDate);
        record.setContinuousDays(continuous);
        record.setPointsAward(SIGN_POINTS);
        record.setExpAward(SIGN_EXP);
        record.setCreateTime(new Date());
        save(record);

        // 发放积分与经验
        userProfileService.addPoints(userId, SIGN_POINTS, "signin", "sign", record.getId(), "每日签到");
        userProfileService.addExp(userId, SIGN_EXP);

        UserProfile profile = userProfileService.getById(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("continuous", continuous);
        result.put("points", SIGN_POINTS);
        result.put("exp", SIGN_EXP);
        result.put("level", profile != null ? profile.getLevel() : 0);
        result.put("totalPoints", profile != null ? profile.getPoints() : 0L);
        return result;
    }
}
