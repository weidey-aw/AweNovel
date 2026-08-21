package com.weidey.community.controller;

import com.weidey.common.core.controller.BaseController;
import com.weidey.common.core.domain.AjaxResult;
import com.weidey.common.core.page.TableDataInfo;
import com.weidey.community.domain.Comment;
import com.weidey.community.service.CommentService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * 评论接口
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

    /** 评论列表（公开） */
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam String targetType, @RequestParam Long targetId) {
        return getDataTable(commentService.pageComments(startPage(), targetType, targetId));
    }

    /** 发表评论（需登录） */
    @PostMapping
    public AjaxResult add(@RequestBody Comment comment) {
        comment.setUserId(getUserId());
        return success(commentService.addComment(comment));
    }

    /** 删除评论（管理） */
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(commentService.removeByIds(Arrays.asList(ids)));
    }
}
