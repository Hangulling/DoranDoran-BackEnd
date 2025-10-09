package com.dorandoran.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 사용자 업데이트 요청 DTO
 */
public record UpdateUserRequest(
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 50, message = "이름은 50자를 초과할 수 없습니다")
    String firstName,
    
    @NotBlank(message = "성은 필수입니다")
    @Size(max = 50, message = "성은 50자를 초과할 수 없습니다")
    String lastName,
    
    @NotBlank(message = "표시 이름은 필수입니다")
    @Size(max = 50, message = "표시 이름은 50자를 초과할 수 없습니다")
    String name,
    
    @Size(max = 500, message = "사진 URL은 500자를 초과할 수 없습니다")
    String picture,
    
    @Size(max = 100, message = "정보는 100자를 초과할 수 없습니다")
    String info
) {}
