package com.dorandoran.chat.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    public static final String USER_ID_HEADER = "X-User-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId != null && !userId.isBlank()) {
            try {
                UUID uuid = UUID.fromString(userId);
                Authentication auth = new UserIdAuthentication(uuid);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (IllegalArgumentException ignored) {
            }
        }
        filterChain.doFilter(request, response);
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


