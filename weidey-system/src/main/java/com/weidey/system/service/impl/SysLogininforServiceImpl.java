package com.weidey.system.service.impl;

import java.util.Arrays;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.weidey.common.utils.StringUtils;
import com.weidey.system.domain.SysLogininfor;
import com.weidey.system.mapper.SysLogininforMapper;
import com.weidey.system.service.ISysLogininforService;

/**
 * 系统访问日志情况信息 服务层处理
 * 
 * @author ruoyi
 */
@Service
public class SysLogininforServiceImpl extends ServiceImpl<SysLogininforMapper, SysLogininfor> implements ISysLogininforService
{
    /**
     * 构造登录日志查询条件
     */
    private LambdaQueryWrapper<SysLogininfor> buildLogininforWrapper(SysLogininfor logininfor)
    {
        LambdaQueryWrapper<SysLogininfor> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(logininfor.getIpaddr()))
        {
            wrapper.like(SysLogininfor::getIpaddr, logininfor.getIpaddr());
        }
        if (StringUtils.isNotEmpty(logininfor.getStatus()))
        {
            wrapper.eq(SysLogininfor::getStatus, logininfor.getStatus());
        }
        if (StringUtils.isNotEmpty(logininfor.getUserName()))
        {
            wrapper.like(SysLogininfor::getUserName, logininfor.getUserName());
        }
        if (logininfor.getParams() != null)
        {
            Object beginTime = logininfor.getParams().get("beginTime");
            if (beginTime != null && StringUtils.isNotEmpty(beginTime.toString()))
            {
                wrapper.ge(SysLogininfor::getLoginTime, beginTime.toString());
            }
            Object endTime = logininfor.getParams().get("endTime");
            if (endTime != null && StringUtils.isNotEmpty(endTime.toString()))
            {
                wrapper.le(SysLogininfor::getLoginTime, endTime.toString());
            }
        }
        wrapper.orderByDesc(SysLogininfor::getInfoId);
        return wrapper;
    }

    /**
     * 新增系统登录日志
     * 
     * @param logininfor 访问日志对象
     */
    @Override
    public void insertLogininfor(SysLogininfor logininfor)
    {
        baseMapper.insertLogininfor(logininfor);
    }

    /**
     * 查询系统登录日志集合
     * 
     * @param logininfor 访问日志对象
     * @return 登录记录集合
     */
    @Override
    public List<SysLogininfor> selectLogininforList(SysLogininfor logininfor)
    {
        return list(buildLogininforWrapper(logininfor));
    }

    /**
     * 分页查询系统登录日志集合
     * 
     * @param page 分页对象
     * @param logininfor 访问日志对象
     * @return 登录记录分页集合
     */
    @Override
    public Page<SysLogininfor> selectLogininforPage(Page<SysLogininfor> page, SysLogininfor logininfor)
    {
        return page(page, buildLogininforWrapper(logininfor));
    }

    /**
     * 批量删除系统登录日志
     * 
     * @param infoIds 需要删除的登录日志ID
     * @return 结果
     */
    @Override
    public int deleteLogininforByIds(Long[] infoIds)
    {
        return removeByIds(Arrays.asList(infoIds)) ? 1 : 0;
    }

    /**
     * 清空系统登录日志
     */
    @Override
    public void cleanLogininfor()
    {
        baseMapper.cleanLogininfor();
    }
}
