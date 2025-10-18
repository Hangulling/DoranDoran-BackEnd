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
   */
  @PostMapping
  @Operation(summary = "표현 보관", description = "사용자가 표현과 AI 응답을 보관함에 저장")
  public ResponseEntity<BookmarkResponse> saveBookmark(
      @Parameter(description = "사용자 ID", required = true)
      @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,

      @Parameter(description = "보관 요청 데이터", required = true)
      @Valid @RequestBody BookmarkRequest request) {

    UUID userId = parseUserIdHeader(userIdHeader);
    if (userId == null) {
      log.warn("X-User-Id header is missing or invalid");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    log.info("POST /api/store/bookmarks - userId: {}", userId);

    BookmarkResponse response = storageService.saveBookmark(userId, request);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * 보관함 전체 조회
   */
  @GetMapping
  @Operation(summary = "보관함 전체 조회", description = "사용자의 보관함 전체 목록 조회")
  public ResponseEntity<List<StorageListResponse>> getBookmarks(
      @Parameter(description = "사용자 ID", required = true)
      @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {

    UUID userId = parseUserIdHeader(userIdHeader);
    if (userId == null) {
      log.warn("X-User-Id header is missing or invalid");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    log.info("GET /api/store/bookmarks - userId: {}", userId);

    List<StorageListResponse> response = storageService.getBookmarks(userId);

    return ResponseEntity.ok(response);
  }


  /**
   * 보관함 전체 조회 (페이징)
   */
  @GetMapping("/page")
  @Operation(summary = "보관함 조회 (페이징)", description = "페이징된 보관함 목록 조회")
  public ResponseEntity<Page<StorageListResponse>> getBookmarksPage(
      @Parameter(description = "사용자 ID", required = true)
      @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,

      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
      Pageable pageable) {

    UUID userId = parseUserIdHeader(userIdHeader);
    if (userId == null) {
      log.warn("X-User-Id header is missing or invalid");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    log.info("GET /api/store/bookmarks/page - userId: {}", userId);

    Page<StorageListResponse> response = storageService.getBookmarks(userId, pageable);

    return ResponseEntity.ok(response);
  }

//  /**
//   * 방별 보관함 조회
//   */
//  @GetMapping("/chatroom/{chatroomId}")
//  @Operation(summary = "방별 보관함 조회", description = "특정 채팅방의 보관함만 조회")
//  public ResponseEntity<List<StorageListResponse>> getBookmarksByChatroom(
//      @Parameter(description = "사용자 ID", required = true)
//      @RequestHeader("X-User-Id") UUID userId,
//
//      @Parameter(description = "채팅방 ID", required = true)
//      @PathVariable UUID chatroomId) {
//
//    log.info("GET /api/store/bookmarks/chatroom/{} - userId: {}", chatroomId, userId);
//
//    List<StorageListResponse> response = storageService.getBookmarksByChatroom(userId, chatroomId);
//
//    return ResponseEntity.ok(response);
//  }

  /**
   * 챗봇 타입별 보관함 조회
   */
  @GetMapping("/bot-type/{botType}")
  @Operation(summary = "챗봇 타입별 보관함 조회", description = "특정 챗봇 타입의 모든 보관함 조회")
  public ResponseEntity<List<StorageListResponse>> getBookmarksByBotType(
      @Parameter(description = "사용자 ID", required = true)
      @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,

      @Parameter(description = "챗봇 타입 (friend, honey, coworker, senior)", required = true)
      @PathVariable String botType) {

    UUID userId = parseUserIdHeader(userIdHeader);
    if (userId == null) {
      log.warn("X-User-Id header is missing or invalid");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    log.info("GET /api/store/bookmarks/bot-type/{} - userId: {}", botType, userId);

    // botType 유효성 검증
    if (!botType.matches("^(friend|honey|coworker|senior)$")) {
      log.warn("유효하지 않은 botType: {}", botType);
      return ResponseEntity.badRequest().build();
    }

    List<StorageListResponse> response = storageService.getBookmarksByBotType(userId, botType);

    return ResponseEntity.ok(response);
  }

  /**
   * 보관함 삭제
   */
  @DeleteMapping("/{bookmarkId}")
  @Operation(summary = "보관함 삭제", description = "보관함 항목 삭제 (소프트 삭제)")
  public ResponseEntity<Void> deleteBookmark(
      @Parameter(description = "사용자 ID", required = true)
      @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,

      @Parameter(description = "보관함 ID", required = true)
      @PathVariable UUID bookmarkId) {

    UUID userId = parseUserIdHeader(userIdHeader);
    if (userId == null) {
      log.warn("X-User-Id header is missing or invalid");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    log.info("DELETE /api/store/bookmarks/{} - userId: {}", bookmarkId, userId);

    storageService.deleteBookmark(userId, bookmarkId);

    return ResponseEntity.noContent().build();
  }


  /**
   * 보관함 일괄 삭제
   */
  @DeleteMapping
  @Operation(summary = "보관함 일괄 삭제", description = "여러 보관함 항목 한 번에 삭제")
  public ResponseEntity<Void> deleteBookmarks(
      @Parameter(description = "사용자 ID", required = true)
      @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,

      @Parameter(description = "삭제할 보관함 ID 리스트", required = true)
      @RequestBody List<UUID> bookmarkIds) {

    UUID userId = parseUserIdHeader(userIdHeader);
    if (userId == null) {
      log.warn("X-User-Id header is missing or invalid");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    log.info("DELETE /api/store/bookmarks - userId: {}, count: {}", userId, bookmarkIds.size());

    storageService.deleteBookmarks(userId, bookmarkIds);

    return ResponseEntity.noContent().build();
  }

  /**
   * 보관함 개수 조회
   */
  @GetMapping("/count")
  @Operation(summary = "보관함 개수", description = "사용자의 보관함 항목 개수 조회")
  public ResponseEntity<Long> countBookmarks(
      @Parameter(description = "사용자 ID", required = true)
      @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {

    UUID userId = parseUserIdHeader(userIdHeader);
    if (userId == null) {
      log.warn("X-User-Id header is missing or invalid");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    log.info("GET /api/store/bookmarks/count - userId: {}", userId);

    long count = storageService.countBookmarks(userId);

    return ResponseEntity.ok(count);
  }

//  /**
//   * 챗봇별 보관함 조회
//   */
//  @GetMapping("/chatbot/{chatbotId}")
//  @Operation(summary = "챗봇별 보관함 조회", description = "특정 챗봇의 모든 보관함 조회 (여러 채팅방 포함)")
//  public ResponseEntity<List<StorageListResponse>> getBookmarksByChatbot(
//      @Parameter(description = "사용자 ID", required = true)
//      @RequestHeader("X-User-Id") UUID userId,
//
//      @Parameter(description = "챗봇 ID", required = true)
//      @PathVariable UUID chatbotId) {
//
//    log.info("GET /api/store/bookmarks/chatbot/{} - userId: {}", chatbotId, userId);
//
//    List<StorageListResponse> response = storageService.getBookmarksByChatbot(userId, chatbotId);
//
//    return ResponseEntity.ok(response);
//  }

  /**
   * Cursor 기반 페이징 조회
   */
  @GetMapping("/cursor")
  @Operation(summary = "보관함 조회 (Cursor 페이징)", description = "Cursor 기반 무한 스크롤용 페이징 조회")
  public ResponseEntity<Page<StorageListResponse>> getBookmarksWithCursor(
      @Parameter(description = "사용자 ID", required = true)
      @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,

      @Parameter(description = "마지막 조회 ID (null이면 처음부터)")
      @RequestParam(required = false) UUID lastId,

      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
      Pageable pageable) {

    UUID userId = parseUserIdHeader(userIdHeader);
    if (userId == null) {
      log.warn("X-User-Id header is missing or invalid");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    log.info("GET /api/store/bookmarks/cursor - userId: {}, lastId: {}", userId, lastId);

    Page<StorageListResponse> response = storageService.getBookmarksWithCursor(userId, lastId, pageable);

    return ResponseEntity.ok(response);
  }

  /**
   * X-User-Id 헤더 파싱
   * @param userIdHeader X-User-Id 헤더 값
   * @return UUID 또는 null
   */
  private UUID parseUserIdHeader(String userIdHeader) {
    if (userIdHeader == null || userIdHeader.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(userIdHeader);
    } catch (IllegalArgumentException e) {
      log.warn("Invalid X-User-Id header format: {}", userIdHeader);
      return null;
    }
  }
}