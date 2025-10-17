package com.dorandoran.chat.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageSendRequest {
    @Pattern(regexp = "^(user|bot|system)$", message = "발신자 타입은 user, bot, system 중 하나여야 합니다")
    private String senderType = "user"; // user | bot | system (API에서는 user만 허용)
    
    @NotBlank(message = "메시지 내용은 필수입니다")
    @Size(min = 1, max = 10000, message = "메시지 내용은 1-10000자 사이여야 합니다")
    private String content;
    
    @Pattern(regexp = "^(text|code|system)$", message = "콘텐츠 타입은 text, code, system 중 하나여야 합니다")
    private String contentType = "text";
}


