package com.weidey.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weidey.community.domain.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
