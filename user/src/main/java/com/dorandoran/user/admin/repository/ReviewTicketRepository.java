package com.dorandoran.user.admin.repository;

import com.dorandoran.user.admin.entity.ReviewTicket;
import com.dorandoran.user.admin.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 관리 필요 내역 Repository
 */
@Repository
public interface ReviewTicketRepository extends JpaRepository<ReviewTicket, Long> {

    /**
     * 상태별 티켓 목록 조회 (페이지네이션)
     */
    Page<ReviewTicket> findByStatus(ReviewStatus status, Pageable pageable);

    /**
     * 상태 및 에이전트 타입별 티켓 목록 조회 (페이지네이션)
     */
    Page<ReviewTicket> findByStatusAndAgentType(ReviewStatus status, String agentType, Pageable pageable);

    /**
     * 상태별 카운트 조회
     */
    long countByStatus(ReviewStatus status);

    /**
     * 에이전트 타입별 카운트 조회
     */
    @Query("SELECT rt.agentType, COUNT(rt) FROM ReviewTicket rt " +
           "WHERE rt.status = :status " +
           "GROUP BY rt.agentType")
    List<Object[]> countByStatusGroupByAgentType(@Param("status") ReviewStatus status);

    /**
     * ID 목록으로 조회
     */
    List<ReviewTicket> findByIdIn(List<Long> ids);
}
