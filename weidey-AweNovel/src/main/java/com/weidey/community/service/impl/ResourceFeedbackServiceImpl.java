package com.weidey.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weidey.common.core.domain.entity.SysUser;
import com.weidey.common.exception.ServiceException;
import com.weidey.common.utils.StringUtils;
import com.weidey.community.domain.Game;
import com.weidey.community.domain.Resource;
import com.weidey.community.domain.ResourceFeedback;
import com.weidey.community.mapper.GameMapper;
import com.weidey.community.mapper.ResourceFeedbackMapper;
import com.weidey.community.mapper.ResourceMapper;
import com.weidey.community.service.MessageService;
import com.weidey.community.service.ResourceFeedbackService;
import com.weidey.system.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 资源反馈服务实现
 *
 * @author weidey
 */
@Service
public class ResourceFeedbackServiceImpl extends ServiceImpl<ResourceFeedbackMapper, ResourceFeedback>
        implements ResourceFeedbackService {

    @Autowired
    private ResourceMapper resourceMapper;

    @Autowired
    private GameMapper gameMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private MessageService messageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceFeedback submit(Long userId, Long resourceId, Integer type, String content) {
        Resource resource = resourceMapper.selectById(resourceId);
        if (resource == null) {
            throw new ServiceException("资源不存在");
        }
        if (resource.getUserId() != null && resource.getUserId().equals(userId)) {
            throw new ServiceException("不能给自己上传的资源提交反馈");
        }
        if (type == null || type < 1 || type > 4) {
            throw new ServiceException("请选择反馈类型");
        }
        if (StringUtils.isBlank(content)) {
            throw new ServiceException("请填写反馈内容");
        }
        String text = content.trim();
        if (text.length() > 500) {
            text = text.substring(0, 500);
        }

        ResourceFeedback feedback = new ResourceFeedback();
        feedback.setResourceId(resourceId);
        feedback.setUserId(userId);
        feedback.setType(type);
        feedback.setContent(text);
        feedback.setStatus("0");
        feedback.setCreateTime(new Date());
        save(feedback);

        // 失效类反馈累计到 report_count，便于资源排序/治理
        if (type == 1) {
            resourceMapper.update(null, new LambdaUpdateWrapper<Resource>()
                    .setSql("report_count = report_count + 1")
                    .eq(Resource::getResourceId, resourceId));
        }

        if (resource.getUserId() != null && !resource.getUserId().equals(userId)) {
            String gameTitle = "";
            if (resource.getGameId() != null) {
                Game game = gameMapper.selectById(resource.getGameId());
                gameTitle = game == null ? "" : StringUtils.isNotBlank(game.getTitleCn()) ? game.getTitleCn() : game.getTitle();
            }
            String message = String.format("【%s】%s%s%n%s",
                    ResourceFeedback.typeLabel(type),
                    StringUtils.isNotBlank(gameTitle) ? "《" + gameTitle + "》" : "",
                    resource.getTitle(),
                    text);
            messageService.send(resource.getUserId(), userId, "feedback", "resource", resourceId, message);
        }
        // 返回值补全展示字段，便于前端直接使用
        feedback.setTypeLabel(ResourceFeedback.typeLabel(type));
        feedback.setResourceTitle(resource.getTitle());
        feedback.setGameId(resource.getGameId());
        return feedback;
    }

    @Override
    public IPage<ResourceFeedback> pageByResource(IPage<ResourceFeedback> page, Long resourceId, Long userId) {
        Page<ResourceFeedback> result = (Page<ResourceFeedback>) baseMapper.selectPage((Page<ResourceFeedback>) page,
                new LambdaQueryWrapper<ResourceFeedback>()
                        .eq(ResourceFeedback::getResourceId, resourceId)
                        .orderByDesc(ResourceFeedback::getFeedbackId));
        fillRows(result.getRecords());
        return result;
    }

    @Override
    public IPage<ResourceFeedback> pageReceived(IPage<ResourceFeedback> page, Long userId) {
        List<Resource> resources = resourceMapper.selectList(new LambdaQueryWrapper<Resource>()
                .eq(Resource::getUserId, userId)
                .select(Resource::getResourceId));
        if (resources.isEmpty()) {
            return new Page<>(page.getCurrent(), page.getSize());
        }
        List<Long> ids = resources.stream().map(Resource::getResourceId).collect(Collectors.toList());
        Page<ResourceFeedback> result = (Page<ResourceFeedback>) baseMapper.selectPage((Page<ResourceFeedback>) page,
                new LambdaQueryWrapper<ResourceFeedback>()
                        .in(ResourceFeedback::getResourceId, ids)
                        .orderByDesc(ResourceFeedback::getFeedbackId));
        fillRows(result.getRecords());
        return result;
    }

    /** 批量补全反馈人、资源、游戏信息 */
    private void fillRows(List<ResourceFeedback> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> userIds = rows.stream().map(ResourceFeedback::getUserId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getUserId, u -> u, (a, b) -> a));

        List<Long> resourceIds = rows.stream().map(ResourceFeedback::getResourceId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Long, Resource> resourceMap = resourceIds.isEmpty() ? Collections.emptyMap()
                : resourceMapper.selectBatchIds(resourceIds).stream()
                .collect(Collectors.toMap(Resource::getResourceId, r -> r, (a, b) -> a));

        List<Long> gameIds = resourceMap.values().stream().map(Resource::getGameId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Long, Game> gameMap = gameIds.isEmpty() ? Collections.emptyMap()
                : gameMapper.selectBatchIds(gameIds).stream()
                .collect(Collectors.toMap(Game::getGameId, g -> g, (a, b) -> a));

        for (ResourceFeedback item : rows) {
            SysUser user = item.getUserId() == null ? null : userMap.get(item.getUserId());
            if (user != null) {
                item.setUserName(StringUtils.isNotBlank(user.getNickName()) ? user.getNickName() : user.getUserName());
                item.setUserAvatar(user.getAvatar());
            }
            Resource resource = resourceMap.get(item.getResourceId());
            if (resource != null) {
                item.setResourceTitle(resource.getTitle());
                item.setGameId(resource.getGameId());
                Game game = resource.getGameId() == null ? null : gameMap.get(resource.getGameId());
                if (game != null) {
                    item.setGameTitle(StringUtils.isNotBlank(game.getTitleCn()) ? game.getTitleCn() : game.getTitle());
                }
            }
            item.setTypeLabel(ResourceFeedback.typeLabel(item.getType()));
        }
    }

    @Override
    public long countUnread(Long userId) {
        List<Resource> resources = resourceMapper.selectList(new LambdaQueryWrapper<Resource>()
                .eq(Resource::getUserId, userId)
                .select(Resource::getResourceId));
        if (resources.isEmpty()) {
            return 0;
        }
        List<Long> ids = resources.stream().map(Resource::getResourceId).collect(Collectors.toList());
        return count(new LambdaQueryWrapper<ResourceFeedback>()
                .in(ResourceFeedback::getResourceId, ids)
                .eq(ResourceFeedback::getStatus, "0"));
    }

    @Override
    public void markRead(Long userId, Long feedbackId) {
        ResourceFeedback feedback = getById(feedbackId);
        if (feedback == null) {
            throw new ServiceException("反馈不存在");
        }
        Resource resource = resourceMapper.selectById(feedback.getResourceId());
        if (resource == null || !Objects.equals(resource.getUserId(), userId)) {
            throw new ServiceException("只能查看自己资源的反馈");
        }
        update(new LambdaUpdateWrapper<ResourceFeedback>()
                .set(ResourceFeedback::getStatus, "1")
                .eq(ResourceFeedback::getFeedbackId, feedbackId));
    }
}
