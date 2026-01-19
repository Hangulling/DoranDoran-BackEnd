package com.dorandoran.store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 북마크 상세 카운트 응답 DTO
 * GA 태깅을 위한 사용자별 북마크 통계 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkCountDetailResponse {

  /**
   * 전체 북마크 개수
   * isDeleted=false인 모든 북마크
   */
  private long totalBookmarks;

  /**
   * 교정 메시지 저장 개수
   * correctedContent가 null이 아닌 북마크
   * GA 이벤트: "교정_표현_저장" 누적 횟수
   */
  private long correctedBookmarks;

  /**
   * AI description 저장 개수
   * aiResponse.description이 존재하는 북마크
   * GA 이벤트: "AI_설명_저장" 누적 횟수
   */
  private long aiDescriptionBookmarks;

  /**
   * 정적 팩토리 메서드
   * Repository에서 조회한 카운트 값들로 DTO 생성
   *
   * @param totalBookmarks 전체 북마크 개수
   * @param correctedBookmarks 교정 메시지 개수
   * @param aiDescriptionBookmarks AI description 개수
   * @return 카운트 정보를 담은 응답 DTO
   */
  public static BookmarkCountDetailResponse of(
      long totalBookmarks,
      long correctedBookmarks,
      long aiDescriptionBookmarks) {

    return BookmarkCountDetailResponse.builder()
        .totalBookmarks(totalBookmarks)
        .correctedBookmarks(correctedBookmarks)
        .aiDescriptionBookmarks(aiDescriptionBookmarks)
        .build();
  }
}