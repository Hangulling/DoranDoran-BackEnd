package com.dorandoran.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 사용자 엔티티 (Chat 서비스)
 */
@Entity
@Table(name = "app_user", schema = "chat_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "email", unique = true, nullable = false, length = 320)
    private String email;
    
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;
    
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;
    
    @Column(name = "name", nullable = false, length = 50)
    private String name;
    
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    
    @Column(name = "picture")
    private String picture;
    
    @Column(name = "info", nullable = false, length = 100)
    @Builder.Default
    private String info = "";
    
    @Column(name = "last_conn_time", nullable = false)
    @Builder.Default
    private LocalDateTime lastConnTime = LocalDateTime.now();
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private RoleName role = RoleName.ROLE_USER;

    @Column(name = "coach_check", nullable = false)
    @Builder.Default
    private boolean coachCheck = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Chat 서비스 내부 연관관계
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ChatRoom> chatRooms = new ArrayList<>();
    
    // 주의: billing 엔티티들과의 연관관계는 각 billing 엔티티에서 관리
    // 순환 참조를 피하기 위해 User 엔티티에서는 참조하지 않음
    
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
     * 사용자 정보 업데이트
     */
    public void updateInfo(String firstName, String lastName, String name, String picture, String info) {
        if (firstName != null) this.firstName = firstName;
        if (lastName != null) this.lastName = lastName;
        if (name != null) this.name = name;
        if (picture != null) this.picture = picture;
        if (info != null) this.info = info;
    }
    
    /**
     * 상태 업데이트
     */
    public void updateStatus(UserStatus status) {
        this.status = status;
    }
    
    /**
     * 코치 체크 상태 업데이트
     */
    public void updateCoachCheck(boolean coachCheck) {
        this.coachCheck = coachCheck;
    }
    
    /**
     * 마지막 연결 시간 업데이트
     */
    public void updateLastConnectionTime() {
        this.lastConnTime = LocalDateTime.now();
    }
    
    /**
     * 비밀번호 업데이트
     */
    public void updatePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
