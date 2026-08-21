package com.weidey.common.utils.email;

import cn.hutool.core.util.StrUtil;
import com.weidey.common.core.domain.model.EmailBody;
import com.weidey.common.core.redis.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class EmailUtils {
    @Autowired
    private  RedisCache redisCache;


    /**
     *  邮箱校验工具
     * @param str 邮箱
     * @return boolean
     */
    public  boolean checkQQ(String str) {
        // matches()方法告知此字符串是否匹配给定的正则表达式。
        return str.matches("[1-9][0-9]+@qq.com");
    }

    /**
     *  校验邮箱是否正确
     */
    public  boolean checkEmail(String email) {
        return StrUtil.isBlank(email) && checkQQ(email);
    }

    /**
     * 校验验证码
     */
    public  Boolean verifyCode(String emailType, String email, String code) {
        //查询缓存
        try {
            EmailBody emailCache =  redisCache.getCacheObject(emailType + ":" + email);
            //校验成功后删除缓存
//            redisCache.deleteObject(emailType + ":" + email);
            return emailCache.getCode().equals(code);
        }catch ( Exception e){
            return  false;
        }
    }


}
