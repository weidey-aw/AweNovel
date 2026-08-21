package com.weidey.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weidey.system.domain.SysOperLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * 操作日志 数据层
 * 
 * @author ruoyi
 */
@Mapper
public interface SysOperLogMapper extends BaseMapper<SysOperLog>
{
    /**
     * 新增操作日志
     * 
     * @param operLog 操作日志对象
     */
    @Insert("insert into sys_oper_log(title, business_type, method, request_method, operator_type, oper_name, dept_name, oper_url, oper_ip, oper_location, oper_param, json_result, status, error_msg, cost_time, oper_time) values (#{title}, #{businessType}, #{method}, #{requestMethod}, #{operatorType}, #{operName}, #{deptName}, #{operUrl}, #{operIp}, #{operLocation}, #{operParam}, #{jsonResult}, #{status}, #{errorMsg}, #{costTime}, sysdate())")
    void insertOperlog(SysOperLog operLog);

    /**
     * 清空操作日志
     * 
     * @return 结果
     */
    @Update("truncate table sys_oper_log")
    int cleanOperLog();
}
