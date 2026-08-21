package com.weidey.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weidey.common.core.domain.entity.SysDictType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 字典表 数据层
 * 
 * @author ruoyi
 */
@Mapper
public interface SysDictTypeMapper extends BaseMapper<SysDictType>
{
    /**
     * 根据字典类型ID查询信息
     * 
     * @param dictId 字典类型ID
     * @return 字典类型
     */
    @Select("select dict_id,dict_name,dict_type,status,create_by,create_time,remark from sys_dict_type where dict_id=#{dictId}")
    SysDictType selectDictTypeById(Long dictId);

    /**
     * 校验字典类型称是否唯一
     * 
     * @param dictType 字典类型
     * @return 结果
     */
    @Select("select dict_id,dict_name,dict_type,status,create_by,create_time,remark from sys_dict_type where dict_type=#{dictType} limit 1")
    SysDictType checkDictTypeUnique(String dictType);
}
