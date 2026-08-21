package com.weidey.community.controller;

import com.weidey.common.core.controller.BaseController;
import com.weidey.common.core.domain.AjaxResult;
import com.weidey.community.service.SignService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 签到接口
 *
 * @author weidey
 */
@RestController
@RequestMapping("/community/sign")
public class SignController extends BaseController {

    private final SignService signService;

    public SignController(SignService signService) {
        this.signService = signService;
    }

    /** 每日签到 */
    @PostMapping
    public AjaxResult sign() {
        return success(signService.doSign(getUserId()));
    }

    /** 签到状态（保持接口占位，具体状态由签到结果返回） */
    @GetMapping
    public AjaxResult status() {
        return success();
    }
}
