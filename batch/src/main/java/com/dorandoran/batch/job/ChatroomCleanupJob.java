package com.dorandoran.batch.job;

import com.dorandoran.batch.common.TimeProvider;
import com.dorandoran.batch.service.ChatroomCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatroomCleanupJob {

    private final ChatroomCleanupService cleanupService;
    private final TimeProvider timeProvider;

    // 매일 새벽 3시(Asia/Seoul)
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void purgeSoftDeletedChatrooms() {
        var now = timeProvider.nowKst();
        log.info("[ChatroomCleanupJob] Start at {}", now);
        int affected = cleanupService.hardDeleteSoftDeletedChatrooms();
        log.info("[ChatroomCleanupJob] Finished. deleted = {}", affected);
    }
}


