package com.dorandoran.chat.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Intimacy Agent Excel 예시 데이터 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntimacyExampleDto {
    private String concept; // friend, honey, senior, coworker, boss
    private Integer intimacyLevel; // 1, 2, 3
    private String userMessage; // 사용자 입력 메시지
    private String correctedSentence; // 교정된 문장
    private String ko; // 한국어 피드백
    private String en; // 영어 피드백
    private String critical; // 문제 사항 (선택적)
}


