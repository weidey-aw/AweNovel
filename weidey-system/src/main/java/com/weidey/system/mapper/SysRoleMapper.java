package com.weidey.system.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weidey.common.core.domain.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 角色表 数据层
 * 
 * @author ruoyi
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole>
{
    /**
     * 根据用户ID查询角色
     * 
     * @param userId 用户ID
     * @return 角色列表
     */
    @Select("select distinct r.role_id,r.role_name,r.role_key,r.role_sort,r.menu_check_strictly,r.status,r.del_flag,r.create_time,r.remark from sys_role r left join sys_user_role ur on ur.role_id=r.role_id where r.del_flag='0' and ur.user_id=#{userId}")
    List<SysRole> selectRolePermissionByUserId(Long userId);

    /**
     * 根据用户ID获取角色选择框列表
     * 
     * @param userId 用户ID
     * @return 选中角色ID列表
     */
    @Select("select r.role_id from sys_role r left join sys_user_role ur on ur.role_id=r.role_id where ur.user_id=#{userId}")
    List<Long> selectRoleListByUserId(Long userId);

    /**
     * 根据用户名查询角色
     * 
     * @param userName 用户名
     * @return 角色列表
     */
    @Select("select distinct r.role_id,r.role_name,r.role_key,r.role_sort,r.menu_check_strictly,r.status,r.del_flag,r.create_time,r.remark from sys_role r left join sys_user_role ur on ur.role_id=r.role_id left join sys_user u on u.user_id=ur.user_id where r.del_flag='0' and u.user_name=#{userName}")
    List<SysRole> selectRolesByUserName(String userName);

    /**
     * 校验角色名称是否唯一
     * 
     * @param roleName 角色名称
     * @return 角色信息
     */
    @Select("select role_id,role_name from sys_role where role_name=#{roleName} and del_flag='0' limit 1")
    SysRole checkRoleNameUnique(String roleName);

    /**
     * 校验角色权限是否唯一
     * 
     * @param roleKey 角色权限
     * @return 角色信息
     */
    @Select("select role_id,role_key from sys_role where role_key=#{roleKey} and del_flag='0' limit 1")
    SysRole checkRoleKeyUnique(String roleKey);
}
