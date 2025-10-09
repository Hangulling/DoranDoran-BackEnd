package com.dorandoran.auth.service;

import com.dorandoran.auth.entity.RefreshToken;
import com.dorandoran.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshToken issue(com.dorandoran.auth.entity.User user, String tokenHash, LocalDateTime expiresAt, String deviceId, String userAgent, String ip) {
        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .token(tokenHash)
                .issuedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .revoked(false)
                .deviceId(deviceId)
                .userAgent(userAgent)
                .ipAddress(ip)
                .build();
        return refreshTokenRepository.save(entity);
    }

    public Optional<RefreshToken> findByHash(String tokenHash) {
        return refreshTokenRepository.findByToken(tokenHash);
    }

    public void revoke(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    public RefreshToken rotate(RefreshToken current, String newTokenHash, LocalDateTime newExpiresAt) {
        current.setRevoked(true);
        refreshTokenRepository.save(current);

        RefreshToken next = RefreshToken.builder()
                .user(current.getUser())
                .token(newTokenHash)
                .issuedAt(LocalDateTime.now())
                .expiresAt(newExpiresAt)
                .revoked(false)
                .rotatedFromId(current.getId())
                .deviceId(current.getDeviceId())
                .userAgent(current.getUserAgent())
                .ipAddress(current.getIpAddress())
                .build();
        return refreshTokenRepository.save(next);
    }
}


