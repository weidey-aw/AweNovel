package com.weidey.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weidey.common.utils.StringUtils;
import com.weidey.community.domain.Brand;
import com.weidey.community.domain.Game;
import com.weidey.community.domain.GameTag;
import com.weidey.community.domain.Resource;
import com.weidey.community.domain.Tag;
import com.weidey.community.mapper.BrandMapper;
import com.weidey.community.mapper.GameMapper;
import com.weidey.community.mapper.GameTagMapper;
import com.weidey.community.mapper.ResourceMapper;
import com.weidey.community.mapper.TagMapper;
import com.weidey.community.service.GameService;
import com.weidey.community.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 游戏条目服务实现
 *
 * @author weidey
 */
@Service
public class GameServiceImpl extends ServiceImpl<GameMapper, Game> implements GameService {

    @Autowired
    private BrandMapper brandMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private GameTagMapper gameTagMapper;

    @Autowired
    private ResourceMapper resourceMapper;

    @Autowired
    private ReviewService reviewService;

    @Override
    public IPage<Game> pageGames(IPage<Game> page, String keyword, Long brandId, List<Long> tagIds,
                                 String status, String sortBy, boolean isAsc) {
        LambdaQueryWrapper<Game> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.isNotBlank(status), Game::getStatus, status)
                .eq(brandId != null, Game::getBrandId, brandId)
                .and(StringUtils.isNotBlank(keyword), w -> w.like(Game::getTitle, keyword)
                        .or().like(Game::getTitleCn, keyword));
        if (tagIds != null && !tagIds.isEmpty()) {
            String idList = tagIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            wrapper.inSql(Game::getGameId, "select game_id from gal_game_tag where tag_id in (" + idList + ")");
        }
        // 排序：hot 点击量 / download 资源下载总量 / rating 评分 / publish 发布时间 / release 发售时间
        String direction = isAsc ? "asc" : "desc";
        String orderBy;
        if ("download".equalsIgnoreCase(sortBy)) {
            orderBy = "(select ifnull(sum(r.download_count), 0) from gal_resource r "
                    + "where r.game_id = gal_game.game_id and r.del_flag = '0') " + direction;
        } else if ("rating".equalsIgnoreCase(sortBy)) {
            orderBy = "rating_avg " + direction;
        } else if ("publish".equalsIgnoreCase(sortBy)) {
            orderBy = "create_time " + direction;
        } else if ("release".equalsIgnoreCase(sortBy)) {
            orderBy = "release_date " + direction;
        } else {
            // 默认：热门（点击量）
            orderBy = "view_count " + direction;
        }
        wrapper.last("order by " + orderBy + ", game_id desc");
        Page<Game> result = (Page<Game>) baseMapper.selectPage((Page<Game>) page, wrapper);
        fillRows(result.getRecords());
        fillDownloadCounts(result.getRecords());
        return result;
    }

    /** 汇总每个游戏下资源的下载次数 */
    private void fillDownloadCounts(List<Game> games) {
        if (games == null || games.isEmpty()) {
            return;
        }
        List<Long> ids = games.stream().map(Game::getGameId).collect(Collectors.toList());
        List<Resource> resources = resourceMapper.selectList(new LambdaQueryWrapper<Resource>()
                .select(Resource::getGameId, Resource::getDownloadCount)
                .in(Resource::getGameId, ids));
        java.util.Map<Long, Integer> sums = resources.stream()
                .filter(r -> r.getGameId() != null)
                .collect(Collectors.groupingBy(Resource::getGameId,
                        Collectors.summingInt(r -> r.getDownloadCount() == null ? 0 : r.getDownloadCount())));
        for (Game game : games) {
            game.setDownloadCount(sums.getOrDefault(game.getGameId(), 0));
        }
    }

    @Override
    public IPage<Game> pageAdminGames(IPage<Game> page, String keyword, Long brandId, List<Long> tagIds,
                                      String status, String beginDate, String endDate) {
        LambdaQueryWrapper<Game> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.isNotBlank(status), Game::getStatus, status)
                .eq(brandId != null, Game::getBrandId, brandId)
                .and(StringUtils.isNotBlank(keyword), w -> w.like(Game::getTitle, keyword)
                        .or().like(Game::getTitleCn, keyword));
        if (tagIds != null && !tagIds.isEmpty()) {
            String idList = tagIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            wrapper.inSql(Game::getGameId, "select game_id from gal_game_tag where tag_id in (" + idList + ")");
        }
        if (StringUtils.isNotBlank(beginDate)) {
            wrapper.ge(Game::getReleaseDate, beginDate);
        }
        if (StringUtils.isNotBlank(endDate)) {
            wrapper.le(Game::getReleaseDate, endDate);
        }
        wrapper.orderByDesc(Game::getUpdateTime).orderByDesc(Game::getGameId);
        Page<Game> result = (Page<Game>) baseMapper.selectPage((Page<Game>) page, wrapper);
        fillRows(result.getRecords());
        return result;
    }

    /** 填充会社名与标签列表 */
    private void fillRows(List<Game> games) {
        if (games == null || games.isEmpty()) {
            return;
        }
        for (Game game : games) {
            if (game.getBrandId() != null) {
                Brand brand = brandMapper.selectById(game.getBrandId());
                game.setBrandName(brand != null ? brand.getName() : null);
            }
            game.setTags(getGameTags(game.getGameId()));
        }
    }

    @Override
    public void publishGame(Long gameId) {
        if (hasPendingReview(gameId)) {
            throw new com.weidey.common.exception.ServiceException("该游戏已在审核队列中，请先撤回后再发布");
        }
        update(new LambdaUpdateWrapper<Game>()
                .set(Game::getStatus, "2")
                .eq(Game::getGameId, gameId));
        reviewService.startReview("game", gameId);
    }

    @Override
    public void withdrawGame(Long gameId) {
        reviewService.cancelReview("game", gameId);
        update(new LambdaUpdateWrapper<Game>()
                .set(Game::getStatus, "4")
                .eq(Game::getGameId, gameId));
    }

    @Override
    public boolean hasPendingReview(Long gameId) {
        return reviewService.hasPendingReview("game", gameId);
    }

    @Override
    public Game getGameDetail(Long gameId) {
        Game game = getById(gameId);
        if (game == null) {
            return null;
        }
        if (game.getBrandId() != null) {
            Brand brand = brandMapper.selectById(game.getBrandId());
            game.setBrandName(brand != null ? brand.getName() : null);
        }
        game.setTags(getGameTags(gameId));
        return game;
    }

    @Override
    public List<Tag> getGameTags(Long gameId) {
        List<GameTag> gameTags = gameTagMapper.selectList(
                new LambdaQueryWrapper<GameTag>().eq(GameTag::getGameId, gameId));
        if (gameTags == null || gameTags.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> tagIds = gameTags.stream().map(GameTag::getTagId).collect(Collectors.toList());
        return tagMapper.selectBatchIds(tagIds);
    }

    @Override
    public void saveGameTags(Long gameId, List<Long> tagIds) {
        // 先删除旧关联
        gameTagMapper.delete(new LambdaQueryWrapper<GameTag>().eq(GameTag::getGameId, gameId));
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : tagIds) {
            gameTagMapper.insert(new GameTag(gameId, tagId));
        }
    }

    @Override
    public void incrementView(Long gameId) {
        update(new LambdaUpdateWrapper<Game>()
                .setSql("view_count = view_count + 1")
                .eq(Game::getGameId, gameId));
    }
}
