package com.weidey.system.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weidey.common.core.domain.entity.SysDictData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 字典表 数据层
 * 
 * @author ruoyi
 */
@Mapper
public interface SysDictDataMapper extends BaseMapper<SysDictData>
{
    /**
     * 根据字典类型查询字典数据
     * 
     * @param dictType 字典类型
     * @return 字典数据集合信息
     */
    @Select("select dict_code,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,remark from sys_dict_data where status='0' and dict_type=#{dictType} order by dict_sort asc")
    List<SysDictData> selectDictDataByType(String dictType);

    /**
     * 查询字典数据数量
     * 
     * @param dictType 字典类型
     * @return 字典数据
     */
    @Select("select count(1) from sys_dict_data where dict_type=#{dictType}")
    int countDictDataByType(String dictType);

    /**
     * 同步修改字典类型
     * 
     * @param oldDictType 旧字典类型
     * @param newDictType 新字典类型
     * @return 结果
     */
    @Update("update sys_dict_data set dict_type=#{newDictType} where dict_type=#{oldDictType}")
    int updateDictDataType(@Param("oldDictType") String oldDictType, @Param("newDictType") String newDictType);
}
