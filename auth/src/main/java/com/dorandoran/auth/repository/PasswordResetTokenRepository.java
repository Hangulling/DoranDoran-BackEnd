package com.dorandoran.auth.repository;

import com.dorandoran.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    
    void deleteByUser(com.dorandoran.auth.entity.User user);
}


