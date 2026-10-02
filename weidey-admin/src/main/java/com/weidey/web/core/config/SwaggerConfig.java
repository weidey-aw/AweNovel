package com.weidey.web.core.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import com.weidey.common.config.AweNovelConfig;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Swagger的接口配置
 * 
 * @author weidey
 */
@Configuration
public class SwaggerConfig implements WebMvcConfigurer
{
    /** 系统基础配置 */
    @Autowired
    private AweNovelConfig aweNovelConfig;


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        /**
         * 配置swagger-ui显示文档
         */
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");


    }
}
