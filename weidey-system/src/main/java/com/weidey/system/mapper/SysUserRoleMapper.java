package com.weidey.system.mapper;

import java.util.List;

import com.weidey.system.domain.SysUserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户与角色关联表 数据层
 * 
 * @author ruoyi
 */
@Mapper
public interface SysUserRoleMapper
{
    /**
     * 通过用户ID删除用户和角色关联
     * 
     * @param userId 用户ID
     * @return 结果
     */
    @Delete("delete from sys_user_role where user_id=#{userId}")
    int deleteUserRoleByUserId(Long userId);

    /**
     * 批量删除用户和角色关联
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Delete("<script>delete from sys_user_role where user_id in <foreach collection='array' item='userId' open='(' separator=',' close=')'>#{userId}</foreach></script>")
    int deleteUserRole(Long[] ids);

    /**
     * 通过角色ID查询角色使用数量
     * 
     * @param roleId 角色ID
     * @return 结果
     */
    @Select("select count(1) from sys_user_role where role_id=#{roleId}")
    int countUserRoleByRoleId(Long roleId);

    /**
     * 批量新增用户角色信息
     * 
     * @param userRoleList 用户角色列表
     * @return 结果
     */
    @Insert("<script>insert into sys_user_role(user_id, role_id) values <foreach item='item' index='index' collection='list' separator=','>(#{item.userId},#{item.roleId})</foreach></script>")
    int batchUserRole(List<SysUserRole> userRoleList);

    /**
     * 删除用户和角色关联信息
     * 
     * @param userRole 用户和角色关联信息
     * @return 结果
     */
    @Delete("delete from sys_user_role where user_id=#{userId} and role_id=#{roleId}")
    int deleteUserRoleInfo(SysUserRole userRole);

    /**
     * 批量取消授权用户角色
     * 
     * @param roleId 角色ID
     * @param userIds 需要删除的用户数据ID
     * @return 结果
     */
    @Delete("<script>delete from sys_user_role where role_id=#{roleId} and user_id in <foreach collection='userIds' item='userId' open='(' separator=',' close=')'>#{userId}</foreach></script>")
    int deleteUserRoleInfos(@Param("roleId") Long roleId, @Param("userIds") Long[] userIds);
}
