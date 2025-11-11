package com.dorandoran.chat.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Greeting Service Excel 예시 데이터 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GreetingExampleDto {
    private String concept; // friend, honey, senior, coworker, boss
    private String topic; // 주제
    private Integer intimacyLevel; // intimacyLevel (1, 2, 3)
    private String botMessage; // 봇 메시지
    private String guideMessage; // 가이드 메시지
}


