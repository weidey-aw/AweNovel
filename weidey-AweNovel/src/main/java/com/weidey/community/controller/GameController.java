package com.weidey.community.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weidey.common.core.controller.BaseController;
import com.weidey.common.core.domain.AjaxResult;
import com.weidey.common.core.page.TableDataInfo;
import com.weidey.common.utils.StringUtils;
import com.weidey.community.domain.Game;
import com.weidey.community.domain.Tag;
import com.weidey.community.service.GameService;
import com.weidey.community.service.TagService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 游戏条目接口
 *
 * <p>状态：0 已下架、1 已发布、2 待审核、3 审核驳回、4 草稿；前台仅展示 1。</p>
 *
 * @author weidey
 */
@RestController
@RequestMapping("/community/game")
public class GameController extends BaseController {

    /** 前台可见状态 */
    private static final String STATUS_PUBLISHED = "1";
    /** 已下架 */
    private static final String STATUS_OFFLINE = "0";
    /** 待审核 */
    private static final String STATUS_PENDING = "2";
    /** 草稿 */
    private static final String STATUS_DRAFT = "4";
    /** 游戏截图：可不上传，上传则至少 3 张（前台一屏 3 张），最多 7 张 */
    private static final int MIN_SCREENSHOTS = 3;
    private static final int MAX_SCREENSHOTS = 7;

    private final GameService gameService;
    private final TagService tagService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public GameController(GameService gameService, TagService tagService,
                          com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.gameService = gameService;
        this.tagService = tagService;
        this.objectMapper = objectMapper;
    }

    /**
     * 前台游戏列表（仅已发布，公开）
     *
     * @param tagIds 多标签（逗号分隔，与其他标签取并集）
     * @param sortBy hot 点击量 / download 下载量 / rating 评分 / publish 发布时间 / release 发售时间
     * @param isAsc  是否升序，默认降序
     */
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String keyword,
                              @RequestParam(required = false) Long brandId,
                              @RequestParam(required = false) Long tagId,
                              @RequestParam(required = false) String tagIds,
                              @RequestParam(required = false) String sortBy,
                              @RequestParam(required = false) Boolean isAsc) {
        return getDataTable(gameService.pageGames(startPage(), keyword, brandId,
                parseTagIds(tagIds, tagId), STATUS_PUBLISHED, sortBy, Boolean.TRUE.equals(isAsc)));
    }

    /**
     * 管理端游戏列表：全状态，支持 关键词 / 会社 / 标签(多选) / 发售日期区间 / 状态
     */
    @PreAuthorize("@ss.hasPermi('community:game:list')")
    @GetMapping("/admin/list")
    public TableDataInfo adminList(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) Long brandId,
                                   @RequestParam(required = false) Long tagId,
                                   @RequestParam(required = false) String tagIds,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) String beginDate,
                                   @RequestParam(required = false) String endDate) {
        return getDataTable(gameService.pageAdminGames(startPage(), keyword, brandId,
                parseTagIds(tagIds, tagId), status, beginDate, endDate));
    }

    /** 兼容单选标签与多选标签两种入参 */
    private List<Long> parseTagIds(String tagIds, Long tagId) {
        List<Long> ids = new ArrayList<>();
        if (StringUtils.isNotBlank(tagIds)) {
            for (String part : tagIds.split(",")) {
                if (StringUtils.isNotBlank(part)) {
                    try {
                        ids.add(Long.valueOf(part.trim()));
                    } catch (NumberFormatException ignored) {
                        // 忽略非法标签ID
                    }
                }
            }
        } else if (tagId != null) {
            ids.add(tagId);
        }
        return ids;
    }

    /** 游戏详情（公开，浏览量 +1） */
    @GetMapping("/{gameId}")
    public AjaxResult detail(@PathVariable Long gameId) {
        Game game = gameService.getGameDetail(gameId);
        if (game == null) {
            return error("游戏不存在");
        }
        if (STATUS_PUBLISHED.equals(game.getStatus())) {
            gameService.incrementView(gameId);
        }
        return success(game);
    }

    /** 管理端游戏详情（含草稿/待审核，不增加浏览量） */
    @PreAuthorize("@ss.hasPermi('community:game:list')")
    @GetMapping("/admin/{gameId}")
    public AjaxResult adminDetail(@PathVariable Long gameId) {
        Game game = gameService.getGameDetail(gameId);
        if (game == null) {
            return error("游戏不存在");
        }
        return success(game);
    }

    /** 全部标签（公开） */
    @GetMapping("/tag/all")
    public AjaxResult tags() {
        return success(tagService.list(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getTagId)));
    }

    /** 新增游戏（管理）：不传 status 时按草稿保存 */
    @PreAuthorize("@ss.hasPermi('community:game:add')")
    @PostMapping
    public AjaxResult add(@RequestBody Game game) {
        if (StringUtils.isBlank(game.getStatus())) {
            game.setStatus(STATUS_DRAFT);
        }
        gameService.save(game);
        // 仅在显式传了 tagIds 时才覆盖标签关联（避免部分字段更新误删标签）
        if (game.getTagIds() != null) {
            gameService.saveGameTags(game.getGameId(), game.getTagIds());
        }
        return success(game);
    }

    /** 修改游戏（管理） */
    @PreAuthorize("@ss.hasPermi('community:game:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody Game game) {
        if (game.getGameId() == null) {
            return error("游戏ID不能为空");
        }
        // 审核中的游戏不允许修改（否则状态与流程会不一致：状态被改成草稿但流程仍挂着）
        if (gameService.hasPendingReview(game.getGameId())) {
            return error("该游戏正在审核中，请先撤回后再修改");
        }
        gameService.updateById(game);
        if (game.getTagIds() != null) {
            gameService.saveGameTags(game.getGameId(), game.getTagIds());
        }
        return success(game);
    }

    /**
     * 提交发布：状态置为「待审核」并启动审核流程，审核通过后前台可见
     */
    @PreAuthorize("@ss.hasPermi('community:game:publish')")
    @PostMapping("/publish/{gameId}")
    public AjaxResult publish(@PathVariable Long gameId) {
        Game game = gameService.getById(gameId);
        if (game == null) {
            return error("游戏不存在");
        }
        if (StringUtils.isBlank(game.getTitle())) {
            return error("请先填写游戏名称");
        }
        if (StringUtils.isBlank(game.getCover())) {
            return error("请先上传封面图");
        }
        if (STATUS_PENDING.equals(game.getStatus())) {
            return error("该游戏已提交审核，请等待审核结果");
        }
        if (gameService.hasPendingReview(gameId)) {
            return error("该游戏已在审核队列中，请先撤回后再发布");
        }
        // 游戏截图（carousel 字段）要么不传，要么至少 3 张（前台按 3 张一屏展示）
        int shots = countJsonArray(game.getCarousel());
        if (shots < 0) {
            return error("游戏截图数据格式不正确");
        }
        if (shots > 0 && shots < MIN_SCREENSHOTS) {
            return error("游戏截图至少上传 " + MIN_SCREENSHOTS + " 张（或不上传）");
        }
        if (shots > MAX_SCREENSHOTS) {
            return error("游戏截图最多 " + MAX_SCREENSHOTS + " 张");
        }
        gameService.publishGame(gameId);
        return success("已提交审核");
    }

    /**
     * 下架：已发布 → 已下架（前台立即不可见），可再次发布（走审核）
     */
    @PreAuthorize("@ss.hasPermi('community:game:publish')")
    @PostMapping("/offline/{gameId}")
    public AjaxResult offline(@PathVariable Long gameId) {
        Game game = gameService.getById(gameId);
        if (game == null) {
            return error("游戏不存在");
        }
        if (!STATUS_PUBLISHED.equals(game.getStatus())) {
            return error("只有已发布的游戏可以下架");
        }
        Game update = new Game();
        update.setGameId(gameId);
        update.setStatus(STATUS_OFFLINE);
        gameService.updateById(update);
        return success("已下架");
    }

    /**
     * 统计 JSON 数组元素个数；空串返回 0，格式错误返回 -1
     */
    private int countJsonArray(String json) {
        if (StringUtils.isBlank(json)) {
            return 0;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(json);
            return node.isArray() ? node.size() : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 撤回审核：待审核 → 草稿，并取消审核流程（撤回后可重新编辑并发布）
     */
    @PreAuthorize("@ss.hasPermi('community:game:publish')")
    @PostMapping("/withdraw/{gameId}")
    public AjaxResult withdraw(@PathVariable Long gameId) {
        Game game = gameService.getById(gameId);
        if (game == null) {
            return error("游戏不存在");
        }
        if (!STATUS_PENDING.equals(game.getStatus()) && !gameService.hasPendingReview(gameId)) {
            return error("只有待审核的游戏可以撤回");
        }
        gameService.withdrawGame(gameId);
        return success("已撤回为草稿");
    }

    /** 删除游戏（管理） */
    @PreAuthorize("@ss.hasPermi('community:game:remove')")
    @DeleteMapping("/{gameIds}")
    public AjaxResult remove(@PathVariable Long[] gameIds) {
        return toAjax(gameService.removeByIds(Arrays.asList(gameIds)));
    }
}
