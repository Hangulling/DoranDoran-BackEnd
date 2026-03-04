package com.dorandoran.user.service;

import com.dorandoran.user.entity.FcmToken;
import com.dorandoran.user.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;

    @Transactional
    public FcmToken registerToken(UUID userId, String token, String platform) {
        String normalizedToken = token != null ? token.trim() : "";
        String normalizedPlatform = platform != null && !platform.isBlank() ? platform.trim() : "unknown";
        if (log.isDebugEnabled() && (token != null && !token.equals(normalizedToken))) {
            log.debug("[FCM] registerToken 토큰 정규화: userId={}, trimmed={}", userId, !token.equals(normalizedToken));
        }
        return fcmTokenRepository.findByUserIdAndToken(userId, normalizedToken)
            .map(existing -> {
                if (!normalizedPlatform.equals(existing.getPlatform())) {
                    existing.setPlatform(normalizedPlatform);
                }
                return fcmTokenRepository.save(existing);
            })
            .orElseGet(() -> fcmTokenRepository.save(
                FcmToken.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .token(normalizedToken)
                    .platform(normalizedPlatform)
                    .build()
            ));
    }

    @Transactional(readOnly = true)
    public List<FcmToken> getTokens(UUID userId) {
        return fcmTokenRepository.findByUserId(userId);
    }

    @Transactional
    public void deleteToken(UUID tokenId) {
        fcmTokenRepository.deleteById(tokenId);
    }
}
