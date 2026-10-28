package com.weidey.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.weidey.community.domain.Tag;

import java.util.List;

/**
 * 标签服务
 *
 * @author weidey
 */
public interface TagService extends IService<Tag> {

    /**
     * 管理端分页查询（名称模糊 / 类型过滤），并填充关联游戏数
     */
    IPage<Tag> pageTags(IPage<Tag> page, String name, String type);

    /**
     * 统计该标签关联的游戏数量（游戏-标签 一对多）
     */
    long countGames(Long tagId);

    /**
     * 删除标签，同时清理游戏-标签关联
     */
    void deleteTagsCascade(List<Long> tagIds);
}
