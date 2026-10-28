package com.weidey.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weidey.common.utils.StringUtils;
import com.weidey.community.domain.GameTag;
import com.weidey.community.domain.Tag;
import com.weidey.community.mapper.GameTagMapper;
import com.weidey.community.mapper.TagMapper;
import com.weidey.community.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 标签服务实现
 *
 * @author weidey
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Autowired
    private GameTagMapper gameTagMapper;

    @Override
    public IPage<Tag> pageTags(IPage<Tag> page, String name, String type) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(name), Tag::getName, name)
                .eq(StringUtils.isNotBlank(type), Tag::getType, type)
                .orderByAsc(Tag::getTagId);
        Page<Tag> result = (Page<Tag>) baseMapper.selectPage((Page<Tag>) page, wrapper);
        for (Tag tag : result.getRecords()) {
            tag.setGameCount(countGames(tag.getTagId()));
        }
        return result;
    }

    @Override
    public long countGames(Long tagId) {
        if (tagId == null) {
            return 0;
        }
        return gameTagMapper.selectCount(new LambdaQueryWrapper<GameTag>().eq(GameTag::getTagId, tagId));
    }

    @Override
    @Transactional
    public void deleteTagsCascade(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        gameTagMapper.delete(new LambdaQueryWrapper<GameTag>().in(GameTag::getTagId, tagIds));
        removeByIds(tagIds);
    }
}
