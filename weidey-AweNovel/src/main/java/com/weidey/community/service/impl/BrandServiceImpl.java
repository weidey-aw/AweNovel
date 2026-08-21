package com.weidey.community.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weidey.community.domain.Brand;
import com.weidey.community.mapper.BrandMapper;
import com.weidey.community.service.BrandService;
import org.springframework.stereotype.Service;

/**
 * 制作会社服务实现
 *
 * @author weidey
 */
@Service
public class BrandServiceImpl extends ServiceImpl<BrandMapper, Brand> implements BrandService {
}
