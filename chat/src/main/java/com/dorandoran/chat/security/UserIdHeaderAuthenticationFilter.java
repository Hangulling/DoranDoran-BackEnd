package com.dorandoran.chat.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserIdHeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(UserIdHeaderAuthenticationFilter.class);

    public static final String USER_ID_HEADER = "X-User-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        boolean isStartGreeting = request.getRequestURI() != null && request.getRequestURI().contains("start-greeting");
        
        // 필터 진입 시 SecurityContext 상태 확인
        if (isStartGreeting) {
            Authentication beforeAuth = SecurityContextHolder.getContext().getAuthentication();
            log.debug("UserIdHeaderAuthenticationFilter 진입: path={}, 기존 auth={}", 
                    request.getRequestURI(), (beforeAuth != null ? beforeAuth.getPrincipal() : "null"));
        }
        
        String userIdRaw = request.getHeader(USER_ID_HEADER);
        String userId = (userIdRaw != null) ? userIdRaw.trim() : null;
        if (userId != null && !userId.isBlank()) {
            try {
                UUID uuid = UUID.fromString(userId);
                Authentication auth = new UserIdAuthentication(uuid);
                SecurityContextHolder.getContext().setAuthentication(auth);
                
                if (isStartGreeting) {
                    // 설정 직후 확인
                    Authentication afterAuth = SecurityContextHolder.getContext().getAuthentication();
                    log.debug("UserIdHeaderAuthenticationFilter: X-User-Id 설정 완료 path={}, userId={}, 설정 후 auth={}", 
                            request.getRequestURI(), uuid, (afterAuth != null ? afterAuth.getPrincipal() : "null"));
                }
            } catch (IllegalArgumentException e) {
                if (isStartGreeting) {
                    log.warn("UserIdHeaderAuthenticationFilter: X-User-Id 값이 유효한 UUID가 아님 path={}, value=[{}]", request.getRequestURI(), userId);
                }
            }
        } else {
            if (isStartGreeting) {
                log.warn("UserIdHeaderAuthenticationFilter: X-User-Id 헤더 없음 또는 비어있음 path={}", request.getRequestURI());
            }
        }
        
        filterChain.doFilter(request, response);
        
        // 필터 체인 종료 후 SecurityContext 상태 확인
        if (isStartGreeting) {
            Authentication afterChainAuth = SecurityContextHolder.getContext().getAuthentication();
            log.debug("UserIdHeaderAuthenticationFilter 종료: path={}, 필터 체인 후 auth={}", 
                    request.getRequestURI(), (afterChainAuth != null ? afterChainAuth.getPrincipal() : "null"));
        }
    }

    static class UserIdAuthentication extends AbstractAuthenticationToken {
        private final UUID userId;

        public UserIdAuthentication(UUID userId) {
            super(defaultAuthorities());
            this.userId = userId;
            setAuthenticated(true);
        }

        @Override
        public Object getCredentials() {
            return "";
        }

        @Override
        public Object getPrincipal() {
            return userId;
        }

        @Override
        public String getName() {
            return userId.toString();
        }

        private static Collection<? extends GrantedAuthority> defaultAuthorities() {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
    }
}


