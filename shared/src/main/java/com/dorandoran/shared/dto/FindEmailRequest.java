package com.dorandoran.shared.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이메일 찾기 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindEmailRequest {
    
    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 1, max = 50, message = "이름은 1-50자 사이여야 합니다")
    private String firstName;
    
    @NotBlank(message = "성은 필수입니다")
    @Size(min = 1, max = 50, message = "성은 1-50자 사이여야 합니다")
    private String lastName;
    
    @NotBlank(message = "생년월일은 필수입니다")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "생년월일은 yyyy-MM-dd 형식이어야 합니다")
    private String birthDate;
    
    @NotBlank(message = "회원가입 질문은 필수입니다")
    @Size(max = 255, message = "회원가입 질문은 255자를 초과할 수 없습니다")
    private String signupQuestion;
    
    @NotBlank(message = "회원가입 답변은 필수입니다")
    @Size(max = 30, message = "답변은 최대 30자까지 입력할 수 있습니다")
    @Pattern(regexp = "^(?!\\s).{1,30}$", message = "답변의 첫 글자는 공백이 될 수 없습니다")
    private String signupAnswer;
}

