package com.dorandoran.auth.service;

import com.dorandoran.auth.entity.PasswordResetToken;
import com.dorandoran.auth.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TokenBlacklistService tokenBlacklistService;

    public PasswordResetToken issue(UUID userId, String rawToken, LocalDateTime expiresAt) {
        String hash = tokenBlacklistService.hashToken(rawToken);
        PasswordResetToken entity = PasswordResetToken.builder()
                .userId(userId)
                .tokenHash(hash)
                .expiresAt(expiresAt)
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
        return passwordResetTokenRepository.save(entity);
    }

    public Optional<PasswordResetToken> findByRawToken(String rawToken) {
        return passwordResetTokenRepository.findByTokenHash(tokenBlacklistService.hashToken(rawToken));
    }

    public void markUsed(PasswordResetToken token) {
        token.setUsed(true);
        passwordResetTokenRepository.save(token);
    }
    
    /**
     * 사용자의 모든 비밀번호 재설정 토큰 무효화
     */
    public void invalidateUserTokens(UUID userId) {
        passwordResetTokenRepository.deleteByUserId(userId);
    }
}


