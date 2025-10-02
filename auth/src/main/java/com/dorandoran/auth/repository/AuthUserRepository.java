package com.dorandoran.auth.repository;

import com.dorandoran.auth.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Auth 사용자 Repository
 */
@Repository
public interface AuthUserRepository extends JpaRepository<AuthUser, UUID> {
    
    /**
     * 이메일로 사용자 조회
     */
    Optional<AuthUser> findByEmail(String email);
    
    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);
}
