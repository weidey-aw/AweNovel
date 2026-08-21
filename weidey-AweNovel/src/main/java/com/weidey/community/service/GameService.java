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
