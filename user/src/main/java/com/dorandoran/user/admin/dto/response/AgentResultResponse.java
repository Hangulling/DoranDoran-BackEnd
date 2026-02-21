package com.dorandoran.user.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Agent 결과 응답 DTO
 * intimacy, conversation, vocabulary 타입별로 구조화
 */
@Getter
@Builder
public class AgentResultResponse {

  /**
   * 친밀도 교정 결과 (intimacy agent)
   */
  private IntimacyResult intimacy;

  /**
   * 대화 생성 결과 (conversation agent)
   */
  private ConversationResult conversation;

  /**
   * 어휘 추출 결과 (vocabulary agent)
   */
  private VocabularyResult vocabulary;

  /**
   * 친밀도 교정 결과
   */
  @Getter
  @Builder
  public static class IntimacyResult {
    private Integer detectedLevel;
    private String correctedSentence;
    private String corrections;
    private Feedback feedback;

    @Getter
    @Builder
    public static class Feedback {
      private String ko;
      private String en;
    }
  }

  /**
   * 대화 생성 결과
   */
  @Getter
  @Builder
  public static class ConversationResult {
    private String content;
  }

  /**
   * 어휘 추출 결과
   */
  @Getter
  @Builder
  public static class VocabularyResult {
    private List<Word> words;

    @Getter
    @Builder
    public static class Word {
      private String word;
      private Integer difficulty;
      private String context;
    }
  }
}