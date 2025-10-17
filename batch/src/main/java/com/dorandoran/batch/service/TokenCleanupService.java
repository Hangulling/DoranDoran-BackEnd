package com.dorandoran.batch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public int cleanupExpiredTokenBlacklist() {
        String sql = "DELETE FROM auth_schema.token_blacklist WHERE expires_at < NOW()";
        int affected = jdbcTemplate.update(sql);
        log.info("[TokenCleanupService] token_blacklist expired deleted = {}", affected);
        return affected;
    }

    @Transactional
    public int cleanupExpiredRefreshTokens() {
        String sql = "DELETE FROM auth_schema.refresh_tokens WHERE expires_at < NOW() OR revoked = TRUE";
        int affected = jdbcTemplate.update(sql);
        log.info("[TokenCleanupService] refresh_tokens expired/revoked deleted = {}", affected);
        return affected;
    }
}


