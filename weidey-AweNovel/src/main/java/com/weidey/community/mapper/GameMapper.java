package com.weidey.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weidey.community.domain.Game;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GameMapper extends BaseMapper<Game> {
}
