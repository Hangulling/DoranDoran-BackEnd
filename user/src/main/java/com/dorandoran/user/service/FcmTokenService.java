package com.dorandoran.user.service;

import com.dorandoran.user.entity.FcmToken;
import com.dorandoran.user.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;

    @Transactional
    public FcmToken registerToken(UUID userId, String token, String platform) {
        return fcmTokenRepository.findByUserIdAndToken(userId, token)
            .map(existing -> {
                if (platform != null && !platform.isBlank() && !platform.equals(existing.getPlatform())) {
                    existing.setPlatform(platform);
                }
                return fcmTokenRepository.save(existing);
            })
            .orElseGet(() -> fcmTokenRepository.save(
                FcmToken.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .token(token)
                    .platform(platform)
                    .build()
            ));
    }

    @Transactional(readOnly = true)
    public List<FcmToken> getTokens(UUID userId) {
        return fcmTokenRepository.findByUserId(userId);
    }
}
