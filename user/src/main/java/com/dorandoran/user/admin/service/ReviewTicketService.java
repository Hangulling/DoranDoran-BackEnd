package com.dorandoran.user.admin.service;

import com.dorandoran.user.admin.entity.ReviewTicket;
import com.dorandoran.user.admin.entity.ReviewTicketItem;
import com.dorandoran.user.admin.enums.ReviewStatus;
import com.dorandoran.user.admin.repository.ReviewTicketItemRepository;
import com.dorandoran.user.admin.repository.ReviewTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 관리 필요 내역 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewTicketService {

    private final ReviewTicketRepository reviewTicketRepository;
    private final ReviewTicketItemRepository reviewTicketItemRepository;

    /**
     * 티켓 목록 조회
     */
    // TODO: ManagementQueueService의 getManagementQueueList()와 100% 중복 - 팀 내 통합 논의 필요
    @Transactional(readOnly = true)
    public Page<ReviewTicket> getTickets(ReviewStatus status, String agentType, Pageable pageable) {
        if (agentType != null && !agentType.isEmpty()) {
            return reviewTicketRepository.findByStatusAndAgentType(status, agentType, pageable);
        }
        return reviewTicketRepository.findByStatus(status, pageable);
    }

    /**
     * 탭별 카운트 조회
     */
    // TODO: ManagementQueueService의 getCountByItemType()와 100% 중복 - 팀 내 통합 논의 필요
    @Transactional(readOnly = true)
    public Map<String, Long> getTicketCounts(ReviewStatus status) {
        Map<String, Long> counts = new HashMap<>();

        // 전체 카운트
        long total = reviewTicketRepository.countByStatus(status);
        counts.put("total", total);

        // 에이전트 타입별 카운트
        List<Object[]> results = reviewTicketRepository.countByStatusGroupByAgentType(status);
        for (Object[] result : results) {
            String agentType = (String) result[0];
            Long count = (Long) result[1];
            counts.put(agentType != null ? agentType : "UNKNOWN", count);
        }

        return counts;
    }

    /**
     * 티켓 생성
     */
    // TODO: ManagementQueueService의 createManagementQueue()와 100% 중복 - 팀 내 통합 논의 필요
    @Transactional
    public ReviewTicket createTicket(UUID conversationId, String agentType, String note, UUID createdBy) {
        ReviewTicket ticket = ReviewTicket.builder()
            .conversationId(conversationId)
            .status(ReviewStatus.OPEN)
            .agentType(agentType)
            .note(note)
            .createdBy(createdBy)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        return reviewTicketRepository.save(ticket);
    }

    /**
     * 티켓 항목 저장
     */
    // TODO: ManagementQueue는 JSONB items 배열 사용, ReviewTicket은 별도 테이블 사용 - 구조 차이 존재
    @Transactional
    public void saveTicketItems(ReviewTicket ticket, List<ReviewTicketItem> items) {
        for (ReviewTicketItem item : items) {
            ReviewTicketItem toSave = ReviewTicketItem.builder()
                .ticket(ticket)
                .messageId(item.getMessageId())
                .agentType(item.getAgentType())
                .snapshotJson(item.getSnapshotJson())
                .build();
            reviewTicketItemRepository.save(toSave);
        }
    }

    /**
     * 티켓 상세 조회
     */
    // TODO: ManagementQueueService의 getManagementQueue()와 100% 중복 - 팀 내 통합 논의 필요
    @Transactional(readOnly = true)
    public ReviewTicket getTicket(Long ticketId) {
        return reviewTicketRepository.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("ReviewTicket not found: " + ticketId));
    }

    @Transactional(readOnly = true)
    public List<ReviewTicketItem> getTicketItems(Long ticketId) {
        return reviewTicketItemRepository.findByTicket_Id(ticketId);
    }

    /**
     * 티켓 메모 수정
     */
    // TODO: ManagementQueueService의 updateManagementQueue()와 100% 중복 - 팀 내 통합 논의 필요
    @Transactional
    public ReviewTicket updateTicket(Long ticketId, String note) {
        ReviewTicket ticket = reviewTicketRepository.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("ReviewTicket not found: " + ticketId));

        ticket = ReviewTicket.builder()
            .id(ticket.getId())
            .conversationId(ticket.getConversationId())
            .status(ticket.getStatus())
            .agentType(ticket.getAgentType())
            .note(note)
            .createdBy(ticket.getCreatedBy())
            .assignee(ticket.getAssignee())
            .createdAt(ticket.getCreatedAt())
            .updatedAt(LocalDateTime.now())
            .doneAt(ticket.getDoneAt())
            .build();

        return reviewTicketRepository.save(ticket);
    }

    /**
     * 티켓 삭제
     */
    // TODO: ManagementQueueService의 deleteManagementQueue()와 100% 중복 - 팀 내 통합 논의 필요
    @Transactional
    public void deleteTicket(Long ticketId) {
        if (!reviewTicketRepository.existsById(ticketId)) {
            throw new RuntimeException("ReviewTicket not found: " + ticketId);
        }
        reviewTicketRepository.deleteById(ticketId);
    }

    /**
     * 다건 처리 완료
     */
    // TODO: ManagementQueueService의 batchCompleteManagementQueue()와 100% 중복 - 팀 내 통합 논의 필요
    @Transactional
    public void completeTickets(List<Long> ticketIds) {
        List<ReviewTicket> tickets = reviewTicketRepository.findByIdIn(ticketIds);

        LocalDateTime now = LocalDateTime.now();
        for (ReviewTicket ticket : tickets) {
            ticket = ReviewTicket.builder()
                .id(ticket.getId())
                .conversationId(ticket.getConversationId())
                .status(ReviewStatus.DONE)
                .agentType(ticket.getAgentType())
                .note(ticket.getNote())
                .createdBy(ticket.getCreatedBy())
                .assignee(ticket.getAssignee())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(now)
                .doneAt(now)
                .build();

            reviewTicketRepository.save(ticket);
        }
    }
}