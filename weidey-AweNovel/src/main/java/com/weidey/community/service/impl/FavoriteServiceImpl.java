package com.weidey.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weidey.common.utils.StringUtils;
import com.weidey.community.domain.Article;
import com.weidey.community.domain.Game;
import com.weidey.community.domain.Resource;
import com.weidey.community.domain.UserFavorite;
import com.weidey.community.mapper.ArticleMapper;
import com.weidey.community.mapper.GameMapper;
import com.weidey.community.mapper.ResourceMapper;
import com.weidey.community.mapper.UserFavoriteMapper;
import com.weidey.community.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 收藏服务实现
 *
 * @author weidey
 */
@Service
public class FavoriteServiceImpl extends ServiceImpl<UserFavoriteMapper, UserFavorite> implements FavoriteService {

    @Autowired
    private GameMapper gameMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ResourceMapper resourceMapper;

    @Override
    public void favorite(Long userId, String targetType, Long targetId) {
        if (isFavorited(userId, targetType, targetId)) {
            return;
        }
        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setTargetType(targetType);
        favorite.setTargetId(targetId);
        favorite.setCreateTime(new Date());
        save(favorite);
    }

    @Override
    public void unfavorite(Long userId, String targetType, Long targetId) {
        remove(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getTargetType, targetType)
                .eq(UserFavorite::getTargetId, targetId));
    }

    @Override
    public boolean toggle(Long userId, String targetType, Long targetId) {
        if (isFavorited(userId, targetType, targetId)) {
            unfavorite(userId, targetType, targetId);
            return false;
        }
        favorite(userId, targetType, targetId);
        return true;
    }

    @Override
    public boolean isFavorited(Long userId, String targetType, Long targetId) {
        return count(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getTargetType, targetType)
                .eq(UserFavorite::getTargetId, targetId)) > 0;
    }

    @Override
    public IPage<UserFavorite> pageFavorites(IPage<UserFavorite> page, Long userId, String targetType) {
        Page<UserFavorite> result = (Page<UserFavorite>) baseMapper.selectPage((Page<UserFavorite>) page,
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .eq(StringUtils.isNotBlank(targetType), UserFavorite::getTargetType, targetType)
                        .orderByDesc(UserFavorite::getId));
        fillTargets(result.getRecords());
        return result;
    }

    /** 批量补全收藏目标的标题 / 封面 / 跳转地址 */
    private void fillTargets(List<UserFavorite> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<UserFavorite> gameFavs = rows.stream().filter(f -> "G".equals(f.getTargetType())).toList();
        List<UserFavorite> articleFavs = rows.stream().filter(f -> "A".equals(f.getTargetType())).toList();
        List<UserFavorite> resourceFavs = rows.stream().filter(f -> "R".equals(f.getTargetType())).toList();

        Map<Long, Game> gameMap = idsOf(gameFavs).isEmpty() ? Collections.emptyMap()
                : gameMapper.selectBatchIds(idsOf(gameFavs)).stream()
                .collect(Collectors.toMap(Game::getGameId, g -> g, (a, b) -> a));
        Map<Long, Article> articleMap = idsOf(articleFavs).isEmpty() ? Collections.emptyMap()
                : articleMapper.selectBatchIds(idsOf(articleFavs)).stream()
                .collect(Collectors.toMap(Article::getArticleId, a -> a, (a, b) -> a));
        Map<Long, Resource> resourceMap = idsOf(resourceFavs).isEmpty() ? Collections.emptyMap()
                : resourceMapper.selectBatchIds(idsOf(resourceFavs)).stream()
                .collect(Collectors.toMap(Resource::getResourceId, r -> r, (a, b) -> a));

        for (UserFavorite favorite : rows) {
            if ("G".equals(favorite.getTargetType())) {
                favorite.setTypeLabel("游戏");
                Game game = gameMap.get(favorite.getTargetId());
                if (game != null) {
                    favorite.setTitle(StringUtils.isNotBlank(game.getTitleCn()) ? game.getTitleCn() : game.getTitle());
                    favorite.setSubtitle(game.getTitle());
                    favorite.setCover(game.getCover());
                }
                favorite.setUrl("/game/" + favorite.getTargetId());
            } else if ("A".equals(favorite.getTargetType())) {
                favorite.setTypeLabel("文章");
                Article article = articleMap.get(favorite.getTargetId());
                if (article != null) {
                    favorite.setTitle(article.getTitle());
                    favorite.setCover(article.getCover());
                    favorite.setSubtitle(categoryLabel(article.getCategory()));
                }
                favorite.setUrl("/article/" + favorite.getTargetId());
            } else {
                favorite.setTypeLabel("资源");
                Resource resource = resourceMap.get(favorite.getTargetId());
                if (resource != null) {
                    favorite.setTitle(resource.getTitle());
                    favorite.setSubtitle(StringUtils.isNotBlank(resource.getSize()) ? resource.getSize() : "");
                }
                favorite.setUrl("/game/" + (resource != null ? resource.getGameId() : favorite.getTargetId()));
            }
        }
    }

    private List<Long> idsOf(List<UserFavorite> rows) {
        return rows.stream().map(UserFavorite::getTargetId).filter(java.util.Objects::nonNull).distinct().toList();
    }

    private String categoryLabel(String category) {
        if (category == null) {
            return "文章";
        }
        return switch (category) {
            case "news" -> "资讯";
            case "review" -> "评测";
            case "guide" -> "攻略";
            default -> category;
        };
    }
}
