package com.weidey.system.service.impl;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.weidey.common.core.domain.entity.SysDictData;
import com.weidey.common.utils.DictUtils;
import com.weidey.common.utils.StringUtils;
import com.weidey.system.mapper.SysDictDataMapper;
import com.weidey.system.service.ISysDictDataService;

/**
 * 字典 业务层处理
 * 
 * @author ruoyi
 */
@Service
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData> implements ISysDictDataService
{
    /**
     * 构造字典数据查询条件
     */
    private LambdaQueryWrapper<SysDictData> buildDictDataWrapper(SysDictData dictData)
    {
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(dictData.getDictType()))
        {
            wrapper.eq(SysDictData::getDictType, dictData.getDictType());
        }
        if (StringUtils.isNotEmpty(dictData.getDictLabel()))
        {
            wrapper.like(SysDictData::getDictLabel, dictData.getDictLabel());
        }
        if (StringUtils.isNotEmpty(dictData.getStatus()))
        {
            wrapper.eq(SysDictData::getStatus, dictData.getStatus());
        }
        wrapper.orderByAsc(SysDictData::getDictSort);
        return wrapper;
    }

    /**
     * 根据条件查询字典数据
     * 
     * @param dictData 字典数据信息
     * @return 字典数据集合信息
     */
    @Override
    public List<SysDictData> selectDictDataList(SysDictData dictData)
    {
        return list(buildDictDataWrapper(dictData));
    }

    /**
     * 分页查询字典数据
     * 
     * @param page 分页对象
     * @param dictData 字典数据信息
     * @return 字典数据分页集合信息
     */
    @Override
    public Page<SysDictData> selectDictDataPage(Page<SysDictData> page, SysDictData dictData)
    {
        return page(page, buildDictDataWrapper(dictData));
    }

    /**
     * 根据字典类型和字典键值查询字典数据信息
     * 
     * @param dictType 字典类型
     * @param dictValue 字典键值
     * @return 字典标签
     */
    @Override
    public String selectDictLabel(String dictType, String dictValue)
    {
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictData::getDictType, dictType).eq(SysDictData::getDictValue, dictValue).last("limit 1");
        SysDictData data = getOne(wrapper, false);
        return data != null ? data.getDictLabel() : null;
    }

    /**
     * 根据字典数据ID查询信息
     * 
     * @param dictCode 字典数据ID
     * @return 字典数据
     */
    @Override
    public SysDictData selectDictDataById(Long dictCode)
    {
        return getById(dictCode);
    }

    /**
     * 批量删除字典数据信息
     * 
     * @param dictCodes 需要删除的字典数据ID
     */
    @Override
    public void deleteDictDataByIds(Long[] dictCodes)
    {
        for (Long dictCode : dictCodes)
        {
            SysDictData data = selectDictDataById(dictCode);
            removeById(dictCode);
            List<SysDictData> dictDatas = baseMapper.selectDictDataByType(data.getDictType());
            DictUtils.setDictCache(data.getDictType(), dictDatas);
        }
    }

    /**
     * 新增保存字典数据信息
     * 
     * @param data 字典数据信息
     * @return 结果
     */
    @Override
    public int insertDictData(SysDictData data)
    {
        int row = save(data) ? 1 : 0;
        if (row > 0)
        {
            List<SysDictData> dictDatas = baseMapper.selectDictDataByType(data.getDictType());
            DictUtils.setDictCache(data.getDictType(), dictDatas);
        }
        return row;
    }

    /**
     * 修改保存字典数据信息
     * 
     * @param data 字典数据信息
     * @return 结果
     */
    @Override
    public int updateDictData(SysDictData data)
    {
        int row = updateById(data) ? 1 : 0;
        if (row > 0)
        {
            List<SysDictData> dictDatas = baseMapper.selectDictDataByType(data.getDictType());
            DictUtils.setDictCache(data.getDictType(), dictDatas);
        }
        return row;
    }
}
