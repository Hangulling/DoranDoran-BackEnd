package com.dorandoran.shared.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 사용자 정보 수정 요청 DTO (Record 타입)
 */
public record UpdateUserRequest(
    @Email(message = "올바른 이메일 형식이 아닙니다")
    String email,
    
    @Size(min = 1, max = 50, message = "이름은 1-50자 사이여야 합니다")
    String firstName,
    
    @Size(min = 1, max = 50, message = "성은 1-50자 사이여야 합니다")
    String lastName,
    
    @Size(min = 1, max = 50, message = "표시명은 1-50자 사이여야 합니다")
    String name,
    
    String picture,
    
    @Size(max = 100, message = "정보는 100자를 초과할 수 없습니다")
    String info,
    
    UserDto.UserStatus status,
    
    Boolean coachCheck
) {
    
    /**
     * 표시명이 없으면 firstName + lastName으로 생성
     */
    public String getDisplayName() {
        return name != null && !name.trim().isEmpty() 
            ? name 
            : (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
}
