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
    @org.springframework.beans.factory.annotation.Value("${email.verification.expiration-minutes:5}")
    private int expirationMinutes;

    public EmailVerification issue(com.dorandoran.auth.entity.User user, String rawToken, LocalDateTime expiresAt) {
        String hash = tokenBlacklistService.hashToken(rawToken);
        EmailVerification entity = EmailVerification.builder()
                .user(user)
                .tokenHash(hash)
                .expiresAt(expiresAt)
                .verified(false)
                .createdAt(LocalDateTime.now())
                .build();
        return emailVerificationRepository.save(entity);
    }

    public String issueWithDefaultExpiry(com.dorandoran.auth.entity.User user) {
        String rawToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);
        issue(user, rawToken, expiresAt);
        return rawToken;
    }

    public Optional<EmailVerification> findByRawToken(String rawToken) {
        return emailVerificationRepository.findByTokenHash(tokenBlacklistService.hashToken(rawToken));
    }

    public void markVerified(EmailVerification token) {
        token.setVerified(true);
        emailVerificationRepository.save(token);
    }

    public com.dorandoran.auth.entity.User verifyOrThrow(String rawToken) {
        EmailVerification token = findByRawToken(rawToken)
                .orElseThrow(() -> new com.dorandoran.common.exception.DoranDoranException(
                        com.dorandoran.common.exception.ErrorCode.AUTH_TOKEN_INVALID));

        if (token.isVerified()) {
            throw new com.dorandoran.common.exception.DoranDoranException(
                    com.dorandoran.common.exception.ErrorCode.AUTH_TOKEN_INVALID, "이미 사용된 토큰입니다.");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new com.dorandoran.common.exception.DoranDoranException(
                    com.dorandoran.common.exception.ErrorCode.AUTH_TOKEN_EXPIRED);
        }
        markVerified(token);
        return token.getUser();
    }
}


