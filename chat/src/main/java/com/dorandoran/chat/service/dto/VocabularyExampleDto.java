package com.dorandoran.chat.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vocabulary Agent Excel 예시 데이터 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyExampleDto {
    private String relation; // Friend, Honey, Senior, Coworker (concept)
    private String content; // 문장 내용
    private String word; // 추출된 단어
    private Integer difficulty; // 난이도 (1, 2, 3)
    private String roma; // 로마자 표기
    private String ko; // 한국어 설명
    private String en; // 영어 설명
}


