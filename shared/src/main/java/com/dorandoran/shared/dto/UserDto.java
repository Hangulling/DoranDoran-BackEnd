package com.dorandoran.shared.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 정보 DTO (Record 타입)
 */
public record UserDto(
    String id,                    // UUID를 String으로 변환한 사용자 ID
    String email,                 // 이메일
    String firstName,             // 이름
    String lastName,              // 성
    String name,                  // 전체 이름
    @JsonIgnore String passwordHash,  // 비밀번호 해시 (JSON 직렬화 시 제외)
    String picture,               // 프로필 사진
    String info,                  // 사용자 정보
    String preferences,           // 사용자 설정 (프론트엔드용)
    LocalDateTime lastConnTime,   // 마지막 연결 시간
    UserStatus status,            // 사용자 상태
    RoleName role,                // 사용자 역할
    boolean coachCheck,           // 코치 여부
    LocalDateTime createdAt,      // 생성 시간
    LocalDateTime updatedAt       // 수정 시간
) {
    
    // UUID를 String으로 변환하는 생성자
    public UserDto(UUID id, String email, String firstName, String lastName, String name,
                   String passwordHash, String picture, String info, String preferences,
                   LocalDateTime lastConnTime, UserStatus status, RoleName role,
                   boolean coachCheck, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id.toString(), email, firstName, lastName, name, passwordHash, picture, info, 
             preferences, lastConnTime, status, role, coachCheck, createdAt, updatedAt);
    }
    
    /**
     * UserStatus 열거형
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
        
        public static UserStatus fromValue(String value) {
            for (UserStatus status : values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown user status: " + value);
        }
    }
    
    /**
     * RoleName 열거형
     */
    public enum RoleName {
        ROLE_USER,
        ROLE_ADMIN
    }
}
