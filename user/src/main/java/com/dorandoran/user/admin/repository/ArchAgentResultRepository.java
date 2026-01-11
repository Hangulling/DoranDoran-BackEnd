package com.dorandoran.user.admin.repository;

import com.dorandoran.user.admin.entity.ArchAgentResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * ArchAgentResult Repository
 *
 * Archive 스키마의 Agent 결과 조회 (읽기 전용)
 */
@Repository
public interface ArchAgentResultRepository extends JpaRepository<ArchAgentResult, UUID> {

  /**
   * 특정 메시지의 모든 Agent 결과 조회
   */
  @Query("SELECT a FROM ArchAgentResult a WHERE a.archMessageId = :messageId")
  List<ArchAgentResult> findByMessageId(@Param("messageId") UUID messageId);
}