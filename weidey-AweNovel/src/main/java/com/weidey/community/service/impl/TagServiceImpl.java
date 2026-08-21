package com.weidey.community.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weidey.community.domain.Tag;
import com.weidey.community.mapper.TagMapper;
import com.weidey.community.service.TagService;
import org.springframework.stereotype.Service;

/**
 * 标签服务实现
 *
 * @author weidey
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {
}
