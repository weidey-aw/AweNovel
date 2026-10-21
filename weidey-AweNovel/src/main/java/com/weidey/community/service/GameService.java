package com.weidey.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.weidey.community.domain.Game;
import com.weidey.community.domain.Tag;

import java.util.List;

/**
 * 游戏条目服务
 *
 * @author weidey
 */
public interface GameService extends IService<Game> {

    /**
     * 分页查询游戏（关键词/会社/标签过滤）
     */
    IPage<Game> pageGames(IPage<Game> page, String keyword, Long brandId, Long tagId, String status);

    /**
     * 管理端分页查询（全状态 + 发售日期区间 + 标签多选/会社）
     *
     * @param tagIds    标签ID集合（可空）
     * @param beginDate 发售日期起（yyyy-MM-dd，可空）
     * @param endDate   发售日期止（yyyy-MM-dd，可空）
     */
    IPage<Game> pageAdminGames(IPage<Game> page, String keyword, Long brandId, List<Long> tagIds,
                               String status, String beginDate, String endDate);

    /**
     * 提交发布：状态置为待审核(2) 并启动审核流程
     */
    void publishGame(Long gameId);

    /**
     * 撤回审核：状态回到草稿(4) 并取消该游戏的审核流程
     */
    void withdrawGame(Long gameId);

    /**
     * 是否已在审核队列中（存在运行中的流程实例）
     */
    boolean hasPendingReview(Long gameId);

    /**
     * 游戏详情（含会社名与标签）
     */
    Game getGameDetail(Long gameId);

    /**
     * 查询游戏标签
     */
    List<Tag> getGameTags(Long gameId);

    /**
     * 保存游戏-标签关联
     */
    void saveGameTags(Long gameId, List<Long> tagIds);

    /**
     * 浏览量 +1
     */
    void incrementView(Long gameId);
}
