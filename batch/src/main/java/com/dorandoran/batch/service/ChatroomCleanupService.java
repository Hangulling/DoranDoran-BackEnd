package com.dorandoran.batch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatroomCleanupService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public int hardDeleteSoftDeletedChatrooms() {
        String sql = "DELETE FROM chat_schema.chatrooms WHERE is_deleted = TRUE";
        int affected = jdbcTemplate.update(sql);
        log.info("[ChatroomCleanupService] hard-deleted chatrooms = {}", affected);
        return affected;
    }
}


