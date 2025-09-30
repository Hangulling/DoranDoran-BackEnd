package com.dorandoran.modules.chat.repository;

import com.dorandoran.modules.chat.entity.Chatroom;
import com.dorandoran.modules.user.entity.DoranUser;
import com.dorandoran.shared.enums.BotType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatroomRepository extends JpaRepository<Chatroom, UUID> {

  // 채팅방 단일 조회 (삭제되지 않은 것만)
  @EntityGraph(attributePaths = {"bot", "user"})
  @Query("SELECT c FROM Chatroom c WHERE c.roomId = :roomId AND c.isDeleted = false")
  Optional<Chatroom> findActiveById(@Param("roomId") UUID roomId);

  // 사용자의 모든 채팅방 조회 (삭제되지 않은 것만, 최근 대화 순)
  @EntityGraph(attributePaths = {"bot"})
  @Query("SELECT c FROM Chatroom c WHERE c.user.userId = :userId AND c.isDeleted = false " +
      "ORDER BY c.updateAt DESC NULLS LAST")
  List<Chatroom> findAllByUserIdAndNotDeleted(@Param("userId") UUID userId);

  // 사용자의 채팅방 페이징 조회
  @Query("SELECT c FROM Chatroom c WHERE c.user.userId = :userId AND c.isDeleted = false")
  Page<Chatroom> findByUserIdAndNotDeleted(@Param("userId") UUID userId, Pageable pageable);

  // 사용자와 봇 타입으로 채팅방 조회
  @Query("SELECT c FROM Chatroom c JOIN c.bot b WHERE c.user.userId = :userId " +
      "AND b.botType = :botType AND c.isDeleted = false")
  Optional<Chatroom> findByUserIdAndBotType(@Param("userId") UUID userId, @Param("botType") BotType botType);

  // 채팅방 존재 여부 확인
  @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Chatroom c " +
      "WHERE c.roomId = :roomId AND c.isDeleted = false")
  boolean existsByRoomIdAndNotDeleted(@Param("roomId") UUID roomId);

  // 채팅방 소프트 삭제
  @Modifying
  @Query("UPDATE Chatroom c SET c.isDeleted = true, c.updateAt = :now WHERE c.roomId = :roomId")
  void softDelete(@Param("roomId") UUID roomId, @Param("now") LocalDateTime now);

  // 채팅방 이름 수정
  @Modifying
  @Query("UPDATE Chatroom c SET c.roomName = :roomName, c.updateAt = :now WHERE c.roomId = :roomId")
  void updateRoomName(@Param("roomId") UUID roomId, @Param("roomName") String roomName, @Param("now") LocalDateTime now);

  // 채팅방 업데이트 시간 갱신
  @Modifying
  @Query("UPDATE Chatroom c SET c.updateAt = :now WHERE c.roomId = :roomId")
  void updateLastActivityTime(@Param("roomId") UUID roomId, @Param("now") LocalDateTime now);

  // 사용자가 가진 채팅방 개수
  @Query("SELECT COUNT(c) FROM Chatroom c WHERE c.user.userId = :userId AND c.isDeleted = false")
  Long countByUserId(@Param("userId") UUID userId);
}