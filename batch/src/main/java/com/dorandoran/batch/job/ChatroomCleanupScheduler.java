package com.dorandoran.batch.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatroomCleanupScheduler {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 매일 새벽 3시(Asia/Seoul) 기준으로 soft-delete된 채팅방을 하드 삭제한다.
     * 메시지는 FK ON DELETE CASCADE로 함께 삭제된다.
     */
    @Transactional
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void purgeSoftDeletedChatrooms() {
        ZonedDateTime nowKst = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        log.info("[ChatroomCleanup] Start hard-deleting soft-deleted chatrooms at {}", nowKst);

        // FK ON DELETE CASCADE 보장: docker/scripts/init-shared-db.sql 확인 완료
        String sql = "DELETE FROM chat_schema.chatrooms WHERE is_deleted = TRUE";

        int affected = jdbcTemplate.update(sql);
        log.info("[ChatroomCleanup] Deleted chatrooms count = {}", affected);
    }
}


