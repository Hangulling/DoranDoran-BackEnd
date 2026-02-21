package com.dorandoran.user.admin.repository;

import com.dorandoran.user.admin.entity.ReviewTicketItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 관리 필요 내역 항목 Repository
 */
@Repository
public interface ReviewTicketItemRepository extends JpaRepository<ReviewTicketItem, Long> {
    List<ReviewTicketItem> findByTicket_Id(Long ticketId);
}
