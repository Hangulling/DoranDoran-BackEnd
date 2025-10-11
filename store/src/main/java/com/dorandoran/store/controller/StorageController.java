package com.dorandoran.store.controller;

import com.dorandoran.store.dto.request.BookmarkRequest;
import com.dorandoran.store.dto.response.BookmarkResponse;
import com.dorandoran.store.dto.response.StorageListResponse;
import com.dorandoran.store.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Storage Controller
 * 보관함 API
 */
@RestController
@RequestMapping("/api/store/bookmarks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Storage", description = "보관함 API")
public class StorageController {

  private final StorageService storageService;

  /**
   * 표현 보관하기
   * POST /api/store/bookmarks
   */
  @PostMapping
  @Operation(summary = "표현 보관", description = "사용자가 표현과 AI 응답을 보관함에 저장")
  public ResponseEntity<BookmarkResponse> saveBookmark(
      @Parameter(description = "사용자 ID", required = true)
      @RequestHeader("X-User-Id") UUID userId,

      @Parameter(description = "보관 요청 데이터", required = true)
      @Valid @RequestBody BookmarkRequest request) {

    log.info("POST /api/store/bookmarks - userId: {}", userId);

    BookmarkResponse response = storageService.saveBookmark(userId, request);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * 보관함 전체 조회
   * GET /api/store/bookmarks
   */
  @GetMapping
  @Operation(summary = "보관함 전체 조회", description = "사용자의 보관함 전체 목록 조회")
  public ResponseEntity<List<StorageListResponse>> getBookmarks(
      @Parameter(description = "사용자 ID", required = true)
      @RequestHeader("X-User-Id") UUID userId) {

    log.info("GET /api/store/bookmarks - userId: {}", userId);

    List<StorageListResponse> response = storageService.getBookmarks(userId);

    return ResponseEntity.ok(response);
  }

  /**
   * 보관함 전체 조회 (페이징)
   * GET /api/store/bookmarks/page
   */
  @GetMapping("/page")
  @Operation(summary = "보관함 조회 (페이징)", description = "페이징된 보관함 목록 조회")
  public ResponseEntity<Page<StorageListResponse>> getBookmarksPage(
      @Parameter(description = "사용자 ID", required = true)
      @RequestHeader("X-User-Id") UUID userId,

      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
      Pageable pageable) {

    log.info("GET /api/store/bookmarks/page - userId: {}", userId);

    Page<StorageListResponse> response = storageService.getBookmarks(userId, pageable);

    return ResponseEntity.ok(response);
  }

  /**
   * 보관함 삭제
   * DELETE /api/store/bookmarks/{bookmarkId}
   */
  @DeleteMapping("/{bookmarkId}")
  @Operation(summary = "보관함 삭제", description = "보관함 항목 삭제 (소프트 삭제)")
  public ResponseEntity<Void> deleteBookmark(
      @Parameter(description = "사용자 ID", required = true)
      @RequestHeader("X-User-Id") UUID userId,

      @Parameter(description = "보관함 ID", required = true)
      @PathVariable UUID bookmarkId) {

    log.info("DELETE /api/store/bookmarks/{} - userId: {}", bookmarkId, userId);

    storageService.deleteBookmark(userId, bookmarkId);

    return ResponseEntity.noContent().build();
  }

  /**
   * 보관함 일괄 삭제
   * DELETE /api/store/bookmarks
   */
  @DeleteMapping
  @Operation(summary = "보관함 일괄 삭제", description = "여러 보관함 항목 한 번에 삭제")
  public ResponseEntity<Void> deleteBookmarks(
      @Parameter(description = "사용자 ID", required = true)
      @RequestHeader("X-User-Id") UUID userId,

      @Parameter(description = "삭제할 보관함 ID 리스트", required = true)
      @RequestBody List<UUID> bookmarkIds) {

    log.info("DELETE /api/store/bookmarks - userId: {}, count: {}", userId, bookmarkIds.size());

    storageService.deleteBookmarks(userId, bookmarkIds);

    return ResponseEntity.noContent().build();
  }

  /**
   * 보관함 개수 조회
   * GET /api/store/bookmarks/count
   */
  @GetMapping("/count")
  @Operation(summary = "보관함 개수", description = "사용자의 보관함 항목 개수 조회")
  public ResponseEntity<Long> countBookmarks(
      @Parameter(description = "사용자 ID", required = true)
      @RequestHeader("X-User-Id") UUID userId) {

    log.info("GET /api/store/bookmarks/count - userId: {}", userId);

    long count = storageService.countBookmarks(userId);

    return ResponseEntity.ok(count);
  }
}