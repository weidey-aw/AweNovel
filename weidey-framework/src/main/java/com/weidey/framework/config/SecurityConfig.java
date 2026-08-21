package com.weidey.framework.config;

import com.weidey.framework.security.matcher.SelectPathRequestMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.filter.CorsFilter;
import com.weidey.framework.config.properties.PermitAllUrlProperties;
import com.weidey.framework.security.filter.JwtAuthenticationTokenFilter;
import com.weidey.framework.security.handle.AuthenticationEntryPointImpl;
import com.weidey.framework.security.handle.LogoutSuccessHandlerImpl;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

/**
 * spring security配置
 *
 * @author ruoyi
 */
@EnableMethodSecurity(securedEnabled = true)
@Configuration
public class SecurityConfig
{
    /**
     * 自定义用户认证逻辑
     */
    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * 认证失败处理类
     */
    @Autowired
    private AuthenticationEntryPointImpl unauthorizedHandler;

    /**
     * 退出处理类
     */
    @Autowired
    private LogoutSuccessHandlerImpl logoutSuccessHandler;

    /**
     * token认证过滤器
     */
    @Autowired
    private JwtAuthenticationTokenFilter authenticationTokenFilter;

    /**
     * 跨域过滤器
     */
    @Autowired
    private CorsFilter corsFilter;

    /**
     * 允许匿名访问的地址
     */
    @Autowired
    private PermitAllUrlProperties permitAllUrl;

    /**
     * @return
     * @throws Exception
     */
    @Bean
    AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        // 身份认证接口
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(bCryptPasswordEncoder());
        return new ProviderManager(daoAuthenticationProvider);
    }




    /**
     * anyRequest | 匹配所有请求路径
     * access | SpringEl表达式结果为true时可以访问
     * anonymous | 匿名可以访问
     * denyAll | 用户不能访问
     * fullyAuthenticated | 用户完全认证可以访问（非remember-me下自动登录）
     * hasAnyAuthority | 如果有参数，参数表示权限，则其中任何一个权限可以访问
     * hasAnyRole | 如果有参数，参数表示角色，则其中任何一个角色可以访问
     * hasAuthority | 如果有参数，参数表示权限，则其权限可以访问
     * hasIpAddress | 如果有参数，参数表示IP地址，如果用户IP和参数匹配，则可以访问
     * hasRole | 如果有参数，参数表示角色，则其角色可以访问
     * permitAll | 用户可以任意访问
     * rememberMe | 允许通过remember-me登录的用户访问
     * authenticated | 用户登录后可访问
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                // CSRF禁用，因为不使用session
                .csrf(AbstractHttpConfigurer::disable)
                // 禁用HTTP响应标头
                .headers(header -> header.cacheControl(HeadersConfigurer.CacheControlConfig::disable).frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                // 认证失败处理类
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                // 基于token，所以不需要session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 注解标记允许匿名访问的url
                .authorizeHttpRequests((requests) -> {
                    // 允许匿名访问的URL列表
                    permitAllUrl.getUrls().forEach(url -> requests.requestMatchers(url).permitAll());
                    // 对于登录、注册、验证码等允许匿名访问的路径
                    requests.requestMatchers("/login", "/register", "/captchaImage", "/code", "/forgetPwd", "/forget/code", "/register/code").permitAll();
                    // 社区前台匿名可访问的 GET 接口
                    requests.requestMatchers(HttpMethod.GET,
                            "/community/game/tag/all",
                            "/community/game/*",
                            "/community/brand/list",
                            "/community/tag/list",
                            "/community/resource/list",
                            "/community/article/*",
                            "/community/comment/list",
                            "/register/code"
                    ).permitAll();
                    requests.requestMatchers(HttpMethod.GET, "/", "/*.html", "/**.html", "/**.css", "/**.js", "/profile/**").permitAll();
                    // Swagger UI 和 API 文档相关的路径
                    requests.requestMatchers("/v3/**", "/error","/swagger-ui/**", "/swagger-resources/**", "/webjars/**", "/*/api-docs", "/*/api-docs/**", "/druid/**").permitAll();
                    // 除上面外的所有请求全部需要鉴权认证
                    requests.anyRequest().authenticated();
                })
                // 添加Logout filter
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessHandler(logoutSuccessHandler))
                // 添加JWT filter
                .addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
                // 添加CORS filter
                .addFilterBefore(corsFilter, JwtAuthenticationTokenFilter.class)
                .addFilterBefore(corsFilter, LogoutFilter.class).build();
    }
    /**
     * 强散列哈希加密实现
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder()
    {
        return new BCryptPasswordEncoder();
    }
}
//              .antMatchers("/test/**","/login", "/register", "/captchaImage","/code","/**/select/**","/forgetPwd").anonymous()

