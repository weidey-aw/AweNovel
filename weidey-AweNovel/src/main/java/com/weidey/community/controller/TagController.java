package com.weidey.community.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weidey.common.core.controller.BaseController;
import com.weidey.common.core.domain.AjaxResult;
import com.weidey.community.domain.Tag;
import com.weidey.community.service.TagService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * 标签接口
 *
 * @author weidey
 */
@RestController
@RequestMapping("/community/tag")
public class TagController extends BaseController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    /** 标签列表（公开） */
    @GetMapping("/list")
    public AjaxResult list() {
        return success(tagService.list(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getTagId)));
    }

    /** 新增标签（管理） */
    @PostMapping
    public AjaxResult add(@RequestBody Tag tag) {
        tagService.save(tag);
        return success(tag);
    }

    /** 修改标签（管理） */
    @PutMapping
    public AjaxResult edit(@RequestBody Tag tag) {
        return toAjax(tagService.updateById(tag));
    }

    /** 删除标签（管理） */
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(tagService.removeByIds(Arrays.asList(ids)));
    }
}
