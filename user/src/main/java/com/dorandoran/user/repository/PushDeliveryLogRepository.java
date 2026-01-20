package com.dorandoran.user.repository;

import com.dorandoran.user.entity.PushDeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface PushDeliveryLogRepository extends JpaRepository<PushDeliveryLog, Long> {
    boolean existsByUserIdAndChatroomIdAndSentDate(UUID userId, UUID chatroomId, LocalDate sentDate);
}
