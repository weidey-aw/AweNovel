package com.weidey.system.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weidey.common.core.domain.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 菜单表 数据层
 *
 * @author ruoyi
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu>
{
    /**
     * 根据用户查询系统菜单列表
     *
     * @param menu 菜单信息
     * @return 菜单列表
     */
    @Select("<script>select distinct m.menu_id,m.parent_id,m.menu_name,m.path,m.component,m.`query`,m.visible,m.status,ifnull(m.perms,'') as perms,m.is_frame,m.is_cache,m.menu_type,m.icon,m.order_num,m.create_time from sys_menu m left join sys_role_menu rm on m.menu_id=rm.menu_id left join sys_user_role ur on rm.role_id=ur.role_id where ur.user_id=#{params.userId}" +
            "<if test=\"menuName != null and menuName != ''\"> and m.menu_name like concat('%', #{menuName}, '%')</if>" +
            "<if test=\"visible != null and visible != ''\"> and m.visible = #{visible}</if>" +
            "<if test=\"status != null and status != ''\"> and m.status = #{status}</if>" +
            " order by m.parent_id,m.order_num</script>")
    List<SysMenu> selectMenuListByUserId(SysMenu menu);

    /**
     * 根据角色ID查询权限
     * 
     * @param roleId 角色ID
     * @return 权限列表
     */
    @Select("select distinct m.perms from sys_menu m left join sys_role_menu rm on m.menu_id=rm.menu_id where m.status='0' and rm.role_id=#{roleId}")
    List<String> selectMenuPermsByRoleId(Long roleId);

    /**
     * 根据用户ID查询权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    @Select("select distinct m.perms from sys_menu m left join sys_role_menu rm on m.menu_id=rm.menu_id left join sys_user_role ur on rm.role_id=ur.role_id left join sys_role r on r.role_id=ur.role_id where m.status='0' and r.status='0' and ur.user_id=#{userId}")
    List<String> selectMenuPermsByUserId(Long userId);

    /**
     * 根据用户ID查询菜单
     *
     * @return 菜单列表
     */
    @Select("select menu_id,menu_name,parent_id,order_num,path,component,`query`,is_frame,is_cache,menu_type,visible,status,ifnull(perms,'') as perms,icon,create_time from sys_menu where menu_type in ('M','C') and status=0 order by parent_id,order_num")
    List<SysMenu> selectMenuTreeAll();

    /**
     * 根据用户ID查询菜单
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    @Select("select distinct m.menu_id,m.parent_id,m.menu_name,m.path,m.component,m.`query`,m.visible,m.status,ifnull(m.perms,'') as perms,m.is_frame,m.is_cache,m.menu_type,m.icon,m.order_num,m.create_time from sys_menu m left join sys_role_menu rm on m.menu_id=rm.menu_id left join sys_user_role ur on rm.role_id=ur.role_id where ur.user_id=#{userId} and m.menu_type in ('M','C') and m.status=0 order by m.parent_id,m.order_num")
    List<SysMenu> selectMenuTreeByUserId(Long userId);

    /**
     * 根据角色ID查询菜单树信息
     * 
     * @param roleId 角色ID
     * @param menuCheckStrictly 菜单树选择项是否关联显示
     * @return 选中菜单列表
     */
    @Select("<script>select m.menu_id from sys_menu m left join sys_role_menu rm on m.menu_id=rm.menu_id where rm.role_id=#{roleId}" +
            "<if test=\"menuCheckStrictly\"> and m.menu_id not in (select m.parent_id from sys_menu m inner join sys_role_menu rm on m.menu_id = rm.menu_id and rm.role_id = #{roleId})</if>" +
            " order by m.parent_id,m.order_num</script>")
    List<Long> selectMenuListByRoleId(@Param("roleId") Long roleId, @Param("menuCheckStrictly") boolean menuCheckStrictly);

    /**
     * 是否存在菜单子节点
     *
     * @param menuId 菜单ID
     * @return 结果
     */
    @Select("select count(1) from sys_menu where parent_id=#{menuId}")
    int hasChildByMenuId(Long menuId);

    /**
     * 校验菜单名称是否唯一
     *
     * @param menuName 菜单名称
     * @param parentId 父菜单ID
     * @return 结果
     */
    @Select("select menu_id,menu_name from sys_menu where menu_name=#{menuName} and parent_id=#{parentId} limit 1")
    SysMenu checkMenuNameUnique(@Param("menuName") String menuName, @Param("parentId") Long parentId);
}
