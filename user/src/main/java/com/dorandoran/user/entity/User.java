package com.dorandoran.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 사용자 엔티티 (User 서비스)
 */
@Entity
@Table(name = "app_user", schema = "user_schema")
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
    
    @Column(name = "password_hash", nullable = true, length = 100)
    private String passwordHash;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", length = 20)
    private OAuthProvider oauthProvider;
    
    @Column(name = "oauth_id", length = 255)
    private String oauthId;
    
    @Column(name = "picture")
    private String picture;
    
    @Column(name = "info", nullable = false, length = 100)
    @Builder.Default
    private String info = "";
    
    @Column(name = "birth_date", nullable = false)
    @Builder.Default
    private LocalDate birthDate = LocalDate.of(1900, 1, 1);
    
    @Column(name = "signup_question", nullable = false, length = 255)
    @Builder.Default
    private String signupQuestion = "질문이 설정되지 않았습니다.";
    
    @Column(name = "signup_answer", nullable = false, length = 30)
    @Builder.Default
    private String signupAnswer = "답변이 설정되지 않았습니다.";
    
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
    
    @Column(name = "exit_modal_do_not_show_again", nullable = false)
    @Builder.Default
    private boolean exitModalDoNotShowAgain = false;
    
    @Column(name = "is_onboard", nullable = false)
    @Builder.Default
    private boolean isOnboard = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // User 서비스 내부 연관관계
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private UserProfile profile;
    
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<UserSetting> settings = new ArrayList<>();
    
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
     * OAuth 제공자 열거형
     */
    public enum OAuthProvider {
        GOOGLE,
        FACEBOOK,
        KAKAO,
        NAVER
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
    
    /**
     * 나가기 모달 다시 보지 않기 설정 업데이트
     */
    public void updateExitModalDoNotShowAgain(boolean exitModalDoNotShowAgain) {
        this.exitModalDoNotShowAgain = exitModalDoNotShowAgain;
    }
    
    /**
     * 온보딩 완료 여부 업데이트
     */
    public void updateOnboard(boolean isOnboard) {
        this.isOnboard = isOnboard;
    }
    
    /**
     * 인증 방법 검증
     * passwordHash가 null이면 반드시 oauthProvider와 oauthId가 있어야 함
     * oauthProvider가 null이면 반드시 passwordHash가 있어야 함
     * 
     * @throws IllegalStateException 인증 방법이 올바르지 않은 경우
     */
    public void validateAuthMethod() {
        boolean hasPassword = passwordHash != null && !passwordHash.trim().isEmpty();
        boolean hasOAuth = oauthProvider != null && oauthId != null && !oauthId.trim().isEmpty();
        
        if (!hasPassword && !hasOAuth) {
            throw new IllegalStateException("비밀번호 또는 OAuth 인증 정보 중 하나는 반드시 필요합니다.");
        }
    }
}
