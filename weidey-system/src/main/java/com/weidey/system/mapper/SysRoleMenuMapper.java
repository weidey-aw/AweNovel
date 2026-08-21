package com.weidey.system.mapper;

import java.util.List;

import com.weidey.system.domain.SysRoleMenu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 角色与菜单关联表 数据层
 * 
 * @author ruoyi
 */
@Mapper
public interface SysRoleMenuMapper
{
    /**
     * 查询菜单使用数量
     * 
     * @param menuId 菜单ID
     * @return 结果
     */
    @Select("select count(1) from sys_role_menu where menu_id=#{menuId}")
    int checkMenuExistRole(Long menuId);

    /**
     * 通过角色ID删除角色和菜单关联
     * 
     * @param roleId 角色ID
     * @return 结果
     */
    @Delete("delete from sys_role_menu where role_id=#{roleId}")
    int deleteRoleMenuByRoleId(Long roleId);

    /**
     * 批量删除角色菜单关联信息
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Delete("<script>delete from sys_role_menu where role_id in <foreach collection='array' item='roleId' open='(' separator=',' close=')'>#{roleId}</foreach></script>")
    int deleteRoleMenu(Long[] ids);

    /**
     * 批量新增角色菜单信息
     * 
     * @param roleMenuList 角色菜单列表
     * @return 结果
     */
    @Insert("<script>insert into sys_role_menu(role_id, menu_id) values <foreach item='item' index='index' collection='list' separator=','>(#{item.roleId},#{item.menuId})</foreach></script>")
    int batchRoleMenu(List<SysRoleMenu> roleMenuList);
}
