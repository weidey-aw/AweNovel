package com.weidey.web.controller.system;

import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import com.weidey.common.config.AweNovelConfig;
import com.weidey.common.core.domain.model.EmailBody;
import com.weidey.common.utils.file.FileUploadUtils;
import com.weidey.common.utils.file.MimeTypeUtils;
import com.weidey.framework.web.service.SysAsyncService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.weidey.common.annotation.Log;
import com.weidey.common.core.controller.BaseController;
import com.weidey.common.core.domain.AjaxResult;
import com.weidey.common.core.domain.entity.SysRole;
import com.weidey.common.core.domain.entity.SysUser;
import com.weidey.common.core.page.TableDataInfo;
import com.weidey.common.enums.BusinessType;
import com.weidey.common.utils.SecurityUtils;
import com.weidey.common.utils.StringUtils;
import com.weidey.common.utils.poi.ExcelUtil;
import com.weidey.system.service.ISysRoleService;
import com.weidey.system.service.ISysUserService;

/**
 * 用户信息
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/user")
public class  SysUserController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(SysUserController.class);

    /** 系统自动重置密码长度 */
    private static final int AUTO_PASSWORD_LENGTH = 8;
    /** 数字池（排除易混淆的 0、1） */
    private static final String PASSWORD_DIGITS = "23456789";
    /** 字母池（排除易混淆的 O、l、I） */
    private static final String PASSWORD_LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";
    /** 混合池 */
    private static final String PASSWORD_POOL = PASSWORD_DIGITS + PASSWORD_LETTERS;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysRoleService roleService;

    @Resource
    SysAsyncService emailAsyncService;

    /**
     * 获取用户列表
     */
//    @ApiOperation("获取用户列表")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysUser user)
    {
        return getDataTable(userService.selectUserPage(startPage(), user));
    }

    @Log(title = "用户管理", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('system:user:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysUser user)
    {
        List<SysUser> list = userService.selectUserList(user);
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        util.exportExcel(response, list, "用户数据");
    }

    @Log(title = "用户管理", businessType = BusinessType.IMPORT)
    @PreAuthorize("@ss.hasPermi('system:user:import')")
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        List<SysUser> userList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = userService.importUser(userList, updateSupport, operName);
        return success(message);
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        util.importTemplateExcel(response, "用户数据");
    }

    /**
     * 根据用户编号获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping(value = { "/", "/{userId}" })
    public AjaxResult getInfo(@PathVariable(value = "userId", required = false) Long userId)
    {
        userService.checkUserDataScope(userId);
        AjaxResult ajax = AjaxResult.success();
        List<SysRole> roles = roleService.selectRoleAll();
        ajax.put("roles", SysUser.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        if (StringUtils.isNotNull(userId))
        {
            SysUser sysUser = userService.selectUserById(userId);
            ajax.put(AjaxResult.DATA_TAG, sysUser);
            ajax.put("roleIds", sysUser.getRoles().stream().map(SysRole::getRoleId).collect(Collectors.toList()));
        }
        return ajax;
    }

    /**
     * 新增用户
     */
    @PreAuthorize("@ss.hasPermi('system:user:add')")
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysUser user)
    {
        if (!userService.checkUserNameUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        else if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        else if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setCreateBy(getUsername());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        return toAjax(userService.insertUser(user));
    }

    /**
     * 修改用户
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysUser user)
    {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        if (!userService.checkUserNameUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        else if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        else if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setUpdateBy(getUsername());
        return toAjax(userService.updateUser(user));
    }

    /**
     * 删除用户
     */
    @PreAuthorize("@ss.hasPermi('system:user:remove')")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable("userIds") Long[] userIds)
    {
        if (ArrayUtils.contains(userIds, getUserId()))
        {
            return error("当前用户不能删除");
        }
        return toAjax(userService.deleteUserByIds(userIds));
    }

    /**
     * 重置密码（系统自动生成 8 位数字+字母随机密码，并邮件通知用户）
     */
    @PreAuthorize("@ss.hasPermi('system:user:resetPwd')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody SysUser user)
    {
        if (user.getUserId() == null)
        {
            return error("用户ID不能为空");
        }
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        String newPwd = generateAutoPassword();
        user.setPassword(SecurityUtils.encryptPassword(newPwd));
        user.setUpdateBy(getUsername());
        if (userService.resetPwd(user) <= 0)
        {
            return error("密码重置失败");
        }
        notifyPasswordReset(user.getUserId(), newPwd);
        return AjaxResult.success("密码重置成功，新密码已通过邮件发送给用户");
    }

    /**
     * 手动重置密码（管理员指定新密码，需要独立权限 system:user:resetPwdManual）
     */
    @PreAuthorize("@ss.hasPermi('system:user:resetPwdManual')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwdManual")
    public AjaxResult resetPwdManual(@RequestBody SysUser user)
    {
        if (user.getUserId() == null)
        {
            return error("用户ID不能为空");
        }
        String newPwd = user.getPassword();
        if (StringUtils.isEmpty(newPwd))
        {
            return error("新密码不能为空");
        }
        if (newPwd.length() < 8 || newPwd.length() > 20)
        {
            return error("密码长度需在 8 到 20 个字符之间");
        }
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        user.setPassword(SecurityUtils.encryptPassword(newPwd));
        user.setUpdateBy(getUsername());
        if (userService.resetPwd(user) <= 0)
        {
            return error("密码重置失败");
        }
        notifyPasswordReset(user.getUserId(), newPwd);
        return AjaxResult.success("密码重置成功，新密码已通过邮件发送给用户");
    }

    /**
     * 修改指定用户头像（管理端）
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PostMapping("/avatar")
    public AjaxResult avatar(@RequestParam("userId") Long userId, @RequestParam("avatarfile") MultipartFile file)
            throws Exception
    {
        if (file == null || file.isEmpty())
        {
            return error("上传文件不能为空");
        }
        SysUser target = userService.selectUserById(userId);
        if (StringUtils.isNull(target))
        {
            return error("用户不存在");
        }
        userService.checkUserDataScope(userId);
        String avatar = FileUploadUtils.upload(AweNovelConfig.getAvatarPath(), file, MimeTypeUtils.IMAGE_EXTENSION);
        if (!userService.updateUserAvatar(target.getUserName(), avatar))
        {
            return error("修改头像失败");
        }
        AjaxResult ajax = AjaxResult.success();
        ajax.put("imgUrl", avatar);
        return ajax;
    }

    /**
     * 生成 8 位随机密码（至少包含 1 位数字与 1 位字母，排除易混淆字符）
     */
    private String generateAutoPassword()
    {
        char[] chars = new char[AUTO_PASSWORD_LENGTH];
        chars[0] = PASSWORD_DIGITS.charAt(RANDOM.nextInt(PASSWORD_DIGITS.length()));
        chars[1] = PASSWORD_LETTERS.charAt(RANDOM.nextInt(PASSWORD_LETTERS.length()));
        for (int i = 2; i < AUTO_PASSWORD_LENGTH; i++)
        {
            chars[i] = PASSWORD_POOL.charAt(RANDOM.nextInt(PASSWORD_POOL.length()));
        }
        // 打乱顺序，避免固定以数字+字母开头
        for (int i = chars.length - 1; i > 0; i--)
        {
            int j = RANDOM.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

    /**
     * 重置密码后邮件通知用户
     *
     * @param userId    用户ID
     * @param rawPassword 新密码明文（仅用于邮件正文）
     */
    private void notifyPasswordReset(Long userId, String rawPassword)
    {
        try
        {
            SysUser target = userService.selectUserById(userId);
            if (target == null)
            {
                log.warn("重置密码通知失败：用户不存在，userId={}", userId);
                return;
            }
            if (StringUtils.isEmpty(target.getEmail()))
            {
                log.warn("重置密码通知跳过：用户未绑定邮箱，userName={}", target.getUserName());
                return;
            }
            EmailBody emailBody = new EmailBody();
            emailBody.setEmail(target.getEmail());
            emailBody.setNow(new Date());
            emailBody.setTitle("AweNovel 提示：您的密码已被重置");
            emailBody.setContent("您好，" + StringUtils.nvl(target.getNickName(), target.getUserName()) + "：\n\n"
                    + "您的账号【" + target.getUserName() + "】密码已被管理员重置为：" + rawPassword + "\n"
                    + "请尽快登录并修改密码，切勿向他人泄露本邮件内容。\n\n"
                    + "如果这不是您本人的操作，请立即联系管理员。\n\n"
                    + "—— AweNovel");
            emailAsyncService.send(emailBody);
        }
        catch (Exception e)
        {
            // 邮件发送失败不影响密码重置结果
            log.error("重置密码通知发送失败，userId={}", userId, e);
        }
    }


    /**
     * 状态修改
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysUser user)
    {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        user.setUpdateBy(getUsername());
        return toAjax(userService.updateUserStatus(user));
    }

    /**
     * 根据用户编号获取授权角色
     */
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping("/authRole/{userId}")
    public AjaxResult authRole(@PathVariable("userId") Long userId)
    {
        AjaxResult ajax = AjaxResult.success();
        SysUser user = userService.selectUserById(userId);
        List<SysRole> roles = roleService.selectRolesByUserId(userId);
        ajax.put("user", user);
        ajax.put("roles", SysUser.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        return ajax;
    }

    /**
     * 用户授权角色
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.GRANT)
    @PutMapping("/authRole")
    public AjaxResult insertAuthRole(Long userId, Long[] roleIds)
    {
        userService.checkUserDataScope(userId);
        userService.insertUserAuth(userId, roleIds);
        return success();
    }
}
