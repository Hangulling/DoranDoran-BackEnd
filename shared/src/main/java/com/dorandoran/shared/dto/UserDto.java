package com.dorandoran.shared.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 정보 DTO (Record 타입)
 */
public record UserDto(
    String id,              // UUID -> String으로 변경
    String email,
    String firstName,
    String lastName,
    String name,
    @JsonIgnore String passwordHash,
    String picture,
    String info,
    String preferences,     // 프론트엔드에 맞춰 추가
    LocalDateTime lastConnTime,
    UserStatus status,
    RoleName role,
    boolean coachCheck,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
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
