package com.weidey.community.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weidey.common.core.controller.BaseController;
import com.weidey.common.core.domain.AjaxResult;
import com.weidey.community.domain.Brand;
import com.weidey.community.service.BrandService;
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

    /** 会社列表（公开） */
    @GetMapping("/list")
    public AjaxResult list() {
        return success(brandService.list(new LambdaQueryWrapper<Brand>().orderByAsc(Brand::getBrandId)));
    }

    /** 新增会社（管理） */
    @PostMapping
    public AjaxResult add(@RequestBody Brand brand) {
        brandService.save(brand);
        return success(brand);
    }

    /** 修改会社（管理） */
    @PutMapping
    public AjaxResult edit(@RequestBody Brand brand) {
        return toAjax(brandService.updateById(brand));
    }

    /** 删除会社（管理） */
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(brandService.removeByIds(Arrays.asList(ids)));
    }
}
