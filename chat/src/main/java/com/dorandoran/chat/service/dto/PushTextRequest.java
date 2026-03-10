package com.dorandoran.chat.service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PushTextRequest {

    @NotNull(message = "사용자 ID는 필수입니다")
    private UUID userId;

    @NotNull(message = "챗봇 ID는 필수입니다")
    private UUID chatbotId;

    // 대화 주제 (예: "일상", "연애")
    private String topic;

    // 컨셉 (friend, honey, senior, coworker 등) - 없으면 friend 로 처리
    private String concept;

    // 친밀도 레벨 (1~3), null 이면 1
    private Integer intimacyLevel;
}

