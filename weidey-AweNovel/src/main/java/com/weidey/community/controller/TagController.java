package com.weidey.community.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weidey.common.core.controller.BaseController;
import com.weidey.common.core.domain.AjaxResult;
import com.weidey.common.core.page.TableDataInfo;
import com.weidey.common.utils.StringUtils;
import com.weidey.community.domain.Tag;
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

import java.util.Arrays;
import java.util.List;

/**
 * 标签接口
 *
 * <p>标签与游戏为「多对多」关系（gal_game_tag 关联表），
 * 每个标签可关联多个游戏，每个游戏可有多个标签。</p>
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

    /** 标签列表（公开，全量；用于前台筛选与下拉） */
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) String name,
                           @RequestParam(required = false) String type) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(name), Tag::getName, name)
                .eq(StringUtils.isNotBlank(type), Tag::getType, type)
                .orderByAsc(Tag::getTagId);
        return success(tagService.list(wrapper));
    }

    /** 管理端标签列表（分页 + 名称模糊 + 类型 + 关联游戏数） */
    @PreAuthorize("@ss.hasPermi('community:tag:list')")
    @GetMapping("/admin/list")
    public TableDataInfo adminList(@RequestParam(required = false) String name,
                                   @RequestParam(required = false) String type) {
        return getDataTable(tagService.pageTags(startPage(), name, type));
    }

    /** 标签详情（管理） */
    @PreAuthorize("@ss.hasPermi('community:tag:list')")
    @GetMapping("/{tagId}")
    public AjaxResult detail(@PathVariable Long tagId) {
        Tag tag = tagService.getById(tagId);
        if (tag == null) {
            return error("标签不存在");
        }
        tag.setGameCount(tagService.countGames(tagId));
        return success(tag);
    }

    /** 新增标签（管理） */
    @PreAuthorize("@ss.hasPermi('community:tag:add')")
    @PostMapping
    public AjaxResult add(@RequestBody Tag tag) {
        if (StringUtils.isBlank(tag.getName())) {
            return error("标签名称不能为空");
        }
        if (tagService.count(new LambdaQueryWrapper<Tag>().eq(Tag::getName, tag.getName().trim())) > 0) {
            return error("标签名称已存在");
        }
        tag.setTagId(null);
        tag.setName(tag.getName().trim());
        tagService.save(tag);
        return success(tag);
    }

    /** 修改标签（管理） */
    @PreAuthorize("@ss.hasPermi('community:tag:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody Tag tag) {
        if (tag.getTagId() == null) {
            return error("标签ID不能为空");
        }
        if (StringUtils.isBlank(tag.getName())) {
            return error("标签名称不能为空");
        }
        long dup = tagService.count(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getName, tag.getName().trim())
                .ne(Tag::getTagId, tag.getTagId()));
        if (dup > 0) {
            return error("标签名称已存在");
        }
        tag.setName(tag.getName().trim());
        return toAjax(tagService.updateById(tag));
    }

    /** 删除标签（管理）：同时清理游戏-标签关联 */
    @PreAuthorize("@ss.hasPermi('community:tag:remove')")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        List<Long> tagIds = Arrays.asList(ids);
        tagService.deleteTagsCascade(tagIds);
        return success("删除成功（已同步清理游戏标签关联）");
    }
}
