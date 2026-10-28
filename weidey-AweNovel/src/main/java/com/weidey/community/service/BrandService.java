package com.weidey.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.weidey.community.domain.Brand;

/**
 * 制作会社服务
 *
 * @author weidey
 */
public interface BrandService extends IService<Brand> {

    /**
     * 管理端分页查询（按名称模糊），并填充关联游戏数
     */
    IPage<Brand> pageBrands(IPage<Brand> page, String keyword);

    /**
     * 统计该会社下的游戏数量
     */
    long countGames(Long brandId);
}
