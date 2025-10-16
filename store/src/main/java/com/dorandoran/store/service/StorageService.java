package com.dorandoran.store.service;

import com.dorandoran.store.client.ChatServiceClient;
import com.dorandoran.store.dto.request.BookmarkRequest;
import com.dorandoran.store.dto.response.BookmarkResponse;
import com.dorandoran.store.dto.response.StorageListResponse;
import com.dorandoran.store.entity.Store;
import com.dorandoran.store.exception.BookmarkNotFoundException;
import com.dorandoran.store.exception.DuplicateBookmarkException;
import com.dorandoran.store.exception.UnauthorizedAccessException;
import com.dorandoran.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

//  /**
//   * 표현 보관하기
//   */
//  @Transactional
//  public BookmarkResponse saveBookmark(UUID userId, BookmarkRequest request) {
//    log.info("표현 보관 시작: userId={}, messageId={}", userId, request.getMessageId());
//
//    // 중복 체크
//    if (storeRepository.existsByUserIdAndMessageIdAndIsDeletedFalse(userId, request.getMessageId())) {
//      log.warn("중복 저장 시도: userId={}, messageId={}", userId, request.getMessageId());
//      throw new DuplicateBookmarkException("이미 보관함에 저장된 표현입니다");
//    }
//
//    // Store 엔티티 생성
//    Store store = Store.builder()
//        .userId(userId)
//        .messageId(request.getMessageId())
//        .chatroomId(request.getChatroomId())
////        .chatbotId(request.getChatbotId())
//        .content(request.getContent())
//        .aiResponse(request.getAiResponse())
//        .botType(request.getBotType())
//        .isDeleted(false)
//        .build();
//
//    Store saved = storeRepository.save(store);
//    log.info("표현 보관 완료: storeId={}", saved.getId());
//
//    return BookmarkResponse.from(saved, "표현이 보관함에 저장되었습니다");
//  }

  /**
   * 표현 보관하기 - redis 적용
   */
  @Transactional
  public BookmarkResponse saveBookmark(UUID userId, BookmarkRequest request) {
    log.info("표현 보관 시작: userId={}, messageId={}", userId, request.getMessageId());

    // 중복 체크
    if (storeRepository.existsByUserIdAndMessageIdAndIsDeletedFalse(userId, request.getMessageId())) {
      log.warn("중복 저장 시도: userId={}, messageId={}", userId, request.getMessageId());
      throw new DuplicateBookmarkException("이미 보관함에 저장된 표현입니다");
    }

    // 1. Chat Service API 호출: Redis → DB 이동
    try {
      chatServiceClient.archiveMessage(request.getChatroomId(), request.getMessageId(), userId);
      log.info("Chat Service 메시지 보관 완료: messageId={}", request.getMessageId());
    } catch (Exception e) {
      log.error("Chat Service 메시지 보관 실패: messageId={}", request.getMessageId(), e);
      throw new RuntimeException("메시지 보관에 실패했습니다", e);
    }

    // 2. Store 엔티티 생성
    Store store = Store.builder()
        .userId(userId)
        .messageId(request.getMessageId())
        .chatroomId(request.getChatroomId())
        .content(request.getContent())
        .aiResponse(request.getAiResponse())
        .botType(request.getBotType())
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
        .map(store -> {
          StorageListResponse response = StorageListResponse.from(store);
          try {
            String chatroomName = chatServiceClient.getChatRoom(store.getChatroomId()).getName();
            response.setChatroomNameFromClient(chatroomName);
          } catch (Exception e) {
            log.warn("채팅방 이름 조회 실패: chatroomId={}", store.getChatroomId(), e);
            response.setChatroomNameFromClient("Unknown");
          }
          return response;
        })
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

    return stores.map(store -> {
      StorageListResponse response = StorageListResponse.from(store);
      try {
        String chatroomName = chatServiceClient.getChatRoom(store.getChatroomId()).getName();
        response.setChatroomNameFromClient(chatroomName);
      } catch (Exception e) {
        log.warn("채팅방 이름 조회 실패: chatroomId={}", store.getChatroomId(), e);
        response.setChatroomNameFromClient("Unknown");
      }
      return response;
    });
  }

//  /**
//   * 방별 보관함 조회
//   */
//  @Transactional(readOnly = true)
//  public List<StorageListResponse> getBookmarksByChatroom(UUID userId, UUID chatroomId) {
//    log.info("방별 보관함 조회: userId={}, chatroomId={}", userId, chatroomId);
//
//    List<Store> stores = storeRepository
//        .findByUserIdAndChatroomIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, chatroomId);
//
//    if (stores.isEmpty()) {
//      log.info("해당 채팅방의 보관함이 비어있음: chatroomId={}", chatroomId);
//    }
//
//    // 방별 조회는 같은 채팅방이므로 한 번만 조회
//    String chatroomName = "Unknown";
//    try {
//      chatroomName = chatServiceClient.getChatRoom(chatroomId).getName();
//    } catch (Exception e) {
//      log.warn("채팅방 이름 조회 실패: chatroomId={}", chatroomId, e);
//    }
//
//    final String finalChatroomName = chatroomName;
//    return stores.stream()
//        .map(store -> {
//          StorageListResponse response = StorageListResponse.from(store);
//          response.setChatroomNameFromClient(finalChatroomName);
//          return response;
//        })
//        .collect(Collectors.toList());
//  }
//
//  /**
//   * 방별 보관함 조회 (페이징)
//   */
//  @Transactional(readOnly = true)
//  public Page<StorageListResponse> getBookmarksByChatroom(UUID userId, UUID chatroomId, Pageable pageable) {
//    log.info("방별 보관함 조회 (페이징): userId={}, chatroomId={}", userId, chatroomId);
//
//    Page<Store> stores = storeRepository
//        .findByUserIdAndChatroomIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, chatroomId, pageable);
//
//    // 방별 조회는 같은 채팅방이므로 한 번만 조회
//    String chatroomName = "Unknown";
//    try {
//      chatroomName = chatServiceClient.getChatRoom(chatroomId).getName();
//    } catch (Exception e) {
//      log.warn("채팅방 이름 조회 실패: chatroomId={}", chatroomId, e);
//    }
//
//    final String finalChatroomName = chatroomName;
//    return stores.map(store -> {
//      StorageListResponse response = StorageListResponse.from(store);
//      response.setChatroomNameFromClient(finalChatroomName);
//      return response;
//    });
//  }

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

    // 1. Store 소프트 삭제
    store.setIsDeleted(true);
    store.setDeletedAt(LocalDateTime.now());
    storeRepository.save(store);

    // 2. Chat Service API 호출: Message 소프트 삭제
    try {
      chatServiceClient.deleteMessage(store.getMessageId(), userId);
      log.info("Chat Service 메시지 삭제 완료: messageId={}", store.getMessageId());
    } catch (Exception e) {
      log.error("Chat Service 메시지 삭제 실패: messageId={}", store.getMessageId(), e);
      // Store는 이미 삭제 처리되었으므로 로그만 남김
      // 동기화는 별도 배치 작업으로 처리
    }

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
        log.error("삭제 실패: bookmarkId={}", bookmarkId, e);
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

//  /**
//   * 챗봇별 보관함 조회
//   */
//  @Transactional(readOnly = true)
//  public List<StorageListResponse> getBookmarksByChatbot(UUID userId, UUID chatbotId) {
//    log.info("챗봇별 보관함 조회: userId={}, chatbotId={}", userId, chatbotId);
//
//    List<Store> stores = storeRepository
//        .findByUserIdAndChatbotIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, chatbotId);
//
//    if (stores.isEmpty()) {
//      log.info("해당 챗봇의 보관함이 비어있음: chatbotId={}", chatbotId);
//    }
//
//    return stores.stream()
//        .map(store -> {
//          StorageListResponse response = StorageListResponse.from(store);
//          try {
//            String chatroomName = chatServiceClient.getChatRoom(store.getChatroomId()).getName();
//            response.setChatroomNameFromClient(chatroomName);
//          } catch (Exception e) {
//            log.warn("채팅방 이름 조회 실패: chatroomId={}", store.getChatroomId(), e);
//            response.setChatroomNameFromClient("Unknown");
//          }
//          return response;
//        })
//        .collect(Collectors.toList());
//  }

  /**
   * botType별 조회
   * @param userId
   * @param botType
   * @return
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
        .map(store -> {
          StorageListResponse response = StorageListResponse.from(store);
          try {
            String chatroomName = chatServiceClient.getChatRoom(store.getChatroomId()).getName();
            response.setChatroomNameFromClient(chatroomName);
          } catch (Exception e) {
            log.warn("채팅방 이름 조회 실패: chatroomId={}", store.getChatroomId(), e);
            response.setChatroomNameFromClient("Unknown");
          }
          return response;
        })
        .collect(Collectors.toList());
  }

  /**
   * Cursor 기반 페이징 조회
   */
  @Transactional(readOnly = true)
  public Page<StorageListResponse> getBookmarksWithCursor(UUID userId, UUID lastId, Pageable pageable) {
    log.info("Cursor 기반 보관함 조회: userId={}, lastId={}, size={}",
        userId, lastId, pageable.getPageSize());

    Page<Store> stores = storeRepository.findByUserIdWithCursor(userId, lastId, pageable);

    return stores.map(store -> {
      StorageListResponse response = StorageListResponse.from(store);
      try {
        String chatroomName = chatServiceClient.getChatRoom(store.getChatroomId()).getName();
        response.setChatroomNameFromClient(chatroomName);
      } catch (Exception e) {
        log.warn("채팅방 이름 조회 실패: chatroomId={}", store.getChatroomId(), e);
        response.setChatroomNameFromClient("Unknown");
      }
      return response;
    });
  }
}