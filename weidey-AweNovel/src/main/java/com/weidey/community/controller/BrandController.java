package com.weidey.community.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weidey.common.core.controller.BaseController;
import com.weidey.common.core.domain.AjaxResult;
import com.weidey.common.core.page.TableDataInfo;
import com.weidey.common.utils.StringUtils;
import com.weidey.community.domain.Brand;
import com.weidey.community.service.BrandService;
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
 * 制作会社接口
 *
 * @author weidey
 */
@RestController
@RequestMapping("/community/brand")
public class BrandController extends BaseController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    /** 会社列表（公开，全量；用于前台筛选与下拉） */
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Brand> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(keyword), Brand::getName, keyword)
                .orderByAsc(Brand::getBrandId);
        return success(brandService.list(wrapper));
    }

    /** 管理端会社列表（分页 + 名称模糊 + 关联游戏数） */
    @PreAuthorize("@ss.hasPermi('community:brand:list')")
    @GetMapping("/admin/list")
    public TableDataInfo adminList(@RequestParam(required = false) String keyword) {
        return getDataTable(brandService.pageBrands(startPage(), keyword));
    }

    /** 会社详情（管理） */
    @PreAuthorize("@ss.hasPermi('community:brand:list')")
    @GetMapping("/{brandId}")
    public AjaxResult detail(@PathVariable Long brandId) {
        Brand brand = brandService.getById(brandId);
        if (brand == null) {
            return error("会社不存在");
        }
        brand.setGameCount(brandService.countGames(brandId));
        return success(brand);
    }

    /** 新增会社（管理） */
    @PreAuthorize("@ss.hasPermi('community:brand:add')")
    @PostMapping
    public AjaxResult add(@RequestBody Brand brand) {
        if (StringUtils.isBlank(brand.getName())) {
            return error("会社名称不能为空");
        }
        if (brandService.count(new LambdaQueryWrapper<Brand>().eq(Brand::getName, brand.getName().trim())) > 0) {
            return error("会社名称已存在");
        }
        brand.setBrandId(null);
        brand.setName(brand.getName().trim());
        brandService.save(brand);
        return success(brand);
    }

    /** 修改会社（管理） */
    @PreAuthorize("@ss.hasPermi('community:brand:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody Brand brand) {
        if (brand.getBrandId() == null) {
            return error("会社ID不能为空");
        }
        if (StringUtils.isBlank(brand.getName())) {
            return error("会社名称不能为空");
        }
        long dup = brandService.count(new LambdaQueryWrapper<Brand>()
                .eq(Brand::getName, brand.getName().trim())
                .ne(Brand::getBrandId, brand.getBrandId()));
        if (dup > 0) {
            return error("会社名称已存在");
        }
        brand.setName(brand.getName().trim());
        return toAjax(brandService.updateById(brand));
    }

    /** 删除会社（管理）：存在关联游戏时拒绝 */
    @PreAuthorize("@ss.hasPermi('community:brand:remove')")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        List<Long> blocked = new ArrayList<>();
        for (Long id : ids) {
            long games = brandService.countGames(id);
            if (games > 0) {
                Brand brand = brandService.getById(id);
                blocked.add(id);
                if (brand != null) {
                    return error("会社「" + brand.getName() + "」下还有 " + games + " 个游戏，无法删除");
                }
            }
        }
        if (!blocked.isEmpty()) {
            return error("所选会社仍有关联游戏，无法删除");
        }
        return toAjax(brandService.removeByIds(Arrays.asList(ids)));
    }
}
