package com.weidey.system.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.weidey.common.constant.UserConstants;
import com.weidey.common.core.domain.entity.SysDictData;
import com.weidey.common.core.domain.entity.SysDictType;
import com.weidey.common.exception.ServiceException;
import com.weidey.common.utils.DictUtils;
import com.weidey.common.utils.StringUtils;
import com.weidey.system.mapper.SysDictDataMapper;
import com.weidey.system.mapper.SysDictTypeMapper;
import com.weidey.system.service.ISysDictTypeService;

/**
 * 字典 业务层处理
 * 
 * @author ruoyi
 */
@Service
public class SysDictTypeServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType> implements ISysDictTypeService
{
    @Autowired
    private SysDictDataMapper dictDataMapper;

    /**
     * 项目启动时，初始化字典到缓存
     */
    @PostConstruct
    public void init()
    {
        loadingDictCache();
    }

    /**
     * 构造字典类型查询条件
     */
    private LambdaQueryWrapper<SysDictType> buildDictTypeWrapper(SysDictType dictType)
    {
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(dictType.getDictName()))
        {
            wrapper.like(SysDictType::getDictName, dictType.getDictName());
        }
        if (StringUtils.isNotEmpty(dictType.getStatus()))
        {
            wrapper.eq(SysDictType::getStatus, dictType.getStatus());
        }
        if (StringUtils.isNotEmpty(dictType.getDictType()))
        {
            wrapper.like(SysDictType::getDictType, dictType.getDictType());
        }
        if (dictType.getParams() != null)
        {
            Object beginTime = dictType.getParams().get("beginTime");
            if (beginTime != null && StringUtils.isNotEmpty(beginTime.toString()))
            {
                wrapper.ge(SysDictType::getCreateTime, beginTime.toString());
            }
            Object endTime = dictType.getParams().get("endTime");
            if (endTime != null && StringUtils.isNotEmpty(endTime.toString()))
            {
                wrapper.le(SysDictType::getCreateTime, endTime.toString());
            }
        }
        return wrapper;
    }

    /**
     * 根据条件查询字典类型
     * 
     * @param dictType 字典类型信息
     * @return 字典类型集合信息
     */
    @Override
    public List<SysDictType> selectDictTypeList(SysDictType dictType)
    {
        return list(buildDictTypeWrapper(dictType));
    }

    /**
     * 分页查询字典类型
     * 
     * @param page 分页对象
     * @param dictType 字典类型信息
     * @return 字典类型分页集合信息
     */
    @Override
    public Page<SysDictType> selectDictTypePage(Page<SysDictType> page, SysDictType dictType)
    {
        return page(page, buildDictTypeWrapper(dictType));
    }

    /**
     * 根据所有字典类型
     * 
     * @return 字典类型集合信息
     */
    @Override
    public List<SysDictType> selectDictTypeAll()
    {
        return list();
    }

    /**
     * 根据字典类型查询字典数据
     * 
     * @param dictType 字典类型
     * @return 字典数据集合信息
     */
    @Override
    public List<SysDictData> selectDictDataByType(String dictType)
    {
        List<SysDictData> dictDatas = DictUtils.getDictCache(dictType);
        if (StringUtils.isNotEmpty(dictDatas))
        {
            return dictDatas;
        }
        dictDatas = dictDataMapper.selectDictDataByType(dictType);
        if (StringUtils.isNotEmpty(dictDatas))
        {
            DictUtils.setDictCache(dictType, dictDatas);
            return dictDatas;
        }
        return null;
    }

    /**
     * 根据字典类型ID查询信息
     * 
     * @param dictId 字典类型ID
     * @return 字典类型
     */
    @Override
    public SysDictType selectDictTypeById(Long dictId)
    {
        return baseMapper.selectDictTypeById(dictId);
    }

    /**
     * 根据字典类型查询信息
     * 
     * @param dictType 字典类型
     * @return 字典类型
     */
    @Override
    public SysDictType selectDictTypeByType(String dictType)
    {
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictType::getDictType, dictType).last("limit 1");
        return getOne(wrapper, false);
    }

    /**
     * 批量删除字典类型信息
     * 
     * @param dictIds 需要删除的字典ID
     */
    @Override
    public void deleteDictTypeByIds(Long[] dictIds)
    {
        for (Long dictId : dictIds)
        {
            SysDictType dictType = selectDictTypeById(dictId);
            if (dictDataMapper.countDictDataByType(dictType.getDictType()) > 0)
            {
                throw new ServiceException(String.format("%1$s已分配,不能删除", dictType.getDictName()));
            }
            removeById(dictId);
            DictUtils.removeDictCache(dictType.getDictType());
        }
    }

    /**
     * 加载字典缓存数据
     */
    @Override
    public void loadingDictCache()
    {
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictData::getStatus, "0");
        Map<String, List<SysDictData>> dictDataMap = dictDataMapper.selectList(wrapper).stream().collect(Collectors.groupingBy(SysDictData::getDictType));
        for (Map.Entry<String, List<SysDictData>> entry : dictDataMap.entrySet())
        {
            DictUtils.setDictCache(entry.getKey(), entry.getValue().stream().sorted(Comparator.comparing(SysDictData::getDictSort)).collect(Collectors.toList()));
        }
    }

    /**
     * 清空字典缓存数据
     */
    @Override
    public void clearDictCache()
    {
        DictUtils.clearDictCache();
    }

    /**
     * 重置字典缓存数据
     */
    @Override
    public void resetDictCache()
    {
        clearDictCache();
        loadingDictCache();
    }

    /**
     * 新增保存字典类型信息
     * 
     * @param dict 字典类型信息
     * @return 结果
     */
    @Override
    public int insertDictType(SysDictType dict)
    {
        int row = save(dict) ? 1 : 0;
        if (row > 0)
        {
            DictUtils.setDictCache(dict.getDictType(), null);
        }
        return row;
    }

    /**
     * 修改保存字典类型信息
     * 
     * @param dict 字典类型信息
     * @return 结果
     */
    @Override
    @Transactional
    public int updateDictType(SysDictType dict)
    {
        SysDictType oldDict = baseMapper.selectDictTypeById(dict.getDictId());
        dictDataMapper.updateDictDataType(oldDict.getDictType(), dict.getDictType());
        int row = updateById(dict) ? 1 : 0;
        if (row > 0)
        {
            List<SysDictData> dictDatas = dictDataMapper.selectDictDataByType(dict.getDictType());
            DictUtils.setDictCache(dict.getDictType(), dictDatas);
        }
        return row;
    }

    /**
     * 校验字典类型称是否唯一
     * 
     * @param dict 字典类型
     * @return 结果
     */
    @Override
    public boolean checkDictTypeUnique(SysDictType dict)
    {
        Long dictId = StringUtils.isNull(dict.getDictId()) ? -1L : dict.getDictId();
        SysDictType dictType = baseMapper.checkDictTypeUnique(dict.getDictType());
        if (StringUtils.isNotNull(dictType) && dictType.getDictId().longValue() != dictId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
}
