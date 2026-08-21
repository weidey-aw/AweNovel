package com.weidey.system.service.impl;

import java.util.Arrays;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.weidey.common.utils.StringUtils;
import com.weidey.system.domain.SysNotice;
import com.weidey.system.mapper.SysNoticeMapper;
import com.weidey.system.service.ISysNoticeService;

/**
 * 公告 服务层实现
 * 
 * @author ruoyi
 */
@Service
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements ISysNoticeService
{
    /**
     * 构造公告查询条件
     */
    private LambdaQueryWrapper<SysNotice> buildNoticeWrapper(SysNotice notice)
    {
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(notice.getNoticeTitle()))
        {
            wrapper.like(SysNotice::getNoticeTitle, notice.getNoticeTitle());
        }
        if (StringUtils.isNotEmpty(notice.getNoticeType()))
        {
            wrapper.eq(SysNotice::getNoticeType, notice.getNoticeType());
        }
        if (StringUtils.isNotEmpty(notice.getCreateBy()))
        {
            wrapper.like(SysNotice::getCreateBy, notice.getCreateBy());
        }
        return wrapper;
    }

    /**
     * 查询公告信息
     * 
     * @param noticeId 公告ID
     * @return 公告信息
     */
    @Override
    public SysNotice selectNoticeById(Long noticeId)
    {
        return getById(noticeId);
    }

    /**
     * 查询公告列表
     * 
     * @param notice 公告信息
     * @return 公告集合
     */
    @Override
    public List<SysNotice> selectNoticeList(SysNotice notice)
    {
        return list(buildNoticeWrapper(notice));
    }

    /**
     * 分页查询公告列表
     * 
     * @param page 分页对象
     * @param notice 公告信息
     * @return 公告分页集合
     */
    @Override
    public Page<SysNotice> selectNoticePage(Page<SysNotice> page, SysNotice notice)
    {
        return page(page, buildNoticeWrapper(notice));
    }

    /**
     * 新增公告
     * 
     * @param notice 公告信息
     * @return 结果
     */
    @Override
    public int insertNotice(SysNotice notice)
    {
        return save(notice) ? 1 : 0;
    }

    /**
     * 修改公告
     * 
     * @param notice 公告信息
     * @return 结果
     */
    @Override
    public int updateNotice(SysNotice notice)
    {
        return updateById(notice) ? 1 : 0;
    }

    /**
     * 删除公告对象
     * 
     * @param noticeId 公告ID
     * @return 结果
     */
    @Override
    public int deleteNoticeById(Long noticeId)
    {
        return removeById(noticeId) ? 1 : 0;
    }

    /**
     * 批量删除公告信息
     * 
     * @param noticeIds 需要删除的公告ID
     * @return 结果
     */
    @Override
    public int deleteNoticeByIds(Long[] noticeIds)
    {
        return removeByIds(Arrays.asList(noticeIds)) ? 1 : 0;
    }
}
