package com.weidey.community.controller;

import com.weidey.common.core.controller.BaseController;
import com.weidey.common.core.domain.AjaxResult;
import com.weidey.common.core.page.TableDataInfo;
import com.weidey.community.domain.UserFavorite;
import com.weidey.community.domain.UserProfile;
import com.weidey.community.service.FavoriteService;
import com.weidey.community.service.FollowService;
import com.weidey.community.service.MessageService;
import com.weidey.community.service.UserProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户中心接口
 *
 * @author weidey
 */
@RestController
@RequestMapping("/community/user")
public class UserCenterController extends BaseController {

    private final UserProfileService userProfileService;
    private final MessageService messageService;
    private final FavoriteService favoriteService;
    private final FollowService followService;

    public UserCenterController(UserProfileService userProfileService, MessageService messageService,
                                FavoriteService favoriteService, FollowService followService) {
        this.userProfileService = userProfileService;
        this.messageService = messageService;
        this.favoriteService = favoriteService;
        this.followService = followService;
    }

    /** 我的画像（积分/经验/等级） */
    @GetMapping("/profile")
    public AjaxResult profile() {
        UserProfile profile = userProfileService.getOrCreate(getUserId());
        return success(profile);
    }

    /** 积分流水 */
    @GetMapping("/points")
    public TableDataInfo points() {
        return getDataTable(userProfileService.pagePointLogs(startPage(), getUserId()));
    }

    /** 我的消息（含发送人、业务来源） */
    @GetMapping("/messages")
    public TableDataInfo messages() {
        return getDataTable(messageService.pageMessages(startPage(), getUserId()));
    }

    /** 未读消息数 */
    @GetMapping("/messages/unread")
    public AjaxResult unread() {
        return success(messageService.countUnread(getUserId()));
    }

    /** 标记消息已读 */
    @PostMapping("/message/read/{messageId}")
    public AjaxResult markRead(@PathVariable Long messageId) {
        messageService.markRead(getUserId(), messageId);
        return success();
    }

    /** 我的收藏（可按类型过滤：G游戏 / A文章 / R资源） */
    @GetMapping("/favorites")
    public TableDataInfo favorites(@RequestParam(required = false) String targetType) {
        return getDataTable(favoriteService.pageFavorites(startPage(), getUserId(), targetType));
    }

    /** 收藏 / 取消收藏（切换），返回最新状态 */
    @PostMapping("/favorite/{targetType}/{targetId}")
    public AjaxResult toggleFavorite(@PathVariable String targetType, @PathVariable Long targetId) {
        if (!"G".equals(targetType) && !"A".equals(targetType) && !"R".equals(targetType)) {
            return error("收藏类型不合法");
        }
        boolean favorited = favoriteService.toggle(getUserId(), targetType, targetId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("favorited", favorited);
        data.put("targetType", targetType);
        data.put("targetId", targetId);
        return success(data);
    }

    /** 收藏状态 */
    @GetMapping("/favorite/status")
    public AjaxResult favoriteStatus(@RequestParam String targetType, @RequestParam Long targetId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("favorited", favoriteService.isFavorited(getUserId(), targetType, targetId));
        return success(data);
    }

    /** 我的关注 */
    @GetMapping("/following")
    public TableDataInfo following() {
        return getDataTable(followService.pageFollowing(startPage(), getUserId()));
    }

    /** 关注/取关 */
    @PostMapping("/follow/{targetUserId}")
    public AjaxResult follow(@PathVariable Long targetUserId) {
        Long userId = getUserId();
        if (followService.isFollowing(userId, targetUserId)) {
            followService.unfollow(userId, targetUserId);
        } else {
            followService.follow(userId, targetUserId);
        }
        return success();
    }
}
