package com.dorandoran.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Auth 서비스 사용자 엔티티 (인증 정보만 관리)
 */
@Entity
@Table(name = "users", schema = "auth")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUser {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "email", unique = true, nullable = false, length = 320)
    private String email;
    
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private RoleName role = RoleName.ROLE_USER;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * 사용자 상태 열거형
     */
    public enum UserStatus {
        ACTIVE("active"),
        INACTIVE("inactive"),
        SUSPENDED("suspended");
        
        private final String value;
        
        UserStatus(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }

    public enum RoleName {
        ROLE_USER,
        ROLE_ADMIN
    }
    
    /**
     * 마지막 로그인 시간 업데이트
     */
    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now();
    }
    
    /**
     * 상태 업데이트
     */
    public void updateStatus(UserStatus status) {
        this.status = status;
    }
}
