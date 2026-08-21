package com.weidey.system.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.weidey.common.core.domain.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户表 数据层
 * 
 * @author ruoyi
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser>
{
    /**
     * 通过用户名查询用户
     * 
     * @param userName 用户名
     * @return 用户对象信息
     */
    @Select("select u.user_id,u.user_name,u.nick_name,u.email,u.avatar,u.phonenumber,u.password,u.sex,u.status,u.del_flag,u.login_ip,u.login_date,u.create_by,u.create_time,u.remark from sys_user u where u.user_name=#{userName} and u.del_flag='0'")
    SysUser selectUserByUserName(String userName);

    /**
     * 校验用户名称是否唯一
     * 
     * @param userName 用户名称
     * @return 结果
     */
    @Select("select user_id,user_name from sys_user where user_name=#{userName} and del_flag='0' limit 1")
    SysUser checkUserNameUnique(String userName);

    /**
     * 校验手机号码是否唯一
     *
     * @param phonenumber 手机号码
     * @return 结果
     */
    @Select("select user_id,phonenumber from sys_user where phonenumber=#{phonenumber} and del_flag='0' limit 1")
    SysUser checkPhoneUnique(String phonenumber);

    /**
     * 校验email是否唯一
     *
     * @param email 用户邮箱
     * @return 结果
     */
    @Select("select user_id,email from sys_user where email=#{email} and del_flag='0' limit 1")
    SysUser checkEmailUnique(String email);

    /**
     * 通过用户邮箱查询用户
     * 
     * @param email 用户邮箱
     * @return 用户信息
     */
    @Select("select user_id,password from sys_user where email=#{email} and del_flag='0' limit 1")
    SysUser selectUserByUserEmail(String email);

    /**
     * 修改用户头像
     * 
     * @param userName 用户名
     * @param avatar 头像地址
     * @return 结果
     */
    @Update("update sys_user set avatar=#{avatar} where user_name=#{userName}")
    int updateUserAvatar(@Param("userName") String userName, @Param("avatar") String avatar);

    /**
     * 重置用户密码
     * 
     * @param userName 用户名
     * @param password 密码
     * @return 结果
     */
    @Update("update sys_user set password=#{password} where user_name=#{userName}")
    int resetUserPwd(@Param("userName") String userName, @Param("password") String password);

    /**
     * 根据条件分页查询已配用户角色列表
     * 
     * @param page 分页对象
     * @param roleId 角色ID
     * @return 用户信息集合信息
     */
    @Select("select distinct u.user_id,u.user_name,u.nick_name,u.email,u.phonenumber,u.status,u.create_time from sys_user u left join sys_user_role ur on u.user_id=ur.user_id where u.del_flag='0' and ur.role_id=#{roleId}")
    List<SysUser> selectAllocatedList(IPage<SysUser> page, @Param("roleId") Long roleId);

    /**
     * 根据条件分页查询未分配用户角色列表
     * 
     * @param page 分页对象
     * @param roleId 角色ID
     * @return 用户信息集合信息
     */
    @Select("select distinct u.user_id,u.user_name,u.nick_name,u.email,u.phonenumber,u.status,u.create_time from sys_user u where u.del_flag='0' and u.user_id not in (select user_id from sys_user_role where role_id=#{roleId})")
    List<SysUser> selectUnallocatedList(IPage<SysUser> page, @Param("roleId") Long roleId);
}
