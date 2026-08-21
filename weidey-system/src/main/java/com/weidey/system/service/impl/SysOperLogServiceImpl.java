package com.weidey.system.service.impl;

import java.util.Arrays;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.weidey.common.utils.StringUtils;
import com.weidey.system.domain.SysOperLog;
import com.weidey.system.mapper.SysOperLogMapper;
import com.weidey.system.service.ISysOperLogService;

/**
 * 操作日志 服务层处理
 * 
 * @author ruoyi
 */
@Service
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements ISysOperLogService
{
    /**
     * 构造操作日志查询条件
     */
    private LambdaQueryWrapper<SysOperLog> buildOperLogWrapper(SysOperLog operLog)
    {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(operLog.getOperIp()))
        {
            wrapper.like(SysOperLog::getOperIp, operLog.getOperIp());
        }
        if (StringUtils.isNotEmpty(operLog.getTitle()))
        {
            wrapper.like(SysOperLog::getTitle, operLog.getTitle());
        }
        if (operLog.getBusinessType() != null)
        {
            wrapper.eq(SysOperLog::getBusinessType, operLog.getBusinessType());
        }
        if (operLog.getBusinessTypes() != null && operLog.getBusinessTypes().length > 0)
        {
            wrapper.in(SysOperLog::getBusinessType, Arrays.asList(operLog.getBusinessTypes()));
        }
        if (operLog.getStatus() != null)
        {
            wrapper.eq(SysOperLog::getStatus, operLog.getStatus());
        }
        if (StringUtils.isNotEmpty(operLog.getOperName()))
        {
            wrapper.like(SysOperLog::getOperName, operLog.getOperName());
        }
        if (operLog.getParams() != null)
        {
            Object beginTime = operLog.getParams().get("beginTime");
            if (beginTime != null && StringUtils.isNotEmpty(beginTime.toString()))
            {
                wrapper.ge(SysOperLog::getOperTime, beginTime.toString());
            }
            Object endTime = operLog.getParams().get("endTime");
            if (endTime != null && StringUtils.isNotEmpty(endTime.toString()))
            {
                wrapper.le(SysOperLog::getOperTime, endTime.toString());
            }
        }
        wrapper.orderByDesc(SysOperLog::getOperId);
        return wrapper;
    }

    /**
     * 新增操作日志
     * 
     * @param operLog 操作日志对象
     */
    @Override
    public void insertOperlog(SysOperLog operLog)
    {
        baseMapper.insertOperlog(operLog);
    }

    /**
     * 查询系统操作日志集合
     * 
     * @param operLog 操作日志对象
     * @return 操作日志集合
     */
    @Override
    public List<SysOperLog> selectOperLogList(SysOperLog operLog)
    {
        return list(buildOperLogWrapper(operLog));
    }

    /**
     * 分页查询系统操作日志集合
     * 
     * @param page 分页对象
     * @param operLog 操作日志对象
     * @return 操作日志分页集合
     */
    @Override
    public Page<SysOperLog> selectOperLogPage(Page<SysOperLog> page, SysOperLog operLog)
    {
        return page(page, buildOperLogWrapper(operLog));
    }

    /**
     * 批量删除系统操作日志
     * 
     * @param operIds 需要删除的操作日志ID
     * @return 结果
     */
    @Override
    public int deleteOperLogByIds(Long[] operIds)
    {
        return removeByIds(Arrays.asList(operIds)) ? 1 : 0;
    }

    /**
     * 查询操作日志详细
     * 
     * @param operId 操作ID
     * @return 操作日志对象
     */
    @Override
    public SysOperLog selectOperLogById(Long operId)
    {
        return getById(operId);
    }

    /**
     * 清空操作日志
     */
    @Override
    public void cleanOperLog()
    {
        baseMapper.cleanOperLog();
    }
}
