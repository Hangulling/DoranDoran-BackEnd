package com.dorandoran.shared.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 비밀번호 해시를 포함한 사용자 정보 DTO (Auth 서비스용)
 */
public record UserWithPasswordDto(
    String id,
    String email,
    String firstName,
    String lastName,
    String name,
    String passwordHash,  // @JsonIgnore 없음
    String picture,
    String info,
    String preferences,
    LocalDateTime lastConnTime,
    UserDto.UserStatus status,
    UserDto.RoleName role,
    boolean coachCheck,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    
    // UUID를 String으로 변환하는 생성자
    public UserWithPasswordDto(UUID id, String email, String firstName, String lastName, String name,
                              String passwordHash, String picture, String info, String preferences,
                              LocalDateTime lastConnTime, UserDto.UserStatus status, UserDto.RoleName role,
                              boolean coachCheck, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id.toString(), email, firstName, lastName, name, passwordHash, picture, info, 
             preferences, lastConnTime, status, role, coachCheck, createdAt, updatedAt);
    }
}
