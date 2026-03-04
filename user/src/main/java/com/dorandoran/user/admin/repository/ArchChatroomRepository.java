package com.dorandoran.user.admin.repository;

import com.dorandoran.user.admin.dto.response.ChatLogListResponse;
import com.dorandoran.user.admin.entity.ArchChatroom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ArchChatroom Repository
 *
 * Archive 스키마의 채팅방 조회 (읽기 전용)
 */
@Repository
public interface ArchChatroomRepository extends JpaRepository<ArchChatroom, UUID> {
  /**
   * 채팅 로그 검색 (검색 조건 + 페이징)
   *
   * @param intimacyLevel 친밀도 레벨 (선택)
   * @param startDateTime 시작 일시 (필수)
   * @param endDateTime 종료 일시 (필수)
   * @param pageable 페이징 정보
   * @return 페이징된 채팅방 정보
   */
  @Query("SELECT new com.dorandoran.user.admin.dto.response.ChatLogListResponse(" +
      "c.id, c.name, c.concept, c.chatbotIntimacyLevelSnapshot, " +
      "c.lastMessageAt, " +
      "(SELECT COUNT(m.id) FROM ArchMessage m WHERE m.archChatroomId = c.id), " +
      "c.userEmailSnapshot) " +
      "FROM ArchChatroom c " +
      "WHERE (:concept IS NULL OR c.concept = :concept) " +
      "AND (:intimacyLevel IS NULL OR c.chatbotIntimacyLevelSnapshot = :intimacyLevel) " +
      "AND c.sourceCreatedAt >= :startDateTime " +
      "AND c.sourceCreatedAt <= :endDateTime " +
      "ORDER BY c.lastMessageAt DESC")
  Page<ChatLogListResponse> searchChatLogs(
      @Param("concept") String concept,
      @Param("intimacyLevel") Integer intimacyLevel,
      @Param("startDateTime") LocalDateTime startDateTime,
      @Param("endDateTime") LocalDateTime endDateTime,
      Pageable pageable
  );
}