package com.dorandoran.auth.config;

import com.dorandoran.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// @Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class CleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    // 매 30분마다 만료/폐기된 토큰 정리(간단 버전: 블랙리스트는 JPA로 직접 삭제 대신 보존하고, 리프레시만 정리)
    @Scheduled(cron = "0 */30 * * * *")
    public void cleanupExpired() {
        try {
            long deleted = refreshTokenRepository.deleteByUserIdAndRevokedIsTrueOrExpiresAtBefore(null, LocalDateTime.now());
            log.debug("정리된 리프레시 토큰 수: {}", deleted);
        } catch (Exception e) {
            log.warn("만료 정리 작업 실패", e);
        }
    }
}


