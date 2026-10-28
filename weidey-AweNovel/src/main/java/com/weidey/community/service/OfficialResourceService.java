package com.weidey.community.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weidey.community.domain.OfficialResource;

import java.util.List;

/**
 * 正版资源服务
 *
 * @author weidey
 */
public interface OfficialResourceService extends IService<OfficialResource> {

    /**
     * 按游戏查询正版资源（公开，按 sort 升序）
     */
    List<OfficialResource> listByGame(Long gameId);
}
