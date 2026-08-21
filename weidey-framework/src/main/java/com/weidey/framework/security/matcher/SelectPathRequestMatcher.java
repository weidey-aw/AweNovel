package com.weidey.framework.security.matcher;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.regex.Pattern;

public class SelectPathRequestMatcher implements RequestMatcher {
    private static final Pattern SELECT_PATTERN = Pattern.compile("^(?:/.*/)*select(?:/.*)?$");
//    private static final Pattern SELECT_PATTERN = Pattern.compile("^/select/(detail|list|)$");

    @Override
    public boolean matches(HttpServletRequest request) {
        return SELECT_PATTERN.matcher(request.getServletPath()).matches();
    }
}