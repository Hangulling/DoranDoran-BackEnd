package com.dorandoran.auth.service;

import com.dorandoran.auth.entity.EmailVerification;
import com.dorandoran.auth.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final TokenBlacklistService tokenBlacklistService;

    public EmailVerification issue(UUID userId, String rawToken, LocalDateTime expiresAt) {
        String hash = tokenBlacklistService.hashToken(rawToken);
        EmailVerification entity = EmailVerification.builder()
                .userId(userId)
                .tokenHash(hash)
                .expiresAt(expiresAt)
                .verified(false)
                .createdAt(LocalDateTime.now())
                .build();
        return emailVerificationRepository.save(entity);
    }

    public Optional<EmailVerification> findByRawToken(String rawToken) {
        return emailVerificationRepository.findByTokenHash(tokenBlacklistService.hashToken(rawToken));
    }

    public void markVerified(EmailVerification token) {
        token.setVerified(true);
        emailVerificationRepository.save(token);
    }
}


