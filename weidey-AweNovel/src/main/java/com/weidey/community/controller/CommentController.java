package com.weidey.community.controller;

import com.weidey.common.core.controller.BaseController;
import com.weidey.common.core.domain.AjaxResult;
import com.weidey.common.core.page.TableDataInfo;
import com.weidey.common.utils.StringUtils;
import com.weidey.community.domain.Comment;
import com.weidey.community.service.CommentService;
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

import java.util.Arrays;
import java.util.List;

/**
 * 评论接口
 *
 * <p>targetType：G 游戏 / A 文章 / R 资源；status：1 正常 / 0 隐藏。</p>
 *
 * @author weidey
 */
@RestController
@RequestMapping("/community/comment")
public class CommentController extends BaseController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /* ==================== 前台 ==================== */

    /** 评论列表（公开，仅正常状态） */
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam String targetType, @RequestParam Long targetId) {
        return getDataTable(commentService.pageComments(startPage(), targetType, targetId));
    }

    /** 发表评论（登录） */
    @PostMapping
    public AjaxResult add(@RequestBody Comment comment) {
        if (StringUtils.isBlank(comment.getContent())) {
            return error("评论内容不能为空");
        }
        if (StringUtils.isBlank(comment.getTargetType()) || comment.getTargetId() == null) {
            return error("评论目标不能为空");
        }
        comment.setUserId(getUserId());
        return success(commentService.addComment(comment));
    }

    /** 删除自己的评论（登录，仅限本人） */
    @DeleteMapping("/own/{commentId}")
    public AjaxResult removeOwn(@PathVariable Long commentId) {
        Comment comment = commentService.getById(commentId);
        if (comment == null) {
            return error("评论不存在");
        }
        if (!getUserId().equals(comment.getUserId())) {
            return error("只能删除自己的评论");
        }
        commentService.deleteComments(List.of(commentId));
        return success();
    }

    /* ==================== 管理端 ==================== */

    /** 管理端评论列表 */
    @PreAuthorize("@ss.hasPermi('community:comment:list')")
    @GetMapping("/admin/list")
    public TableDataInfo adminList(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) String targetType,
                                   @RequestParam(required = false) Long targetId,
                                   @RequestParam(required = false) String status) {
        return getDataTable(commentService.pageAdminComments(startPage(), keyword, targetType, targetId, status));
    }

    /** 管理端评论详情 */
    @PreAuthorize("@ss.hasPermi('community:comment:list')")
    @GetMapping("/admin/{commentId}")
    public AjaxResult adminDetail(@PathVariable Long commentId) {
        Comment comment = commentService.getCommentDetail(commentId);
        if (comment == null) {
            return error("评论不存在");
        }
        return success(comment);
    }

    /** 修改评论状态（显示 / 隐藏） */
    @PreAuthorize("@ss.hasPermi('community:comment:edit')")
    @PutMapping("/admin/status")
    public AjaxResult adminStatus(@RequestBody Comment comment) {
        if (comment.getCommentId() == null || StringUtils.isBlank(comment.getStatus())) {
            return error("参数不完整");
        }
        commentService.updateStatus(comment.getCommentId(), comment.getStatus());
        return success();
    }

    /** 管理端删除评论（含子回复） */
    @PreAuthorize("@ss.hasPermi('community:comment:remove')")
    @DeleteMapping("/admin/{commentIds}")
    public AjaxResult adminRemove(@PathVariable Long[] commentIds) {
        commentService.deleteComments(Arrays.asList(commentIds));
        return success("删除成功");
    }
}
