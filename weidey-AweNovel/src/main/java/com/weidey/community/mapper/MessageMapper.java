package com.weidey.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weidey.community.domain.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
