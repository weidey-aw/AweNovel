package com.weidey.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.weidey.community.domain.Resource;

/**
 * 资源服务
 *
 * @author weidey
 */
public interface ResourceService extends IService<Resource> {

    /**
     * 分页查询资源
     */
    IPage<Resource> pageResources(IPage<Resource> page, Long gameId, Long userId, String status);

    /**
     * 发布资源（初始为待审核）
     */
    Resource publish(Resource resource);

    /**
     * 下载资源（扣积分 + 下载次数 +1，返回可下载的资源）
     */
    Resource download(Long resourceId, Long userId);

    /**
     * 失效举报 +1
     */
    void report(Long resourceId);
}
