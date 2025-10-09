package com.dorandoran.shared.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 정보 DTO (Record 타입)
 */
public record UserDto(
    UUID id,
    String email,
    String firstName,
    String lastName,
    String name,
    String passwordHash,
    String picture,
    String info,
    LocalDateTime lastConnTime,
    UserStatus status,
    RoleName role,
    boolean coachCheck,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    
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
