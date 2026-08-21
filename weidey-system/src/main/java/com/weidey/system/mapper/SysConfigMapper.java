package com.weidey.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weidey.system.domain.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 参数配置 数据层
 * 
 * @author ruoyi
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig>
{
    /**
     * 查询参数配置信息
     * 
     * @param config 参数配置信息
     * @return 参数配置信息
     */
    @Select("select config_id,config_name,config_key,config_value,config_type,create_by,create_time,update_by,update_time,remark from sys_config where config_id=#{configId}")
    SysConfig selectConfig(SysConfig config);

    /**
     * 根据键名查询参数配置信息
     * 
     * @param configKey 参数键名
     * @return 参数配置信息
     */
    @Select("select config_id,config_name,config_key,config_value,config_type,create_by,create_time,update_by,update_time,remark from sys_config where config_key=#{configKey} limit 1")
    SysConfig checkConfigKeyUnique(String configKey);
}
