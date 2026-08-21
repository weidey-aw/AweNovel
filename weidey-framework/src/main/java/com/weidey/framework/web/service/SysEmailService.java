package com.weidey.framework.web.service;

import cn.hutool.core.util.RandomUtil;
import com.weidey.common.core.domain.AjaxResult;
import com.weidey.common.core.domain.entity.SysUser;
import com.weidey.common.core.domain.model.EmailBody;
import com.weidey.common.core.redis.RedisCache;
import com.weidey.system.mapper.SysUserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;
import java.util.Date;


@Component
public class SysEmailService {
      @Resource
      SysUserMapper userMapper;
      @Resource
      SysAsyncService emailAsyncService;
      @Resource
      private RedisCache redisCache;


    public AjaxResult sendEmailCode(EmailBody emailBody) {

        final String KEY_REGISTER = "REGISTER";
        final String KEY_FORGET_PASS = "FORGET_PASS";

        SysUser user = userMapper.checkEmailUnique(emailBody.getEmail());

        // 设置验证码和其他信息
        emailBody.setCode(RandomUtil.randomNumbers(6));
        emailBody.setNow(new Date());

        // 发送邮件
        switch (emailBody.getEmailType()) {
            case KEY_REGISTER: {
                // 注册
                if (user != null) {
                    return AjaxResult.error("该邮箱已被注册");
                }
                if (isCacheExpired(emailBody.getEmailType(), emailBody.getEmail())) {
                    return AjaxResult.error("验证码已发送，请勿频繁操作");
                }
                setEmailContent(emailBody, "欢迎注册阿伟博客", "您的验证码为：%s，请在10分钟内填写，如非本人操作，请忽略本邮件。");
                break;
            }
            case KEY_FORGET_PASS: {
                // 忘记密码
                if (user == null) {
                    return AjaxResult.error("该用户不存在");
                }
                if (isCacheExpired(emailBody.getEmailType(), emailBody.getEmail())) {
                    return AjaxResult.error("验证码已发送，请勿频繁操作");
                }
                setEmailContent(emailBody, "阿伟博客密码重置", "您的验证码为：%s，请在10分钟内填写，如非本人操作，请忽略本邮件。");
                break;
            }
            default: {
                return AjaxResult.error("操作异常");
            }
        }
        // 发送邮箱
        emailAsyncService.send(emailBody);
        // 保存验证码
        emailAsyncService.saveCode(emailBody);
        // 邮件发送完成
        return AjaxResult.success("验证码已发送，请注意查收");
    }

    /**
     * 判断验证码是否过期
     * @param emailType 邮箱类型
     * @param email 邮箱地址
     * @return 是否过期
     */
    private boolean isCacheExpired(String emailType, String email) {
        Object cachedObject = redisCache.getCacheObject(emailType + ":" + email);
        long ttl = redisCache.getExpire(emailType + ":" + email, TimeUnit.SECONDS);
        return cachedObject != null || ttl >= 540; // 如果没有缓存，认为未过期
    }

    /**
     * 设置邮件内容
     * @param emailBody 邮件内容
     * @param title 邮件标题
     * @param contentTemplate 邮件内容模板
     */
    private void setEmailContent(EmailBody emailBody, String title, String contentTemplate) {
        emailBody.setTitle(title);
        emailBody.setContent(String.format(contentTemplate, emailBody.getCode()));
    }


}
