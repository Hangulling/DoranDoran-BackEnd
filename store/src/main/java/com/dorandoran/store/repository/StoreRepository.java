package com.dorandoran.store.repository;

import com.dorandoran.store.entity.Store;
import feign.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Store Repository
 */
@Repository
public interface StoreRepository extends JpaRepository<Store, UUID> {

  // 사용자별 전체 조회
  List<Store> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId);

  // 사용자별 전체 조회 (페이징)
  Page<Store> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

//  // 방별 조회
//  List<Store> findByUserIdAndChatroomIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId, UUID chatroomId);
//
//  // 방별 조회 (페이징)
//  Page<Store> findByUserIdAndChatroomIdAndIsDeletedFalseOrderByCreatedAtDesc(
//      UUID userId, UUID chatroomId, Pageable pageable
//  );

  // 중복 저장 확인
  boolean existsByUserIdAndMessageIdAndIsDeletedFalse(UUID userId, UUID messageId);

  // 특정 보관함 조회
  Optional<Store> findByUserIdAndMessageIdAndIsDeletedFalse(UUID userId, UUID messageId);

  // 보관함 개수
  long countByUserIdAndIsDeletedFalse(UUID userId);

  /**
   * 챗봇별 보관함 조회
   */
//  List<Store> findByUserIdAndChatbotIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId, UUID chatbotId);

  /**
   * 챗봇 타입 별 보관함 조회
   * @param userId
   * @param botType
   * @return
   */
  List<Store> findByUserIdAndBotTypeAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId, String botType);


  /**
   * Cursor 기반 페이징 조회
   * @param userId 사용자 ID
   * @param lastId 마지막 조회 ID (null이면 처음부터)
   * @param pageable 페이지 정보
   */
  @Query("SELECT s FROM Store s WHERE s.userId = :userId AND s.isDeleted = false " +
      "AND (:lastId IS NULL OR s.id < :lastId) " +
      "ORDER BY s.createdAt DESC")
  Page<Store> findByUserIdWithCursor(@Param("userId") UUID userId,
      @Param("lastId") UUID lastId,
      Pageable pageable);
}