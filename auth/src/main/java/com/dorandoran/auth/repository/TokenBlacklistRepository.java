package com.dorandoran.auth.repository;

import com.dorandoran.auth.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {

    Optional<TokenBlacklist> findByTokenHash(String tokenHash);
}


