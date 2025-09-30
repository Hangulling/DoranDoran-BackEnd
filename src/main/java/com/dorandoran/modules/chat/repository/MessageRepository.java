package com.dorandoran.modules.chat.repository;

import com.dorandoran.modules.chat.entity.Message;
import com.dorandoran.shared.enums.SenderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

  // 채팅방별 메시지 조회 (페이지네이션, 시간 역순)
  @Query("SELECT m FROM Message m WHERE m.chatroom.roomId = :roomId " +
      "ORDER BY m.messageSendTime DESC")
  Page<Message> findByChatroomId(@Param("roomId") UUID roomId, Pageable pageable);

  // timestamp 기반 메시지 조회 (커서 페이지네이션)
  @Query("SELECT m FROM Message m WHERE m.chatroom.roomId = :roomId " +
      "AND m.messageSendTime < :timestamp ORDER BY m.messageSendTime DESC")
  Page<Message> findByChatroomIdAndTimestampBefore(
      @Param("roomId") UUID roomId,
      @Param("timestamp") LocalDateTime timestamp,
      Pageable pageable);

  // 최근 메시지 조회 (캐싱용)
  @Query("SELECT m FROM Message m WHERE m.chatroom.roomId = :roomId " +
      "ORDER BY m.messageSendTime DESC")
  List<Message> findRecentMessages(@Param("roomId") UUID roomId, Pageable pageable);

  // 채팅방의 최대 chatNum 조회
  @Query("SELECT COALESCE(MAX(m.chatNum), 0) FROM Message m WHERE m.chatroom.roomId = :roomId")
  Integer findMaxChatNum(@Param("roomId") UUID roomId);

  // 메시지 ID로 단일 조회
  Optional<Message> findByMessageId(UUID messageId);

  // 특정 채팅방의 마지막 메시지 조회
  @Query("SELECT m FROM Message m WHERE m.chatroom.roomId = :roomId " +
      "ORDER BY m.messageSendTime DESC")
  Optional<Message> findFirstByChatroomIdOrderByMessageSendTimeDesc(@Param("roomId") UUID roomId);

  // 읽지 않은 메시지 개수 조회 (특정 시간 이후)
  @Query("SELECT COUNT(m) FROM Message m WHERE m.chatroom.roomId = :roomId " +
      "AND m.messageSendTime > :lastReadTime AND m.senderType = :senderType")
  Long countUnreadMessages(
      @Param("roomId") UUID roomId,
      @Param("lastReadTime") LocalDateTime lastReadTime,
      @Param("senderType") SenderType senderType);

  // 키워드 검색 (content 내용 검색)
  @Query("SELECT m FROM Message m WHERE m.chatroom.roomId = :roomId " +
      "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
      "ORDER BY m.messageSendTime DESC")
  Page<Message> searchByKeyword(
      @Param("roomId") UUID roomId,
      @Param("keyword") String keyword,
      Pageable pageable);

  // 날짜 범위로 메시지 조회
  @Query("SELECT m FROM Message m WHERE m.chatroom.roomId = :roomId " +
      "AND m.messageSendTime BETWEEN :startDate AND :endDate " +
      "ORDER BY m.messageSendTime DESC")
  List<Message> findByDateRange(
      @Param("roomId") UUID roomId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  // 배치 삭제용: 30일 지난 메시지 중 북마크되지 않은 것
  @Query("SELECT m FROM Message m WHERE m.messageSendTime < :cutoffDate " +
      "AND NOT EXISTS (SELECT s FROM Store s WHERE s.message = m)")
  List<Message> findOldUnbookmarkedMessages(@Param("cutoffDate") LocalDateTime cutoffDate);

  // 채팅방별 메시지 개수
  @Query("SELECT COUNT(m) FROM Message m WHERE m.chatroom.roomId = :roomId")
  Long countByChatroomId(@Param("roomId") UUID roomId);

  // 사용자가 보낸 메시지 조회
  @Query("SELECT m FROM Message m WHERE m.chatroom.roomId = :roomId " +
      "AND m.senderType = :senderType ORDER BY m.messageSendTime DESC")
  Page<Message> findBySenderType(
      @Param("roomId") UUID roomId,
      @Param("senderType") SenderType senderType,
      Pageable pageable);
}