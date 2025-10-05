package com.dorandoran.auth.repository;

import com.dorandoran.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    long deleteByUserIdAndRevokedIsTrueOrExpiresAtBefore(java.util.UUID userId, LocalDateTime time);
}


