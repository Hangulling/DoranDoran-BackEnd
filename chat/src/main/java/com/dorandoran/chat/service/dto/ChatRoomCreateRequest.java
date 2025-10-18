package com.dorandoran.chat.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ChatRoomCreateRequest {
    @NotNull(message = "사용자 ID는 필수입니다")
    private UUID userId;
    
    @NotNull(message = "챗봇 ID는 필수입니다")
    private UUID chatbotId;
    
    @NotBlank(message = "채팅방 이름은 필수입니다")
    @Size(min = 1, max = 100, message = "채팅방 이름은 1-100자 사이여야 합니다")
    private String name = "대화";
    
    private String concept = "FRIEND"; // FRIEND, HONEY, COWORKER, SENIOR, BOSS (기본값: FRIEND)
    
    @Min(value = 1, message = "친밀도 레벨은 1 이상이어야 합니다")
    @Max(value = 3, message = "친밀도 레벨은 3 이하여야 합니다")
    private Integer intimacyLevel = 2; // 1, 2, 3 (기본값: 2)
}


