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
//    /**
//     * 自定义的 OpenAPI 对象
//     */
//    @Bean
//    public OpenAPI customOpenApi()
//    {
//        return new OpenAPI().components(new Components()
//                        // 设置认证的请求头
//                        .addSecuritySchemes("apikey", securityScheme()))
//                .addSecurityItem(new SecurityRequirement().addList("apikey"))
//                .info(getApiInfo());
//    }

//    @Bean
//    public SecurityScheme securityScheme()
//    {
//        return new SecurityScheme()
//                .type(SecurityScheme.Type.APIKEY)
//                .name("Authorization")
//                .in(SecurityScheme.In.HEADER)
//                .scheme("Bearer");
//    }

//    /**
//     * 添加摘要信息
//     */
//    public Info getApiInfo()
//    {
//        return new Info()
//                // 设置标题
//                .title("标题：若依管理系统_接口文档")
//                // 描述
//                .description("描述：用于管理集团旗下公司的人员信息,具体包括XXX,XXX模块...")
//                // 作者信息
//                .contact(new Contact().name(aweNovelConfig.getName()))
//                // 版本
//                .version("版本号:" + aweNovelConfig.getVersion());
//    }
}
