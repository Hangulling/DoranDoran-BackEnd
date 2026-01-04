package com.dorandoran.shared.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 사용자 생성 요청 DTO (Record 타입)
 */
public record CreateUserRequest(
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    String email,
    
    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 1, max = 50, message = "이름은 1-50자 사이여야 합니다")
    String firstName,
    
    @NotBlank(message = "성은 필수입니다")
    @Size(min = 1, max = 50, message = "성은 1-50자 사이여야 합니다")
    String lastName,
    
    @NotBlank(message = "표시명은 필수입니다")
    @Size(min = 1, max = 50, message = "표시명은 1-50자 사이여야 합니다")
    String name,
    
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 100, message = "비밀번호는 8-100자 사이여야 합니다")
    String password,
    
    String picture,
    
    @Size(max = 100, message = "정보는 100자를 초과할 수 없습니다")
    String info,
    
    @NotBlank(message = "생년월일은 필수입니다")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "생년월일은 yyyy-MM-dd 형식이어야 합니다")
    String birthDate,
    
    @NotBlank(message = "회원가입 질문은 필수입니다")
    @Size(max = 255, message = "회원가입 질문은 255자를 초과할 수 없습니다")
    String signupQuestion,
    
    @NotBlank(message = "회원가입 답변은 필수입니다")
    @Size(max = 30, message = "답변은 최대 30자까지 입력할 수 있습니다")
    @Pattern(regexp = "^(?!\\s).{1,30}$", message = "답변의 첫 글자는 공백이 될 수 없습니다")
    String signupAnswer
) {
    
    /**
     * 표시명이 없으면 firstName + lastName으로 생성
     */
    public String getDisplayName() {
        return name != null && !name.trim().isEmpty() 
            ? name 
            : firstName + " " + lastName;
    }
}
