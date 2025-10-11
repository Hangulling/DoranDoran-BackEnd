package com.dorandoran.store.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Multi-Agent AI 응답 DTO
 * PostgreSQL JSONB 컬럼에 저장
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // null 필드 제외
public class AiResponse {

  // 친밀도 레벨
  private String intimacyLevel;  // "Close", "Casual", "Friendly"

  // AI 설명/피드백
  private String description;  // "Close friends don't use formal language..."

  // 번역 정보
  private Translation translation;

  // 어려운 단어 설명들
  private List<VocabularyItem> vocabulary;

  // 교정 내용들
  private List<String> corrections;

  /**
   * 번역 정보
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Translation {
    private String english;       // 영어 번역
    private String pronunciation; // 발음
  }

  /**
   * 어려운 단어 설명
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class VocabularyItem {
    private String word;          // 단어
    private String pronunciation; // 발음
    private String explanation;   // 설명
  }
}