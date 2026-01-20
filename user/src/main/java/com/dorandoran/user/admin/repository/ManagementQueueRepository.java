package com.dorandoran.user.admin.repository;

import com.dorandoran.user.admin.entity.ManagementQueue;
import com.dorandoran.user.admin.enums.QueueStatus;
import com.dorandoran.user.admin.enums.QueueType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;


@Repository
public interface ManagementQueueRepository extends JpaRepository<ManagementQueue, UUID> {

  /**
   * 상태별 조회 (페이징)
   *
   * @param status 상태 (PENDING, COMPLETED)
   * @param pageable 페이징 정보
   * @return 페이징된 관리 내역 목록
   */
  Page<ManagementQueue> findByStatus(QueueStatus status, Pageable pageable);

  /**
   * 타입별 조회 (페이징)
   *
   * @param queueType 큐 타입 (CORRECTION, DELETION)
   * @param pageable 페이징 정보
   * @return 페이징된 관리 내역 목록
   */
  Page<ManagementQueue> findByQueueType(QueueType queueType, Pageable pageable);

  /**
   * 타입 + 상태별 조회 (페이징)
   *
   * @param queueType 큐 타입
   * @param status 상태
   * @param pageable 페이징 정보
   * @return 페이징된 관리 내역 목록
   */
  Page<ManagementQueue> findByQueueTypeAndStatus(
      QueueType queueType,
      QueueStatus status,
      Pageable pageable
  );

  /**
   * 관리자별 조회 (감사 로그용)
   *
   * @param adminName 관리자 이메일
   * @param pageable 페이징 정보
   * @return 페이징된 관리 내역 목록
   */
  Page<ManagementQueue> findByAdminName(String adminName, Pageable pageable);

  /**
   * 기간별 조회 (감사 로그용)
   *
   * @param startDate 시작일
   * @param endDate 종료일
   * @param pageable 페이징 정보
   * @return 페이징된 관리 내역 목록
   */
  @Query("SELECT mq FROM ManagementQueue mq " +
      "WHERE mq.createdAt >= :startDate AND mq.createdAt <= :endDate " +
      "ORDER BY mq.createdAt DESC")
  Page<ManagementQueue> findByCreatedAtBetween(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      Pageable pageable
  );

  /**
   * 타입 + 관리자 + 기간별 조회 (감사 로그용)
   *
   * @param queueType 큐 타입 (nullable)
   * @param adminName 관리자 이메일 (nullable)
   * @param startDate 시작일
   * @param endDate 종료일
   * @param pageable 페이징 정보
   * @return 페이징된 관리 내역 목록
   */
  @Query("SELECT mq FROM ManagementQueue mq " +
      "WHERE (:queueType IS NULL OR mq.queueType = :queueType) " +
      "AND (:adminName IS NULL OR mq.adminName = :adminName) " +
      "AND mq.createdAt >= :startDate AND mq.createdAt <= :endDate " +
      "ORDER BY mq.createdAt DESC")
  Page<ManagementQueue> findByFilters(
      @Param("queueType") QueueType queueType,
      @Param("adminName") String adminName,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      Pageable pageable
  );

  /**
   * 대기 중인 항목 개수 조회
   *
   * @return PENDING 상태 항목 개수
   */
  Long countByStatus(QueueStatus status);

  /**
   * 관리자별 처리 건수 조회 (통계용)
   *
   * @param adminName 관리자 이메일
   * @param status 상태
   * @return 처리 건수
   */
  @Query("SELECT COUNT(mq) FROM ManagementQueue mq " +
      "WHERE mq.adminName = :adminName AND mq.status = :status")
  Long countByAdminNameAndStatus(
      @Param("adminName") String adminName,
      @Param("status") QueueStatus status
  );
}