package com.dorandoran.batch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageArchivingService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 6개월 이상 지난 메시지를 NDJSON로 백업 후 삭제
     */
    @Transactional
    public int archiveMessagesOlderThan6Months() {
        String selectSql = "SELECT id, chatroom_id, sender_type, sender_id, content, content_type, metadata, parent_message_id, sequence_number, token_count, processing_time_ms, created_at " +
                "FROM chat_schema.messages WHERE created_at < NOW() - INTERVAL '6 months'";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(selectSql);
        if (rows.isEmpty()) {
            log.info("[MessageArchivingService] No messages older than 6 months");
            return 0;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        File outFile = new File("./archives/messages-6m-" + timestamp + ".ndjson");
        outFile.getParentFile().mkdirs();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile, StandardCharsets.UTF_8, false))) {
            for (Map<String, Object> row : rows) {
                String json = objectMapper.writeValueAsString(row);
                writer.write(json);
                writer.newLine();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write archive file", e);
        }

        String deleteSql = "DELETE FROM chat_schema.messages WHERE created_at < NOW() - INTERVAL '6 months'";
        int deleted = jdbcTemplate.update(deleteSql);
        log.info("[MessageArchivingService] Archived to {} and deleted messages = {}", outFile.getAbsolutePath(), deleted);
        return deleted;
    }

    /**
     * 소프트 삭제된 채팅방의 메시지를 영구 삭제
     */
    @Transactional
    public int purgeMessagesFromDeletedRooms() {
        String sql = "DELETE FROM chat_schema.messages m USING chat_schema.chatrooms r " +
                "WHERE m.chatroom_id = r.id AND r.is_deleted = TRUE";
        int affected = jdbcTemplate.update(sql);
        log.info("[MessageArchivingService] Purged messages of soft-deleted rooms = {}", affected);
        return affected;
    }
}


