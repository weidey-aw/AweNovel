package com.weidey.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weidey.common.exception.ServiceException;
import com.weidey.common.utils.StringUtils;
import com.weidey.community.domain.Resource;
import com.weidey.community.mapper.ResourceMapper;
import com.weidey.community.service.ResourceService;
import com.weidey.community.service.ReviewService;
import com.weidey.community.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资源服务实现
 *
 * @author weidey
 */
@Service
public class ResourceServiceImpl extends ServiceImpl<ResourceMapper, Resource> implements ResourceService {

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private ReviewService reviewService;

    @Override
    public IPage<Resource> pageResources(IPage<Resource> page, Long gameId, Long userId, String status) {
        return baseMapper.selectPage((Page<Resource>) page,
                new LambdaQueryWrapper<Resource>()
                        .eq(gameId != null, Resource::getGameId, gameId)
                        .eq(userId != null, Resource::getUserId, userId)
                        .eq(StringUtils.isNotBlank(status), Resource::getStatus, status)
                        .orderByDesc(Resource::getResourceId));
    }

    @Override
    public Resource publish(Resource resource) {
        resource.setStatus("0");
        resource.setDownloadCount(0);
        resource.setReportCount(0);
        save(resource);
        // 启动 Flowable 审核流程
        resource.setProcessInstanceId(reviewService.startReview("resource", resource.getResourceId()));
        updateById(resource);
        return resource;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Resource download(Long resourceId, Long userId) {
        Resource resource = getById(resourceId);
        if (resource == null) {
            throw new ServiceException("资源不存在");
        }
        if (!"1".equals(resource.getStatus())) {
            throw new ServiceException("资源暂不可下载");
        }
        // 扣积分
        int cost = resource.getPoints() != null ? resource.getPoints() : 0;
        if (cost > 0) {
            boolean ok = userProfileService.spendPoints(userId, cost, "download", "resource", resourceId, "下载资源");
            if (!ok) {
                throw new ServiceException("积分不足，无法下载");
            }
        }
        // 下载次数 +1
        update(new LambdaUpdateWrapper<Resource>()
                .setSql("download_count = download_count + 1")
                .eq(Resource::getResourceId, resourceId));
        return resource;
    }

    @Override
    public void report(Long resourceId) {
        update(new LambdaUpdateWrapper<Resource>()
                .setSql("report_count = report_count + 1")
                .eq(Resource::getResourceId, resourceId));
    }
}
