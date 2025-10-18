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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiResponse {

  // 친밀도 레벨 (Close, Casual, Friendly)
  private String intimacyLevel;

  // AI 설명/피드백
  private String description;

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
    private String english;
    private String pronunciation;
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
    private String word;
    private String pronunciation;
    private String explanation;
  }
}