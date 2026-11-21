package com.weidey.framework.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 测试包专用：单页应用（SPA）路由回退。
 *
 * <p>Spring Boot 直接托管前端静态资源时，`/game/30`、`/admin/operation/game` 这类前端路由
 * 在服务端并不存在，需要回退到对应的 `index.html`：</p>
 * <ul>
 *   <li>`/admin/**` → `/admin/index.html`（管理后台）</li>
 *   <li>其余非接口、无扩展名的 GET 请求 → `/index.html`（前台）</li>
 * </ul>
 *
 * @author weidey
 */
@Component
@Profile("test")
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class SpaFallbackFilter extends OncePerRequestFilter {

    /** 后端接口前缀：这些路径即使没有扩展名也不能回退到前端 */
    private static final List<String> API_PREFIXES = List.of(
            "/community", "/system", "/monitor", "/tool", "/common", "/ai", "/profile",
            "/druid", "/captchaImage", "/getInfo", "/getRouters", "/logout", "/code",
            "/forgetPwd", "/v3", "/swagger", "/swagger-resources", "/webjars", "/actuator",
            "/prod-api", "/live2d", "/live2d-core", "/static", "/assets", "/error"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        String path = uri.startsWith(contextPath) ? uri.substring(contextPath.length()) : uri;

        // 静态文件（含 .js/.css/.json/.png…）交给资源处理器
        if (path.contains(".")) {
            chain.doFilter(request, response);
            return;
        }

        // 管理后台路由
        if (path.equals("/admin") || path.startsWith("/admin/")) {
            request.getRequestDispatcher("/admin/index.html").forward(request, response);
            return;
        }

        // 接口路径不处理
        for (String prefix : API_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                chain.doFilter(request, response);
                return;
            }
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/index.html");
        dispatcher.forward(request, response);
    }
}
