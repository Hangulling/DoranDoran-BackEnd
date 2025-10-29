package com.dorandoran.batch.controller;

import com.dorandoran.batch.job.ChatroomCleanupScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Batch 작업 수동 실행 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/batch/jobs")
@RequiredArgsConstructor
public class BatchJobController {

    private final ChatroomCleanupScheduler chatroomCleanupScheduler;

    /**
     * 채팅방 정리 작업 수동 실행
     */
    @PostMapping("/chatroom-cleanup")
    public ResponseEntity<String> triggerChatroomCleanup() {
        try {
            log.info("[BatchJobController] Manual chatroom cleanup triggered");
            chatroomCleanupScheduler.purgeSoftDeletedChatrooms();
            return ResponseEntity.ok("Chatroom cleanup job completed successfully");
        } catch (Exception e) {
            log.error("[BatchJobController] Chatroom cleanup job failed", e);
            return ResponseEntity.status(500).body("Chatroom cleanup job failed: " + e.getMessage());
        }
    }
}
