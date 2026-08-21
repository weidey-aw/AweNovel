package com.weidey.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weidey.system.domain.SysLogininfor;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * 系统访问日志情况信息 数据层
 * 
 * @author ruoyi
 */
@Mapper
public interface SysLogininforMapper extends BaseMapper<SysLogininfor>
{
    /**
     * 新增系统登录日志
     * 
     * @param logininfor 访问日志对象
     */
    @Insert("insert into sys_logininfor (user_name, status, ipaddr, login_location, browser, os, msg, login_time) values (#{userName}, #{status}, #{ipaddr}, #{loginLocation}, #{browser}, #{os}, #{msg}, sysdate())")
    void insertLogininfor(SysLogininfor logininfor);

    /**
     * 清空系统登录日志
     * 
     * @return 结果
     */
    @Update("truncate table sys_logininfor")
    int cleanLogininfor();
}
