package com.weidey.framework.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 测试包专用：剥掉前端统一的 `/api` 前缀。
 *
 * <p>前端（web / admin）所有请求都走 `/api/xxx`，开发态由 Vite 代理剥离、生产由 Nginx 剥离。
 * 一键测试包由 Spring Boot 直接托管前端静态资源，因此在这里用过滤器做同样的剥离，
 * 使同一个 jar 既能当接口服务又能当 Web 容器。</p>
 *
 * @author weidey
 */
@Component
@Profile("test")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiPrefixFilter extends OncePerRequestFilter {

    private static final String PREFIX = "/api";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        String path = uri.startsWith(contextPath) ? uri.substring(contextPath.length()) : uri;
        if (path.equals(PREFIX) || path.startsWith(PREFIX + "/")) {
            String newPath = path.substring(PREFIX.length());
            if (newPath.isEmpty()) {
                newPath = "/";
            }
            String target = newPath;
            String base = contextPath == null ? "" : contextPath;
            chain.doFilter(new HttpServletRequestWrapper(request) {
                @Override
                public String getRequestURI() {
                    return base + target;
                }

                @Override
                public String getServletPath() {
                    return target;
                }

                @Override
                public StringBuffer getRequestURL() {
                    StringBuffer url = new StringBuffer();
                    url.append(getScheme()).append("://").append(getServerName());
                    int port = getServerPort();
                    if (port > 0 && port != 80 && port != 443) {
                        url.append(':').append(port);
                    }
                    return url.append(base).append(target);
                }
            }, response);
            return;
        }
        chain.doFilter(request, response);
    }
}
