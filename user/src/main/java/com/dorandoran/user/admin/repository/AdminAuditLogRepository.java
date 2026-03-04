package com.dorandoran.user.admin.repository;

import com.dorandoran.user.admin.entity.AdminAuditLog;
import com.dorandoran.user.admin.enums.ActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리 감사 로그 Repository
 */
@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    /**
     * 전체 감사 로그 조회 (페이지네이션, 필터 없을 때 사용)
     */
    Page<AdminAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 관리자별 감사 로그 조회 (페이지네이션)
     */
    Page<AdminAuditLog> findByAdminUserIdOrderByCreatedAtDesc(UUID adminUserId, Pageable pageable);

    /**
     * 작업 타입별 감사 로그 조회 (페이지네이션)
     */
    Page<AdminAuditLog> findByActionTypeOrderByCreatedAtDesc(ActionType actionType, Pageable pageable);

    /**
     * 기간별 감사 로그 조회 (페이지네이션)
     */
    @Query("SELECT al FROM AdminAuditLog al " +
           "WHERE al.createdAt BETWEEN :from AND :to " +
           "ORDER BY al.createdAt DESC")
    Page<AdminAuditLog> findByCreatedAtBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    /**
     * 복합 조건 조회 (페이지네이션)
     */
    @Query("SELECT al FROM AdminAuditLog al " +
           "WHERE (:adminUserId IS NULL OR al.adminUserId = :adminUserId) " +
           "AND (:actionType IS NULL OR al.actionType = :actionType) " +
           "AND (:from IS NULL OR al.createdAt >= :from) " +
           "AND (:to IS NULL OR al.createdAt <= :to) " +
           "ORDER BY al.createdAt DESC")
    Page<AdminAuditLog> findByConditions(
            @Param("adminUserId") UUID adminUserId,
            @Param("actionType") ActionType actionType,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
}
