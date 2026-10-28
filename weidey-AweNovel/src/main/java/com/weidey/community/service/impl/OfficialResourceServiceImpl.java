package com.weidey.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weidey.community.domain.OfficialResource;
import com.weidey.community.mapper.OfficialResourceMapper;
import com.weidey.community.service.OfficialResourceService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 正版资源服务实现
 *
 * @author weidey
 */
@Service
public class OfficialResourceServiceImpl extends ServiceImpl<OfficialResourceMapper, OfficialResource>
        implements OfficialResourceService {

    @Override
    public List<OfficialResource> listByGame(Long gameId) {
        if (gameId == null) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<OfficialResource>()
                .eq(OfficialResource::getGameId, gameId)
                .orderByAsc(OfficialResource::getSort)
                .orderByAsc(OfficialResource::getOfficialId));
    }
}
