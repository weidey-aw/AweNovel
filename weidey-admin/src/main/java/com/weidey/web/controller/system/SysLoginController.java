package com.weidey.web.controller.system;

import java.util.List;
import java.util.Set;

import com.weidey.common.core.domain.model.ForgetBody;
import com.weidey.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.weidey.common.constant.Constants;
import com.weidey.common.core.domain.AjaxResult;
import com.weidey.common.core.domain.entity.SysMenu;
import com.weidey.common.core.domain.entity.SysUser;
import com.weidey.common.core.domain.model.LoginBody;
import com.weidey.common.utils.SecurityUtils;
import com.weidey.framework.web.service.SysLoginService;
import com.weidey.framework.web.service.SysPermissionService;
import com.weidey.system.service.ISysMenuService;

/**
 * 登录验证
 * 
 * @author ruoyi
 */
//@Api(tags = "登录验证")
@RestController
public class SysLoginController
{
    @Autowired
    private SysLoginService loginService;

    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private ISysUserService userService;

    /**
     * 登录方法
     * 
     * @param loginBody 登录信息
     * @return 结果
     */
//    @ApiOperation(value = "登录方法")
    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginBody loginBody)
    {
        AjaxResult ajax = AjaxResult.success();
        // 生成令牌
        String token = loginService.login(loginBody.getUsername(), loginBody.getPassword(), loginBody.getCode(),
                loginBody.getUuid());
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }


    /**
     * 忘记密码
     */
//    @ApiOperation(value = "忘记密码")
    @PutMapping("/forgetPwd")
    public AjaxResult forgetPwd(@RequestBody ForgetBody forgetBody){
        //forgetBody
        // 1.校验验证码
        userService.checkEmailCode(forgetBody);
        // 2.根据邮箱查询用户信息
        SysUser user = userService.selectUserByUserEmail(forgetBody.getEmail());
        // 3.修改密码
        user.setPassword(SecurityUtils.encryptPassword(forgetBody.getPassword()));
        user.setUpdateBy(user.getUserName());
        if (userService.resetPwd(user)>0){
            return AjaxResult.success("重置密码成功");
        }else {
            return AjaxResult.error("重置密码失败");
        }
    }

    /**
     * 获取用户信息
     * 
     * @return 用户信息
     */
//    @ApiOperation(value = "获取用户信息")
    @GetMapping("getInfo")
    public AjaxResult getInfo()
    {
        SysUser user = SecurityUtils.getLoginUser().getUser();
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(user);
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(user);

        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles);
        ajax.put("permissions", permissions);
        return ajax;
    }

    /**
     * 获取路由信息
     * 
     * @return 路由信息
     */
    @GetMapping("getRouters")
    public AjaxResult getRouters()
    {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return AjaxResult.success(menuService.buildMenus(menus));
    }
}
