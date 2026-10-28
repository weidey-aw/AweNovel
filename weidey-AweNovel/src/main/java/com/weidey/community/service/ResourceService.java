package com.weidey.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.weidey.community.domain.Resource;

import java.util.List;

/**
 * 资源服务
 *
 * @author weidey
 */
public interface ResourceService extends IService<Resource> {

    /**
     * 前台资源分页（仅审核通过，可按游戏/发布者/状态过滤）
     */
    IPage<Resource> pageResources(IPage<Resource> page, Long gameId, Long userId, String status, String type);

    /**
     * 管理端资源分页（关键词 / 游戏 / 类型 / 状态）
     */
    IPage<Resource> pageAdminResources(IPage<Resource> page, String keyword, Long gameId, String type, String status);

    /**
     * 资源详情（含来源列表、发布者信息、游戏名）
     */
    Resource getResourceDetail(Long resourceId);

    /**
     * 管理端新增资源（含来源）
     */
    Resource saveResource(Resource resource);

    /**
     * 管理端修改资源（sources 非 null 时全量覆盖来源）
     */
    Resource updateResource(Resource resource);

    /**
     * 删除资源（级联删除来源）
     */
    void deleteResources(List<Long> resourceIds);

    /**
     * 发布资源（初始为待审核，并启动审核流程）
     */
    Resource publish(Resource resource);

    /**
     * 下载资源（扣积分 + 下载次数 +1，返回含来源的资源详情）
     */
    Resource download(Long resourceId, Long userId);

    /**
     * 失效举报 +1
     */
    void report(Long resourceId);
}
