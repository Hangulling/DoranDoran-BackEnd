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

  // 방별 조회
  List<Store> findByUserIdAndChatroomIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId, UUID chatroomId);

  // 방별 조회 (페이징)
  Page<Store> findByUserIdAndChatroomIdAndIsDeletedFalseOrderByCreatedAtDesc(
      UUID userId, UUID chatroomId, Pageable pageable
  );

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

  /**
   * 방별 Cursor 기반 페이징 조회 (무한스크롤용)
   * @param userId 사용자 ID
   * @param chatroomId 채팅방 ID
   * @param lastId 마지막 조회 ID (null이면 처음부터)
   * @param pageable 페이지 정보
   */
  @Query("SELECT s FROM Store s WHERE s.userId = :userId " +
      "AND s.chatroomId = :chatroomId " +
      "AND s.isDeleted = false " +
      "AND (:lastId IS NULL OR s.createdAt < " +
      "(SELECT s2.createdAt FROM Store s2 WHERE s2.id = :lastId)) " +
      "ORDER BY s.createdAt DESC")
  Page<Store> findByUserIdAndChatroomIdWithCursor(
      @Param("userId") UUID userId,
      @Param("chatroomId") UUID chatroomId,
      @Param("lastId") UUID lastId,
      Pageable pageable);

  /**
   * 챗봇 타입별 Cursor 기반 페이징 조회 (무한스크롤용)
   * @param userId 사용자 ID
   * @param botType 챗봇 타입
   * @param lastId 마지막 조회 ID (null이면 처음부터)
   * @param pageable 페이지 정보
   */
  @Query("SELECT s FROM Store s WHERE s.userId = :userId " +
      "AND s.botType = :botType " +
      "AND s.isDeleted = false " +
      "AND (:lastId IS NULL OR s.createdAt < " +
      "(SELECT s2.createdAt FROM Store s2 WHERE s2.id = :lastId)) " +
      "ORDER BY s.createdAt DESC, s.id DESC")
  Page<Store> findByUserIdAndBotTypeWithCursor(
      @Param("userId") UUID userId,
      @Param("botType") String botType,
      @Param("lastId") UUID lastId,
      Pageable pageable
  );

  /**
   * 사용자별 교정 메시지 개수 조회
   * correctedContent가 null이 아닌 북마크만 카운트
   *
   * @param userId 사용자 ID
   * @return 교정 메시지를 포함한 북마크 개수
   */
  long countByUserIdAndCorrectedContentIsNotNullAndIsDeletedFalse(UUID userId);

  /**
   * 사용자별 AI description 포함 개수 조회
   * aiResponse JSONB 내부의 description 필드가 존재하는 경우만 카운트
   * PostgreSQL JSONB 연산자 사용: ->> (텍스트로 추출), IS NOT NULL
   *
   * @param userId 사용자 ID
   * @return AI description을 포함한 북마크 개수
   */
  @Query(value = "SELECT COUNT(*) FROM store " +
      "WHERE user_id = :userId " +
      "AND is_deleted = false " +
      "AND ai_response->>'description' IS NOT NULL " +
      "AND ai_response->>'description' != ''",
      nativeQuery = true)
  long countByUserIdWithAiDescription(@Param("userId") UUID userId);

  /**
   * 봇 타입별 보관 수 조회
   * 사용자별로 각 봇 타입에 몇 개씩 저장했는지 카운트
   *
   * @param userId 사용자 ID
   * @param botType 봇 타입 (friend, honey, coworker, senior)
   * @return 해당 봇 타입의 보관 수
   */
  long countByUserIdAndBotTypeAndIsDeletedFalse(UUID userId, String botType);

}
