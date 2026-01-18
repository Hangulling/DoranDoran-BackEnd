package com.dorandoran.store.service;

import com.dorandoran.store.dto.response.BookmarkCountDetailResponse;
import com.dorandoran.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 북마크 통계 서비스
 * GA 태깅 및 메트릭 조회 전담
 * 주요 기능:
 * - 사용자별 북마크 카운트 통계
 * - 교정 메시지/AI description 사용률 측정
 * - GA 이벤트 추적용 데이터 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookmarkStatisticsService {

  private final StoreRepository storeRepository;

  /**
   * 북마크 상세 카운트 조회 (GA 태깅용)
   * 한 번의 API 호출로 모든 카운트 정보 제공
   *
   * 조회 항목:
   * - 전체 북마크 개수 (isDeleted=false)
   * - 교정 메시지(correctedContent) 포함 개수
   * - AI description 포함 개수
   *
   * @param userId 사용자 ID (X-User-Id 헤더에서 추출)
   * @return 통합 카운트 정보가 담긴 응답 DTO
   */
  @Transactional(readOnly = true)
  public BookmarkCountDetailResponse getBookmarkCountDetail(UUID userId) {
    log.info("[getBookmarkCountDetail] 사용자 북마크 상세 카운트 조회 시작 - userId: {}", userId);

    // 전체 북마크 개수 조회
    // isDeleted=false 조건으로 삭제되지 않은 북마크만 카운트
    long totalBookmarks = storeRepository.countByUserIdAndIsDeletedFalse(userId);
    log.debug("[getBookmarkCountDetail] 전체 북마크: {}", totalBookmarks);

    // 교정 메시지 포함 북마크 개수 조회
    // correctedContent가 null이 아닌 경우만 카운트
    long correctedBookmarks = storeRepository
        .countByUserIdAndCorrectedContentIsNotNullAndIsDeletedFalse(userId);
    log.debug("[getBookmarkCountDetail] 교정 메시지 북마크: {}", correctedBookmarks);

    // AI description 포함 북마크 개수 조회
    // aiResponse JSONB 내부의 description 필드가 존재하는 경우만 카운트
    long aiDescriptionBookmarks = storeRepository
        .countByUserIdWithAiDescription(userId);
    log.debug("[getBookmarkCountDetail] AI description 북마크: {}", aiDescriptionBookmarks);

    // 응답 DTO
    BookmarkCountDetailResponse response = BookmarkCountDetailResponse.of(
        totalBookmarks,
        correctedBookmarks,
        aiDescriptionBookmarks
    );

    log.info("[getBookmarkCountDetail] 조회 완료 - total: {}, corrected: {}, aiDescription: {}",
        totalBookmarks, correctedBookmarks, aiDescriptionBookmarks);

    return response;
  }
}