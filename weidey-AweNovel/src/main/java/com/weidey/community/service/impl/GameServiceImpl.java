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
import com.weidey.community.domain.Tag;
import com.weidey.community.mapper.BrandMapper;
import com.weidey.community.mapper.GameMapper;
import com.weidey.community.mapper.GameTagMapper;
import com.weidey.community.mapper.TagMapper;
import com.weidey.community.service.GameService;
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

    @Override
    public IPage<Game> pageGames(IPage<Game> page, String keyword, Long brandId, Long tagId, String status) {
        LambdaQueryWrapper<Game> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.isNotBlank(status), Game::getStatus, status)
                .eq(brandId != null, Game::getBrandId, brandId)
                .and(StringUtils.isNotBlank(keyword), w -> w.like(Game::getTitle, keyword)
                        .or().like(Game::getTitleCn, keyword));
        if (tagId != null) {
            wrapper.inSql(Game::getGameId, "select game_id from gal_game_tag where tag_id = " + tagId);
        }
        wrapper.orderByDesc(Game::getRatingAvg).orderByDesc(Game::getGameId);
        Page<Game> result = (Page<Game>) baseMapper.selectPage((Page<Game>) page, wrapper);
        // 填充会社名
        for (Game game : result.getRecords()) {
            if (game.getBrandId() != null) {
                Brand brand = brandMapper.selectById(game.getBrandId());
                game.setBrandName(brand != null ? brand.getName() : null);
            }
        }
        return result;
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
