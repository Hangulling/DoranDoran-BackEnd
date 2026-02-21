package com.dorandoran.user.service;

import com.dorandoran.user.dto.PushDeliveryLogResponse;
import com.dorandoran.user.entity.PushDeliveryLog;
import com.dorandoran.user.repository.PushDeliveryLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PushDeliveryLogService {

    private final PushDeliveryLogRepository pushDeliveryLogRepository;

    @Transactional(readOnly = true)
    public List<PushDeliveryLogResponse> findLogs(UUID userId, UUID chatroomId, LocalDate sentDate, int limit) {
        List<PushDeliveryLog> logs = pushDeliveryLogRepository.findAll();
        return logs.stream()
            .filter(log -> userId == null || userId.equals(log.getUserId()))
            .filter(log -> chatroomId == null || chatroomId.equals(log.getChatroomId()))
            .filter(log -> sentDate == null || sentDate.equals(log.getSentDate()))
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .limit(limit)
            .map(this::toResponse)
            .toList();
    }

    private PushDeliveryLogResponse toResponse(PushDeliveryLog log) {
        return new PushDeliveryLogResponse(
            log.getId(),
            log.getUserId(),
            log.getChatroomId(),
            log.getSentDate(),
            log.getCreatedAt()
        );
    }
}
