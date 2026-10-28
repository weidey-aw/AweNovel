package com.weidey.community.controller;

import com.weidey.common.core.controller.BaseController;
import com.weidey.common.core.domain.AjaxResult;
import com.weidey.common.core.page.TableDataInfo;
import com.weidey.common.utils.StringUtils;
import com.weidey.community.domain.OfficialResource;
import com.weidey.community.domain.Resource;
import com.weidey.community.service.OfficialResourceService;
import com.weidey.community.service.ResourceService;
import com.weidey.system.service.ISysDictTypeService;
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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 游戏资源接口
 *
 * <p>资源 = 元信息（gal_resource） + 多条来源（gal_resource_source）；
 * 正版资源（gal_official_resource）与用户无关，按游戏展示在前台详情页顶部。</p>
 *
 * @author weidey
 */
@RestController
@RequestMapping("/community/resource")
public class ResourceController extends BaseController {

    /** 资源类型字典 */
    private static final String DICT_RESOURCE_TYPE = "game_resource_type";
    /** 正版资源类型字典 */
    private static final String DICT_OFFICIAL_TYPE = "game_official_type";

    private final ResourceService resourceService;
    private final OfficialResourceService officialResourceService;
    private final ISysDictTypeService dictTypeService;

    public ResourceController(ResourceService resourceService,
                              OfficialResourceService officialResourceService,
                              ISysDictTypeService dictTypeService) {
        this.resourceService = resourceService;
        this.officialResourceService = officialResourceService;
        this.dictTypeService = dictTypeService;
    }

    /* ==================== 前台（公开） ==================== */

    /** 资源列表（仅审核通过），支持按游戏与类型过滤 */
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) Long gameId,
                              @RequestParam(required = false) String type) {
        return getDataTable(resourceService.pageResources(startPage(), gameId, null, "1", type));
    }

    /** 资源类型字典（社区资源 / 正版资源） */
    @GetMapping("/types")
    public AjaxResult types() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("resource", dictTypeService.selectDictDataByType(DICT_RESOURCE_TYPE));
        data.put("official", dictTypeService.selectDictDataByType(DICT_OFFICIAL_TYPE));
        return success(data);
    }

    /** 正版资源列表（公开，按游戏） */
    @GetMapping("/official/list")
    public AjaxResult officialList(@RequestParam Long gameId) {
        return success(officialResourceService.listByGame(gameId));
    }

    /** 下载资源（登录，扣积分，返回含来源详情的资源） */
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

    /** 用户发布资源（登录，进入待审核） */
    @PostMapping
    public AjaxResult publish(@RequestBody Resource resource) {
        resource.setUserId(getUserId());
        return success(resourceService.publish(resource));
    }

    /* ==================== 管理端 ==================== */

    /** 管理端资源列表 */
    @PreAuthorize("@ss.hasPermi('community:resource:list')")
    @GetMapping("/admin/list")
    public TableDataInfo adminList(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) Long gameId,
                                   @RequestParam(required = false) String type,
                                   @RequestParam(required = false) String status) {
        return getDataTable(resourceService.pageAdminResources(startPage(), keyword, gameId, type, status));
    }

    /** 管理端资源详情（含来源） */
    @PreAuthorize("@ss.hasPermi('community:resource:list')")
    @GetMapping("/admin/{resourceId}")
    public AjaxResult adminDetail(@PathVariable Long resourceId) {
        Resource resource = resourceService.getResourceDetail(resourceId);
        if (resource == null) {
            return error("资源不存在");
        }
        return success(resource);
    }

    /** 管理端新增资源 */
    @PreAuthorize("@ss.hasPermi('community:resource:add')")
    @PostMapping("/admin")
    public AjaxResult adminAdd(@RequestBody Resource resource) {
        if (StringUtils.isBlank(resource.getTitle())) {
            return error("资源标题不能为空");
        }
        if (resource.getGameId() == null) {
            return error("请选择所属游戏");
        }
        if (resource.getUserId() == null) {
            resource.setUserId(getUserId());
        }
        return success(resourceService.saveResource(resource));
    }

    /** 管理端修改资源 */
    @PreAuthorize("@ss.hasPermi('community:resource:edit')")
    @PutMapping("/admin")
    public AjaxResult adminEdit(@RequestBody Resource resource) {
        if (resource.getResourceId() == null) {
            return error("资源ID不能为空");
        }
        return success(resourceService.updateResource(resource));
    }

    /** 管理端删除资源（级联删除来源） */
    @PreAuthorize("@ss.hasPermi('community:resource:remove')")
    @DeleteMapping("/admin/{resourceIds}")
    public AjaxResult adminRemove(@PathVariable Long[] resourceIds) {
        resourceService.deleteResources(Arrays.asList(resourceIds));
        return success("删除成功（已同步清理资源来源）");
    }

    /* ==================== 正版资源（管理端） ==================== */

    /** 新增正版资源 */
    @PreAuthorize("@ss.hasPermi('community:resource:edit')")
    @PostMapping("/official")
    public AjaxResult officialAdd(@RequestBody OfficialResource official) {
        if (official.getGameId() == null) {
            return error("请选择所属游戏");
        }
        if (StringUtils.isBlank(official.getTitle())) {
            return error("标题不能为空");
        }
        if (StringUtils.isBlank(official.getUrl())) {
            return error("官方链接不能为空");
        }
        official.setOfficialId(null);
        officialResourceService.save(official);
        return success(official);
    }

    /** 修改正版资源 */
    @PreAuthorize("@ss.hasPermi('community:resource:edit')")
    @PutMapping("/official")
    public AjaxResult officialEdit(@RequestBody OfficialResource official) {
        if (official.getOfficialId() == null) {
            return error("正版资源ID不能为空");
        }
        return toAjax(officialResourceService.updateById(official));
    }

    /** 删除正版资源 */
    @PreAuthorize("@ss.hasPermi('community:resource:edit')")
    @DeleteMapping("/official/{ids}")
    public AjaxResult officialRemove(@PathVariable Long[] ids) {
        return toAjax(officialResourceService.removeByIds(Arrays.asList(ids)));
    }
}
