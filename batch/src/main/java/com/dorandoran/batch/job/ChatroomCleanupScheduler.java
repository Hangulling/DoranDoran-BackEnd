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
     * 관련 데이터(메시지, 친밀도 진행도)도 함께 삭제된다.
     */
    @Transactional
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void purgeSoftDeletedChatrooms() {
        ZonedDateTime nowKst = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        log.info("[ChatroomCleanup] Start hard-deleting soft-deleted chatrooms at {}", nowKst);

        // 1. 삭제 대상 채팅방 조회
        String selectSql = "SELECT id FROM chat_schema.chatrooms WHERE is_deleted = TRUE";
        var chatroomIds = jdbcTemplate.queryForList(selectSql, java.util.UUID.class);
        
        if (chatroomIds.isEmpty()) {
            log.info("[ChatroomCleanup] No chatrooms to delete");
            return;
        }

        int chatroomCount = chatroomIds.size();
        log.info("[ChatroomCleanup] Found {} chatrooms to delete", chatroomCount);

        // 2. 관련 메시지 삭제
        String deleteMessagesSql = "DELETE FROM chat_schema.messages WHERE chatroom_id = ?";
        int messageCount = 0;
        for (java.util.UUID chatroomId : chatroomIds) {
            int count = jdbcTemplate.update(deleteMessagesSql, chatroomId);
            messageCount += count;
        }

        // 3. 친밀도 진행도 삭제
        String deleteIntimacySql = "DELETE FROM chat_schema.intimacy_progress WHERE chatroom_id = ?";
        int intimacyCount = 0;
        for (java.util.UUID chatroomId : chatroomIds) {
            int count = jdbcTemplate.update(deleteIntimacySql, chatroomId);
            intimacyCount += count;
        }

        // 4. 채팅방 삭제
        String deleteChatroomsSql = "DELETE FROM chat_schema.chatrooms WHERE id = ?";
        int affected = 0;
        for (java.util.UUID chatroomId : chatroomIds) {
            affected += jdbcTemplate.update(deleteChatroomsSql, chatroomId);
        }

        log.info("[ChatroomCleanup] Deleted {} chatrooms, {} messages, {} intimacy progress records", 
                affected, messageCount, intimacyCount);
    }
}


