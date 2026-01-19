package com.dorandoran.store.controller;

import com.dorandoran.store.dto.response.BookmarkCountDetailResponse;
import com.dorandoran.store.service.BookmarkStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 북마크 통계 컨트롤러
 * GA 태깅 및 메트릭 조회 API 제공
 *
 * 주요 기능:
 * - 사용자별 북마크 카운트 통계
 * - 교정 메시지/AI description 사용률 측정
 * - 학습 패턴 분석용 데이터 제공
 */
@RestController
@RequestMapping("/api/store/statistics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bookmark Statistics", description = "북마크 통계 및 메트릭 API")
public class BookmarkStatisticsController {

  private final BookmarkStatisticsService bookmarkStatisticsService;

  /**
   * 북마크 상세 카운트 조회 (GA 태깅용)
   * 전체/교정/AI description 개수를 한 번에 조회
   *
   * GA 전송 데이터:
   * - total_bookmarks: 전체 북마크 수
   * - corrected_bookmarks: 교정 표현 저장 수
   * - ai_description_bookmarks: AI 설명 저장 수
   */
  @GetMapping("/bookmarks/count/detail")
  @Operation(
      summary = "북마크 상세 카운트 조회",
      description = "GA 태깅을 위한 사용자별 북마크 통계 정보 (전체/교정/AI description) 한 번에 조회"
  )
  public ResponseEntity<BookmarkCountDetailResponse> getBookmarkCountDetail(
      @Parameter(description = "사용자 ID", required = true)
      @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {

    // 사용자 ID 헤더 파싱 및 검증
    UUID userId = parseUserIdHeader(userIdHeader);
    if (userId == null) {
      log.warn("[getBookmarkCountDetail] X-User-Id header is missing or invalid");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    log.info("GET /api/store/statistics/bookmarks/count/detail - userId: {}", userId);

    // 통계 서비스 호출
    // 전체/교정/AI description 카운트를 한 번에 조회
    BookmarkCountDetailResponse response = bookmarkStatisticsService
        .getBookmarkCountDetail(userId);

    // 성공 응답 반환 (200 OK)
    return ResponseEntity.ok(response);
  }

  /**
   * X-User-Id 헤더 파싱 유틸리티 메서드
   * StorageController와 동일한 검증 로직 사용
   *
   * @param userIdHeader X-User-Id 헤더 값
   * @return 파싱된 UUID, 실패 시 null
   */
  private UUID parseUserIdHeader(String userIdHeader) {
    // null 또는 빈 문자열 체크
    if (userIdHeader == null || userIdHeader.isBlank()) {
      return null;
    }

    // UUID 포맷 검증
    try {
      return UUID.fromString(userIdHeader);
    } catch (IllegalArgumentException e) {
      log.warn("[parseUserIdHeader] Invalid X-User-Id header format: {}", userIdHeader);
      return null;
    }
  }
}