package com.weidey.community.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weidey.common.core.controller.BaseController;
import com.weidey.common.core.domain.AjaxResult;
import com.weidey.common.core.page.TableDataInfo;
import com.weidey.community.domain.Game;
import com.weidey.community.domain.Tag;
import com.weidey.community.service.GameService;
import com.weidey.community.service.TagService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 游戏条目接口
 *
 * @author weidey
 */
@RestController
@RequestMapping("/community/game")
public class GameController extends BaseController {

    private final GameService gameService;
    private final TagService tagService;

    public GameController(GameService gameService, TagService tagService) {
        this.gameService = gameService;
        this.tagService = tagService;
    }

    /** 游戏列表（公开） */
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String keyword,
                              @RequestParam(required = false) Long brandId,
                              @RequestParam(required = false) Long tagId) {
        return getDataTable(gameService.pageGames(startPage(), keyword, brandId, tagId, "1"));
    }

    /** 游戏详情（公开） */
    @GetMapping("/{gameId}")
    public AjaxResult detail(@PathVariable Long gameId) {
        Game game = gameService.getGameDetail(gameId);
        if (game == null) {
            return error("游戏不存在");
        }
        gameService.incrementView(gameId);
        return success(game);
    }

    /** 全部标签（公开） */
    @GetMapping("/tag/all")
    public AjaxResult tags() {
        return success(tagService.list(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getTagId)));
    }

    /** 新增游戏（管理） */
    @PostMapping
    public AjaxResult add(@RequestBody Game game) {
        game.setStatus("1");
        gameService.save(game);
        gameService.saveGameTags(game.getGameId(), game.getTagIds());
        return success(game);
    }

    /** 修改游戏（管理） */
    @PutMapping
    public AjaxResult edit(@RequestBody Game game) {
        gameService.updateById(game);
        gameService.saveGameTags(game.getGameId(), game.getTagIds());
        return success(game);
    }

    /** 删除游戏（管理） */
    @DeleteMapping("/{gameIds}")
    public AjaxResult remove(@PathVariable Long[] gameIds) {
        return toAjax(gameService.removeByIds(Arrays.asList(gameIds)));
    }
}
