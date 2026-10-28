package com.weidey.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weidey.common.exception.ServiceException;
import com.weidey.common.utils.StringUtils;
import com.weidey.community.domain.Game;
import com.weidey.community.domain.Resource;
import com.weidey.community.domain.ResourceSource;
import com.weidey.community.mapper.GameMapper;
import com.weidey.community.mapper.ResourceMapper;
import com.weidey.community.mapper.ResourceSourceMapper;
import com.weidey.community.service.ResourceService;
import com.weidey.community.service.ReviewService;
import com.weidey.community.service.UserProfileService;
import com.weidey.system.mapper.SysUserMapper;
import com.weidey.common.core.domain.entity.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资源服务实现
 *
 * @author weidey
 */
@Service
public class ResourceServiceImpl extends ServiceImpl<ResourceMapper, Resource> implements ResourceService {

    @Autowired
    private ResourceSourceMapper resourceSourceMapper;

    @Autowired
    private GameMapper gameMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private ReviewService reviewService;

    @Override
    public IPage<Resource> pageResources(IPage<Resource> page, Long gameId, Long userId, String status, String type) {
        LambdaQueryWrapper<Resource> wrapper = new LambdaQueryWrapper<Resource>()
                .eq(gameId != null, Resource::getGameId, gameId)
                .eq(userId != null, Resource::getUserId, userId)
                .eq(StringUtils.isNotBlank(status), Resource::getStatus, status)
                .eq(StringUtils.isNotBlank(type), Resource::getType, type)
                .orderByDesc(Resource::getResourceId);
        Page<Resource> result = (Page<Resource>) baseMapper.selectPage((Page<Resource>) page, wrapper);
        fillRows(result.getRecords());
        return result;
    }

    @Override
    public IPage<Resource> pageAdminResources(IPage<Resource> page, String keyword, Long gameId, String type, String status) {
        LambdaQueryWrapper<Resource> wrapper = new LambdaQueryWrapper<Resource>()
                .like(StringUtils.isNotBlank(keyword), Resource::getTitle, keyword)
                .eq(gameId != null, Resource::getGameId, gameId)
                .eq(StringUtils.isNotBlank(type), Resource::getType, type)
                .eq(StringUtils.isNotBlank(status), Resource::getStatus, status)
                .eq(StringUtils.isNotBlank(type), Resource::getType, type)
                .orderByDesc(Resource::getResourceId);
        Page<Resource> result = (Page<Resource>) baseMapper.selectPage((Page<Resource>) page, wrapper);
        fillRows(result.getRecords());
        return result;
    }

    /** 批量填充发布者、游戏名、来源数量 */
    private void fillRows(List<Resource> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> userIds = rows.stream().map(Resource::getUserId).filter(java.util.Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getUserId, u -> u, (a, b) -> a));

        List<Long> gameIds = rows.stream().map(Resource::getGameId).filter(java.util.Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Long, String> gameMap = gameIds.isEmpty() ? Collections.emptyMap()
                : gameMapper.selectBatchIds(gameIds).stream()
                .collect(Collectors.toMap(Game::getGameId, g -> StringUtils.isNotBlank(g.getTitleCn()) ? g.getTitleCn() : g.getTitle(), (a, b) -> a));

        List<ResourceSource> sources = resourceSourceMapper.selectList(
                new LambdaQueryWrapper<ResourceSource>().in(ResourceSource::getResourceId, rows.stream().map(Resource::getResourceId).collect(Collectors.toList())));
        Map<Long, Long> countMap = sources.stream()
                .collect(Collectors.groupingBy(ResourceSource::getResourceId, Collectors.counting()));

        for (Resource r : rows) {
            SysUser user = r.getUserId() == null ? null : userMap.get(r.getUserId());
            if (user != null) {
                r.setUserName(StringUtils.isNotBlank(user.getNickName()) ? user.getNickName() : user.getUserName());
                r.setUserAvatar(user.getAvatar());
            }
            r.setGameTitle(gameMap.get(r.getGameId()));
            r.setSourceCount(countMap.getOrDefault(r.getResourceId(), 0L));
        }
    }

    @Override
    public Resource getResourceDetail(Long resourceId) {
        Resource resource = getById(resourceId);
        if (resource == null) {
            return null;
        }
        resource.setSources(listSources(resourceId));
        fillRows(Collections.singletonList(resource));
        return resource;
    }

    private List<ResourceSource> listSources(Long resourceId) {
        return resourceSourceMapper.selectList(new LambdaQueryWrapper<ResourceSource>()
                .eq(ResourceSource::getResourceId, resourceId)
                .orderByAsc(ResourceSource::getSort)
                .orderByAsc(ResourceSource::getSourceId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Resource saveResource(Resource resource) {
        if (StringUtils.isBlank(resource.getStatus())) {
            resource.setStatus("1");
        }
        if (resource.getDownloadCount() == null) {
            resource.setDownloadCount(0);
        }
        if (resource.getReportCount() == null) {
            resource.setReportCount(0);
        }
        if (resource.getPoints() == null) {
            resource.setPoints(0);
        }
        resource.setResourceId(null);
        save(resource);
        syncSources(resource);
        return resource;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Resource updateResource(Resource resource) {
        if (resource.getResourceId() == null) {
            throw new ServiceException("资源ID不能为空");
        }
        updateById(resource);
        syncSources(resource);
        return resource;
    }

    /** 全量覆盖来源（sources 为 null 时不改动） */
    private void syncSources(Resource resource) {
        if (resource.getSources() == null) {
            return;
        }
        Long resourceId = resource.getResourceId();
        resourceSourceMapper.delete(new LambdaQueryWrapper<ResourceSource>()
                .eq(ResourceSource::getResourceId, resourceId));
        int index = 0;
        for (ResourceSource source : resource.getSources()) {
            if (StringUtils.isBlank(source.getUrl())) {
                continue;
            }
            source.setSourceId(null);
            source.setResourceId(resourceId);
            source.setSort(source.getSort() == null ? index : source.getSort());
            source.setCreateTime(new Date());
            resourceSourceMapper.insert(source);
            index++;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteResources(List<Long> resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return;
        }
        resourceSourceMapper.delete(new LambdaQueryWrapper<ResourceSource>()
                .in(ResourceSource::getResourceId, resourceIds));
        removeByIds(resourceIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Resource publish(Resource resource) {
        resource.setStatus("0");
        resource.setDownloadCount(0);
        resource.setReportCount(0);
        if (resource.getPoints() == null) {
            resource.setPoints(0);
        }
        save(resource);
        syncSources(resource);
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
        return getResourceDetail(resourceId);
    }

    @Override
    public void report(Long resourceId) {
        update(new LambdaUpdateWrapper<Resource>()
                .setSql("report_count = report_count + 1")
                .eq(Resource::getResourceId, resourceId));
    }
}
