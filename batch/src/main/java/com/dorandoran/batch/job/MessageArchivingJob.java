package com.dorandoran.batch.job;

import com.dorandoran.batch.common.TimeProvider;
import com.dorandoran.batch.service.MessageArchivingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageArchivingJob {

    private final MessageArchivingService messageArchivingService;
    private final TimeProvider timeProvider;

    /**
     * 3. 채팅 메시지 아카이빙 (Message Archiving)
     * 매일 새벽 4시
     */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void archiveOldMessages() {
        var now = timeProvider.nowKst();
        log.info("[MessageArchivingJob] Start at {}", now);

        int archived = messageArchivingService.archiveMessagesOlderThan6Months();
        int purged = messageArchivingService.purgeMessagesFromDeletedRooms();

        log.info("[MessageArchivingJob] Finished. archived(6m)={}, purged(deleted rooms)={}", archived, purged);
    }
}


