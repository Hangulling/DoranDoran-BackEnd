package com.dorandoran.batch.job;

import com.dorandoran.batch.common.TimeProvider;
import com.dorandoran.batch.service.TokenCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupJob {

    private final TokenCleanupService tokenCleanupService;
    private final TimeProvider timeProvider;

    /**
     * 1. 만료된 토큰 정리 (Token Cleanup Job)
     * 매일 새벽 3시 실행
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void cleanupExpiredTokens() {
        var now = timeProvider.nowKst();
        log.info("[TokenCleanupJob] Start at {}", now);
        int delBlacklist = tokenCleanupService.cleanupExpiredTokenBlacklist();
        int delRefresh = tokenCleanupService.cleanupExpiredRefreshTokens();
        log.info("[TokenCleanupJob] Finished. blacklist={}, refresh_tokens={}", delBlacklist, delRefresh);
    }
}


