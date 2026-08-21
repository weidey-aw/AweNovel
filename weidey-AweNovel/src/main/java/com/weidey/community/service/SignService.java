package com.weidey.community.service;

import java.util.Map;

/**
 * 签到服务
 *
 * @author weidey
 */
public interface SignService {

    /**
     * 每日签到（获得 5 积分 + 5 经验）
     *
     * @return 签到结果（continuous/total/points/exp/level）
     */
    Map<String, Object> doSign(Long userId);
}
