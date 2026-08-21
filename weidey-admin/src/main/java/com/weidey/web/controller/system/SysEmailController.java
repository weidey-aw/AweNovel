package com.weidey.web.controller.system;


import com.weidey.common.core.domain.AjaxResult;
import com.weidey.common.core.domain.model.EmailBody;
import com.weidey.common.core.domain.param.EmailCodeParam;
import com.weidey.framework.web.service.SysEmailService;
import com.weidey.system.service.ISysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.weidey.common.core.domain.AjaxResult.error;

//@Api(tags = "获取邮箱验证码")
@RestController
public class SysEmailController {

    final String KEY_REGISTER = "REGISTER";
    final String KEY_FORGET_PASS = "FORGET_PASS";

    @Autowired
    SysEmailService emailService;

    @Autowired
    private ISysConfigService configService;


//    @ApiOperation("获取注册邮箱验证码")
    @PostMapping("/code")
    public AjaxResult getEmailCode(@Validated @RequestBody EmailCodeParam emailCodeParam){
        return sendCode(emailCodeParam.getEmail(), emailCodeParam.getType());
    }

    /** 获取注册邮箱验证码（前端 GET 形式，type 固定 REGISTER） */
    @GetMapping("/register/code")
    public AjaxResult getRegisterCode(@RequestParam("email") String email){
        return sendCode(email, KEY_REGISTER);
    }

    /** 获取忘记密码邮箱验证码（type 固定 FORGET_PASS） */
    @GetMapping("/forget/code")
    public AjaxResult getForgetCode(@RequestParam("email") String email){
        return sendCode(email, KEY_FORGET_PASS);
    }

    private AjaxResult sendCode(String email, String type){
        if (!("true".equals(configService.selectConfigByKey("sys.account.enable.Email"))))
        {
            return error("当前系统没有开启邮箱功能！");
        }
        EmailBody emailBody = new EmailBody();
        emailBody.setEmail(email);
        switch (type){
            case KEY_REGISTER:
                emailBody.setEmailType(KEY_REGISTER);
                break;
            case KEY_FORGET_PASS:
                emailBody.setEmailType(KEY_FORGET_PASS);
                break;
            default:
                return error("非法的邮箱请求");
        }
        return emailService.sendEmailCode(emailBody);
    }
}
