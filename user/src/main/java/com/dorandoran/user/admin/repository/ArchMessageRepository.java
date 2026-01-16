package com.dorandoran.user.admin.repository;

import com.dorandoran.user.admin.entity.ArchMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * ArchMessage Repository
 *
 * Archive 스키마의 메시지 조회 (읽기 전용)
 */
@Repository
public interface ArchMessageRepository extends JpaRepository<ArchMessage, UUID> {

  /**
   * 특정 채팅방의 메시지 조회 (페이징)
   */
  @Query("SELECT m FROM ArchMessage m WHERE m.archChatroomId = :chatroomId ORDER BY m.sequenceNumber ASC")
  Page<ArchMessage> findByChatroomIdOrderBySequence(@Param("chatroomId") UUID chatroomId, Pageable pageable);
}