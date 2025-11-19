package com.dorandoran.store.service;

import com.dorandoran.store.client.ChatServiceClient;
import com.dorandoran.store.client.dto.ChatRoomDto;
import com.dorandoran.store.dto.request.BookmarkRequest;
import com.dorandoran.store.dto.response.BookmarkResponse;
import com.dorandoran.store.dto.response.StorageListResponse;
import com.dorandoran.store.entity.Store;
import com.dorandoran.store.exception.BookmarkNotFoundException;
import com.dorandoran.store.exception.DuplicateBookmarkException;
import com.dorandoran.store.exception.UnauthorizedAccessException;
import com.dorandoran.store.repository.StoreRepository;
import com.dorandoran.store.util.BotTypeMapper;
import feign.FeignException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Storage Service
 * 보관함 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

  private final StoreRepository storeRepository;
  private final ChatServiceClient chatServiceClient;  // 채팅방 정보 획득
  private final RedisTemplate<String, String> redisTemplate;  // Redis 캐싱용

  // Redis 캐시 키 접두사
  private static final String CHATROOM_NAME_PREFIX = "chatroom:name:";
  // 캐시 TTL: 10분
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  /**
   * 표현 보관하기
   */
  @Transactional
  public BookmarkResponse saveBookmark(UUID userId, BookmarkRequest request) {
    log.info("표현 보관 시작: userId={}, messageId={}", userId, request.getMessageId());

    // 중복 체크
    if (storeRepository.existsByUserIdAndMessageIdAndIsDeletedFalse(userId, request.getMessageId())) {
      log.warn("중복 저장 시도: userId={}, messageId={}", userId, request.getMessageId());
      throw new DuplicateBookmarkException("이미 보관함에 저장된 표현입니다");
    }

    // chatbotId로 botType 자동 매핑
    String botType;
    try {
      botType = BotTypeMapper.getBotType(request.getChatbotId());
      log.info("botType 자동 매핑 완료: chatbotId={}, botType={}",
          request.getChatbotId(), botType);
    } catch (IllegalArgumentException e) {
      log.error("유효하지 않은 chatbotId: {}", request.getChatbotId());
      throw e;
    }

    // Store 엔티티 생성
    Store store = Store.builder()
        .userId(userId)
        .messageId(request.getMessageId())
        .chatroomId(request.getChatroomId())
//        .chatbotId(request.getChatbotId())
        .content(request.getContent())
        .correctedContent(request.getCorrectedContent())
        .aiResponse(request.getAiResponse())
//        .botType(request.getBotType())
        .botType(botType)
        .isDeleted(false)
        .build();

    Store saved = storeRepository.save(store);
    log.info("표현 보관 완료: storeId={}", saved.getId());

    return BookmarkResponse.from(saved, "표현이 보관함에 저장되었습니다");
  }

  /**
   * 보관함 전체 조회
   */
  @Transactional(readOnly = true)
  public List<StorageListResponse> getBookmarks(UUID userId) {
    log.info("보관함 전체 조회: userId={}", userId);

    List<Store> stores = storeRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);

    if (stores.isEmpty()) {
      log.info("보관함이 비어있음: userId={}", userId);
    }

    return stores.stream()
        .map(store -> enrichWithChatroomName(store))
        .collect(Collectors.toList());
  }

  /**
   * 보관함 전체 조회 (페이징)
   */
  @Transactional(readOnly = true)
  public Page<StorageListResponse> getBookmarks(UUID userId, Pageable pageable) {
    log.info("보관함 조회 (페이징): userId={}, page={}, size={}",
        userId, pageable.getPageNumber(), pageable.getPageSize());

    Page<Store> stores = storeRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, pageable);

    return stores.map(store -> enrichWithChatroomName(store));
  }

  /**
   * 방별 보관함 조회
   */
  @Transactional(readOnly = true)
  public List<StorageListResponse> getBookmarksByChatroom(UUID userId, UUID chatroomId) {
    log.info("방별 보관함 조회: userId={}, chatroomId={}", userId, chatroomId);

    List<Store> stores = storeRepository
        .findByUserIdAndChatroomIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, chatroomId);

    if (stores.isEmpty()) {
      log.info("해당 채팅방의 보관함이 비어있음: chatroomId={}", chatroomId);
    }

    // 방별 조회는 같은 채팅방이므로 한 번만 조회 - 캐시 사용
    String chatroomName = getChatroomNameWithCache(chatroomId, userId);

    final String finalChatroomName = chatroomName;
    return stores.stream()
        .map(store -> {
          StorageListResponse response = StorageListResponse.from(store);
          response.setChatroomNameFromClient(finalChatroomName);
          return response;
        })
        .collect(Collectors.toList());
  }


  /**
   * 방별 보관함 조회 (페이징)
   */
  @Transactional(readOnly = true)
  public Page<StorageListResponse> getBookmarksByChatroom(UUID userId, UUID chatroomId, Pageable pageable) {
    log.info("방별 보관함 조회 (페이징): userId={}, chatroomId={}", userId, chatroomId);

    Page<Store> stores = storeRepository
        .findByUserIdAndChatroomIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, chatroomId, pageable);

    // 방별 조회는 같은 채팅방이므로 한 번만 조회
    String chatroomName = getChatroomNameWithCache(chatroomId, userId);

    final String finalChatroomName = chatroomName;
    return stores.map(store -> {
      StorageListResponse response = StorageListResponse.from(store);
      response.setChatroomNameFromClient(finalChatroomName);
      return response;
    });
  }

  /**
   * botType별 조회
   */
  @Transactional(readOnly = true)
  public List<StorageListResponse> getBookmarksByBotType(UUID userId, String botType) {
    log.info("챗봇 타입별 보관함 조회: userId={}, botType={}", userId, botType);

    List<Store> stores = storeRepository
        .findByUserIdAndBotTypeAndIsDeletedFalseOrderByCreatedAtDesc(userId, botType);

    if (stores.isEmpty()) {
      log.info("해당 챗봇 타입의 보관함이 비어있음: botType={}", botType);
    }

    return stores.stream()
        .map(store -> enrichWithChatroomName(store))
        .collect(Collectors.toList());
  }

  /**
   * 챗봇 타입별 보관함 조회 (Cursor 기반 - 무한스크롤용)
   */
  @Transactional(readOnly = true)
  public Page<StorageListResponse> getBookmarksByBotTypeWithCursor(
      UUID userId, String botType, UUID lastId, Pageable pageable) {
    log.info("챗봇 타입별 보관함 Cursor 조회: userId={}, botType={}, lastId={}, size={}",
        userId, botType, lastId, pageable.getPageSize());

    // 방금 추가한 Repository 메서드를 호출
    Page<Store> stores = storeRepository
        .findByUserIdAndBotTypeWithCursor(userId, botType, lastId, pageable);

    // 채팅방 이름 부가 정보 추가는 개별적으로 처리
    return stores.map(this::enrichWithChatroomName);
  }

  /**
   * Cursor 기반 페이징 조회
   */
  @Transactional(readOnly = true)
  public Page<StorageListResponse> getBookmarksWithCursor(UUID userId, UUID lastId, Pageable pageable) {
    log.info("Cursor 기반 보관함 조회: userId={}, lastId={}, size={}",
        userId, lastId, pageable.getPageSize());

    Page<Store> stores = storeRepository.findByUserIdWithCursor(userId, lastId, pageable);

    return stores.map(store -> enrichWithChatroomName(store));
  }

  /**
   * 보관함 삭제 (소프트 삭제)
   */
  @Transactional
  public void deleteBookmark(UUID userId, UUID bookmarkId) {
    log.info("보관함 삭제: userId={}, bookmarkId={}", userId, bookmarkId);

    Store store = storeRepository.findById(bookmarkId)
        .orElseThrow(() -> new BookmarkNotFoundException("보관함 항목을 찾을 수 없습니다"));

    // 권한 확인
    if (!store.getUserId().equals(userId)) {
      log.warn("삭제 권한 없음: userId={}, storeUserId={}", userId, store.getUserId());
      throw new UnauthorizedAccessException("삭제 권한이 없습니다");
    }

    // 이미 삭제됨
    if (store.getIsDeleted()) {
      log.warn("이미 삭제된 항목: bookmarkId={}", bookmarkId);
      throw new IllegalStateException("이미 삭제된 항목입니다");
    }

    // 소프트 삭제
    store.setIsDeleted(true);
    store.setDeletedAt(LocalDateTime.now());
    storeRepository.save(store);

    log.info("보관함 삭제 완료: bookmarkId={}", bookmarkId);
  }

  /**
   * 보관함 일괄 삭제 (소프트 삭제)
   */
  @Transactional
  public void deleteBookmarks(UUID userId, List<UUID> bookmarkIds) {
    log.info("보관함 일괄 삭제: userId={}, count={}", userId, bookmarkIds.size());

    for (UUID bookmarkId : bookmarkIds) {
      try {
        deleteBookmark(userId, bookmarkId);
      } catch (Exception e) {
        log.error("삭제 실패 - 계속 진행합니다: bookmarkId={}", bookmarkId, e);
        // 실패해도 계속 진행
      }
    }

    log.info("보관함 일괄 삭제 완료: userId={}", userId);
  }


  /**
   * 보관함 개수 조회
   */
  @Transactional(readOnly = true)
  public long countBookmarks(UUID userId) {
    return storeRepository.countByUserIdAndIsDeletedFalse(userId);
  }

  private StorageListResponse enrichWithChatroomName(Store store) {
    StorageListResponse response = StorageListResponse.from(store);
    String chatroomName = getChatroomNameWithCache(store.getChatroomId(), store.getUserId());
    response.setChatroomNameFromClient(chatroomName);
    return response;
  }

  // 새로운 메서드 추가
  private String getChatroomNameWithCache(UUID chatroomId, UUID userId) {
    String cacheKey = CHATROOM_NAME_PREFIX + chatroomId;

    try {
      String cachedName = redisTemplate.opsForValue().get(cacheKey);

      if (cachedName != null) {
        log.debug("✅ Cache HIT: chatroomId={}", chatroomId);
        return cachedName;
      }

      log.debug("❌ Cache MISS: chatroomId={}", chatroomId);

      // ✅ skipAuthCheck=true로 권한 체크 생략
      ChatRoomDto chatRoom = chatServiceClient.getChatRoom(chatroomId, userId, true);

      if (chatRoom != null && chatRoom.getName() != null) {
        String chatroomName = chatRoom.getName();
        redisTemplate.opsForValue().set(cacheKey, chatroomName, CACHE_TTL);
        log.info("✅ Cache SAVED: chatroomId={}", chatroomId);
        return chatroomName;
      } else {
        // null 응답인 경우 "Deleted Room"으로 캐시하여 반복 호출 방지
        redisTemplate.opsForValue().set(cacheKey, "Deleted Room", CACHE_TTL);
        log.warn("채팅방 정보가 null: chatroomId={}", chatroomId);
        return "Deleted Room";
      }
    } catch (FeignException.NotFound e) {
      // 404 오류인 경우 "Deleted Room"으로 캐시하여 반복 호출 방지
      redisTemplate.opsForValue().set(cacheKey, "Deleted Room", CACHE_TTL);
      log.warn("채팅방을 찾을 수 없음 (404): chatroomId={}", chatroomId);
      return "Deleted Room";
    } catch (FeignException e) {
      // FeignException의 status() 메서드로 HTTP 상태 코드 확인
      int status = e.status();
      if (status == 404) {
        redisTemplate.opsForValue().set(cacheKey, "Deleted Room", CACHE_TTL);
        log.warn("채팅방을 찾을 수 없음 (404): chatroomId={}, status={}", chatroomId, status);
        return "Deleted Room";
      } else if (status == 403) {
        log.warn("채팅방 접근 권한 없음 (403): chatroomId={}, userId={}", chatroomId, userId);
        return "Forbidden";
      } else if (status >= 500) {
        log.warn("Chat Service 오류 ({}): chatroomId={}", status, chatroomId);
        return "Unavailable";
      } else {
        log.warn("Feign 통신 오류: chatroomId={}, status={}, message={}", chatroomId, status, e.getMessage());
        return "Unknown";
      }
    } catch (Exception e) {
      log.error("채팅방 이름 조회 중 예상치 못한 오류: chatroomId={}", chatroomId, e);
      return "Unknown";
    }
  }

  /**
   * 방별 보관함 조회 (Cursor 기반 - 무한스크롤용)
   */
  @Transactional(readOnly = true)
  public Page<StorageListResponse> getBookmarksByChatroomWithCursor(
      UUID userId, UUID chatroomId, UUID lastId, Pageable pageable) {
    log.info("방별 보관함 Cursor 조회: userId={}, chatroomId={}, lastId={}, size={}",
        userId, chatroomId, lastId, pageable.getPageSize());

    Page<Store> stores = storeRepository
        .findByUserIdAndChatroomIdWithCursor(userId, chatroomId, lastId, pageable);

    // 방별 조회는 같은 채팅방이므로 한 번만 조회
    String chatroomName = getChatroomNameWithCache(chatroomId, userId);

    final String finalChatroomName = chatroomName;
    return stores.map(store -> {
      StorageListResponse response = StorageListResponse.from(store);
      response.setChatroomNameFromClient(finalChatroomName);
      return response;
    });
  }
}