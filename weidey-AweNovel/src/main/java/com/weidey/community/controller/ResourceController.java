package com.weidey.community.controller;

import com.weidey.common.core.controller.BaseController;
import com.weidey.common.core.domain.AjaxResult;
import com.weidey.common.core.page.TableDataInfo;
import com.weidey.community.domain.Resource;
import com.weidey.community.service.ResourceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 资源接口
 *
 * @author weidey
 */
@RestController
@RequestMapping("/community/resource")
public class ResourceController extends BaseController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /** 资源列表（公开，仅审核通过） */
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) Long gameId) {
        return getDataTable(resourceService.pageResources(startPage(), gameId, null, "1"));
    }

    /** 发布资源（需登录，初始待审核） */
    @PostMapping
    public AjaxResult publish(@RequestBody Resource resource) {
        resource.setUserId(getUserId());
        return success(resourceService.publish(resource));
    }

    /** 下载资源（扣积分） */
    @PostMapping("/download/{resourceId}")
    public AjaxResult download(@PathVariable Long resourceId) {
        return success(resourceService.download(resourceId, getUserId()));
    }

    /** 失效举报 */
    @PostMapping("/report/{resourceId}")
    public AjaxResult report(@PathVariable Long resourceId) {
        resourceService.report(resourceId);
        return success();
    }
}
