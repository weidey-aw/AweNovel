package com.weidey.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weidey.common.utils.StringUtils;
import com.weidey.community.domain.Brand;
import com.weidey.community.domain.Game;
import com.weidey.community.mapper.BrandMapper;
import com.weidey.community.mapper.GameMapper;
import com.weidey.community.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 制作会社服务实现
 *
 * @author weidey
 */
@Service
public class BrandServiceImpl extends ServiceImpl<BrandMapper, Brand> implements BrandService {

    @Autowired
    private GameMapper gameMapper;

    @Override
    public IPage<Brand> pageBrands(IPage<Brand> page, String keyword) {
        LambdaQueryWrapper<Brand> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(keyword), Brand::getName, keyword)
                .orderByAsc(Brand::getBrandId);
        Page<Brand> result = (Page<Brand>) baseMapper.selectPage((Page<Brand>) page, wrapper);
        for (Brand brand : result.getRecords()) {
            brand.setGameCount(countGames(brand.getBrandId()));
        }
        return result;
    }

    @Override
    public long countGames(Long brandId) {
        if (brandId == null) {
            return 0;
        }
        return gameMapper.selectCount(new LambdaQueryWrapper<Game>().eq(Game::getBrandId, brandId));
    }
}
