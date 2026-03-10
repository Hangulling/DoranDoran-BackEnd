package com.dorandoran.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 회원 탈퇴 시 해당 회원 관련 데이터(유저 정보 제외)를 archive_schema로 이관.
 * Batch 미사용, User 서비스 내부에서 JdbcTemplate으로 전부 처리.
 * 이관: arch_chatrooms, arch_messages, arch_stores, arch_usage_events, arch_intimacy_progress, arch_agent_results.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserWithdrawalArchiveService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CONCEPT_DEFAULT = "FRIEND";

    /**
     * 탈퇴 시 billing.monthly_user_costs 삭제.
     * FK 없이 user_id만 참조하므로 app_user 삭제 시 고아 행이 남음. 선행 삭제로 정리.
     */
    public void deleteMonthlyUserCosts(UUID userId) {
        try {
            int deleted = jdbcTemplate.update(
                "DELETE FROM billing.monthly_user_costs WHERE user_id = ?", userId);
            if (deleted > 0) {
                log.info("[탈퇴] billing.monthly_user_costs 삭제: userId={}, rows={}", userId, deleted);
            } else {
                log.debug("[탈퇴] billing.monthly_user_costs 없음(스킵): userId={}", userId);
            }
        } catch (Exception e) {
            log.warn("[탈퇴] billing.monthly_user_costs 삭제 실패(탈퇴는 계속 진행): userId={}, reason={}", userId, e.getMessage());
        }
    }

    /**
     * 탈퇴 시 chat_schema.user_chatbot_last_interaction 삭제.
     * 해당 테이블: 사용자별·챗봇별 마지막 상호작용 시각/방 ID (채팅 목록 최신순 등에 사용).
     * app_user 삭제 전에 삭제하지 않으면 고아 레코드가 남을 수 있음.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRED)
    public void deleteUserChatbotLastInteraction(UUID userId) {
        try {
            int deleted = jdbcTemplate.update(
                "DELETE FROM chat_schema.user_chatbot_last_interaction WHERE user_id = ?",
                userId);
            if (deleted > 0) {
                log.info("[탈퇴] user_chatbot_last_interaction 삭제: userId={}, rows={}", userId, deleted);
            } else {
                log.debug("[탈퇴] user_chatbot_last_interaction 없음(스킵): userId={}", userId);
            }
        } catch (Exception e) {
            log.warn("[탈퇴] user_chatbot_last_interaction 삭제 실패(탈퇴는 계속 진행): userId={}, reason={}", userId, e.getMessage());
        }
    }

    @Transactional
    public void archiveUserData(UUID userId) {
        log.info("[아카이브] 회원 데이터 아카이빙 시작: userId={}", userId);
        try {
            String roomIdsSql = "SELECT id FROM chat_schema.chatrooms WHERE user_id = ?";
            List<UUID> chatroomIds = jdbcTemplate.queryForList(roomIdsSql, UUID.class, userId);
            log.info("[아카이브] 대상 채팅방 수: userId={}, chatroomCount={}", userId, chatroomIds.size());

            for (UUID chatroomId : chatroomIds) {
                archiveChatroom(chatroomId, userId);
            }

            log.info("[아카이브] 회원 데이터 아카이빙 완료: userId={}, 처리 채팅방 수={}", userId, chatroomIds.size());
        } catch (Exception e) {
            log.warn("[아카이브] 회원 데이터 아카이빙 중 예외(탈퇴는 계속 진행): userId={}, reason={}", userId, e.getMessage(), e);
        }
    }

    private void archiveChatroom(UUID chatroomId, UUID userId) {
        log.info("[아카이브] 채팅방 이관 시작: chatroomId={}", chatroomId);
        try {
            List<Integer> exists = jdbcTemplate.queryForList(
                "SELECT 1 FROM archive_schema.arch_chatrooms WHERE source_chatroom_id = ? LIMIT 1",
                Integer.class, chatroomId);
            if (!exists.isEmpty()) {
                log.info("[아카이브] 채팅방 이미 이관됨(스킵): chatroomId={}", chatroomId);
                return;
            }

            Map<String, Object> room = queryRoom(chatroomId);
            if (room == null) {
                log.warn("[아카이브] 채팅방 조회 실패로 스킵: chatroomId={}", chatroomId);
                return;
            }

            Map<String, Object> userData = queryUser((UUID) room.get("user_id"));
            Map<String, Object> chatbotData = queryChatbot((UUID) room.get("chatbot_id"));
            Map<String, Object> progressData = queryIntimacyProgress(chatroomId);

            String concept = extractConcept(toNullableString(room.get("settings")));
            String userEmail = userData != null ? toNullableString(userData.get("email")) : null;
            String chatbotName = chatbotData != null ? toNullableString(chatbotData.get("name")) : null;
            String chatbotType = chatbotData != null ? toNullableString(chatbotData.get("bot_type")) : null;
            Integer chatbotIntimacy = chatbotData != null && chatbotData.get("intimacy_level") != null
                ? ((Number) chatbotData.get("intimacy_level")).intValue() : null;
            if (progressData != null && progressData.get("intimacy_level") != null) {
                chatbotIntimacy = ((Number) progressData.get("intimacy_level")).intValue();
            }

            jdbcTemplate.update("""
                INSERT INTO archive_schema.arch_chatrooms (
                    id, source_chatroom_id, user_id, user_email_snapshot, chatbot_id,
                    chatbot_name_snapshot, chatbot_type_snapshot, chatbot_intimacy_level_snapshot,
                    name, description, concept, last_message_at, source_last_message_id,
                    is_archived, is_deleted, source_created_at, source_updated_at, archived_at, meta
                ) VALUES (
                    gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), '{}'::jsonb
                )
                """,
                chatroomId, room.get("user_id"), userEmail, room.get("chatbot_id"), chatbotName,
                chatbotType, chatbotIntimacy,
                toNullableString(room.get("name")), toNullableString(room.get("description")), concept,
                room.get("last_message_at"), room.get("last_message_id"),
                room.get("is_archived") != null ? room.get("is_archived") : false,
                room.get("is_deleted") != null ? room.get("is_deleted") : false,
                room.get("created_at"), room.get("updated_at")
            );

            UUID archChatroomId = jdbcTemplate.queryForObject(
                "SELECT id FROM archive_schema.arch_chatrooms WHERE source_chatroom_id = ?",
                UUID.class, chatroomId);
            log.info("[아카이브] arch_chatrooms 이관 완료: chatroomId={}, archChatroomId={}", chatroomId, archChatroomId);

            archiveStores(chatroomId, archChatroomId);
            archiveUsageEvents(chatroomId, archChatroomId);
            archiveIntimacyProgress(chatroomId, archChatroomId);
            archiveMessages(chatroomId, archChatroomId);
            log.info("[아카이브] 채팅방 이관 완료: chatroomId={}, archChatroomId={}", chatroomId, archChatroomId);
        } catch (Exception e) {
            log.warn("[아카이브] 채팅방 이관 중 예외(스킵, 탈퇴는 계속 진행): chatroomId={}, reason={}", chatroomId, e.getMessage(), e);
        }
    }

    private void archiveStores(UUID chatroomId, UUID archChatroomId) {
        try {
            List<Map<String, Object>> stores = jdbcTemplate.queryForList("""
                SELECT id, message_id, user_id, content, corrected_content,
                       ai_response, bot_type, is_deleted, deleted_at, created_at, updated_at
                FROM store_schema.stores WHERE chatroom_id = ? AND is_deleted = false
                """, chatroomId);
            int inserted = 0;
            for (Map<String, Object> s : stores) {
                UUID sourceStoreId = (UUID) s.get("id");
                List<Integer> ex = jdbcTemplate.queryForList(
                    "SELECT 1 FROM archive_schema.arch_stores WHERE source_store_id = ? LIMIT 1",
                    Integer.class, sourceStoreId);
                if (!ex.isEmpty()) continue;
                Object aiResp = s.get("ai_response");
                String aiRespJson = aiResp != null ? aiResp.toString() : "{}";
                if (!aiRespJson.trim().startsWith("{")) aiRespJson = "{}";
                jdbcTemplate.update("""
                    INSERT INTO archive_schema.arch_stores (
                        id, arch_chatroom_id, source_store_id, source_message_id, user_id,
                        content, corrected_content, ai_response, bot_type,
                        is_deleted, deleted_at, source_created_at, source_updated_at, archived_at
                    ) VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, now())
                    """,
                    archChatroomId, sourceStoreId, s.get("message_id"), s.get("user_id"),
                    s.get("content"), s.get("corrected_content"), aiRespJson,
                    s.get("bot_type") != null ? s.get("bot_type") : "unknown",
                    s.get("is_deleted") != null ? s.get("is_deleted") : false,
                    s.get("deleted_at"), s.get("created_at"), s.get("updated_at")
                );
                inserted++;
            }
            log.info("[아카이브] arch_stores 이관 완료: chatroomId={}, archChatroomId={}, 이관건수={}", chatroomId, archChatroomId, inserted);
        } catch (Exception e) {
            log.warn("[아카이브] arch_stores 이관 중 예외(스킵): chatroomId={}, reason={}", chatroomId, e.getMessage(), e);
        }
    }

    private void archiveUsageEvents(UUID chatroomId, UUID archChatroomId) {
        try {
            List<Map<String, Object>> events = jdbcTemplate.queryForList("""
                SELECT id, user_id, event_time, provider, model, request_id,
                       input_tokens, output_tokens, cost_in, cost_out, meta
                FROM billing.ai_usage_events WHERE chatroom_id = ?
                ORDER BY event_time ASC
                """, chatroomId);
            int inserted = 0;
            for (Map<String, Object> ev : events) {
                UUID sourceId = (UUID) ev.get("id");
                List<Integer> ex = jdbcTemplate.queryForList(
                    "SELECT 1 FROM archive_schema.arch_usage_events WHERE source_usage_event_id = ? LIMIT 1",
                    Integer.class, sourceId);
                if (!ex.isEmpty()) continue;
                Object metaObj = ev.get("meta");
                String metaJson = metaObj != null ? metaObj.toString() : null;
                Number costIn = ev.get("cost_in") != null ? ((Number) ev.get("cost_in")) : 0;
                Number costOut = ev.get("cost_out") != null ? ((Number) ev.get("cost_out")) : 0;
                jdbcTemplate.update("""
                    INSERT INTO archive_schema.arch_usage_events (
                        id, arch_chatroom_id, source_usage_event_id, user_id, event_time,
                        provider, model, request_id, input_tokens, output_tokens, cost_in, cost_out,
                        meta, archived_at
                    ) VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, now())
                    """,
                    archChatroomId, sourceId, ev.get("user_id"), ev.get("event_time"),
                    ev.get("provider") != null ? ev.get("provider") : "",
                    ev.get("model") != null ? ev.get("model") : "",
                    ev.get("request_id"),
                    ev.get("input_tokens") != null ? ((Number) ev.get("input_tokens")).intValue() : 0,
                    ev.get("output_tokens") != null ? ((Number) ev.get("output_tokens")).intValue() : 0,
                    costIn.doubleValue(), costOut.doubleValue(),
                    metaJson != null && metaJson.trim().startsWith("{") ? metaJson : "{}"
                );
                inserted++;
            }
            log.info("[아카이브] arch_usage_events 이관 완료: chatroomId={}, archChatroomId={}, 이관건수={}", chatroomId, archChatroomId, inserted);
        } catch (Exception e) {
            log.warn("[아카이브] arch_usage_events 이관 중 예외(스킵): chatroomId={}, reason={}", chatroomId, e.getMessage(), e);
        }
    }

    private void archiveIntimacyProgress(UUID chatroomId, UUID archChatroomId) {
        try {
            Map<String, Object> p = jdbcTemplate.queryForMap("""
                SELECT id, user_id, intimacy_level, total_corrections, last_feedback, last_updated, progress_data
                FROM chat_schema.intimacy_progress WHERE chatroom_id = ?
                """, chatroomId);
            UUID sourceId = (UUID) p.get("id");
            List<Integer> ex = jdbcTemplate.queryForList(
                "SELECT 1 FROM archive_schema.arch_intimacy_progress WHERE source_intimacy_progress_id = ? LIMIT 1",
                Integer.class, sourceId);
            if (!ex.isEmpty()) {
                log.info("[아카이브] arch_intimacy_progress 이미 이관됨(스킵): chatroomId={}", chatroomId);
                return;
            }
            Object progObj = p.get("progress_data");
            String progJson = progObj != null ? progObj.toString() : null;
            jdbcTemplate.update("""
                INSERT INTO archive_schema.arch_intimacy_progress (
                    id, arch_chatroom_id, source_intimacy_progress_id, user_id,
                    intimacy_level, total_corrections, last_feedback, last_updated, progress_data, archived_at
                ) VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?, ?::jsonb, now())
                """,
                archChatroomId, sourceId, p.get("user_id"),
                p.get("intimacy_level") != null ? ((Number) p.get("intimacy_level")).intValue() : 1,
                p.get("total_corrections") != null ? ((Number) p.get("total_corrections")).intValue() : 0,
                p.get("last_feedback"), p.get("last_updated"),
                progJson != null && progJson.trim().startsWith("{") ? progJson : "{}"
            );
            log.info("[아카이브] arch_intimacy_progress 이관 완료: chatroomId={}, archChatroomId={}, sourceId={}", chatroomId, archChatroomId, sourceId);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            log.info("[아카이브] arch_intimacy_progress 없음(스킵): chatroomId={}", chatroomId);
        } catch (Exception e) {
            log.warn("[아카이브] arch_intimacy_progress 이관 실패: chatroomId={}, reason={}", chatroomId, e.getMessage(), e);
        }
    }

    private void archiveMessages(UUID chatroomId, UUID archChatroomId) {
        try {
            List<Map<String, Object>> messages = jdbcTemplate.queryForList("""
                SELECT id, parent_message_id, sender_type, sender_id, content, content_type,
                       sequence_number, turn_number, token_count, processing_time_ms,
                       is_edited, edited_at, is_deleted, deleted_at, created_at, updated_at, metadata
                FROM chat_schema.messages WHERE chatroom_id = ? ORDER BY sequence_number ASC
                """, chatroomId);

            int messagesInserted = 0;
            int agentResultsInserted = 0;
            for (Map<String, Object> m : messages) {
                UUID sourceMessageId = (UUID) m.get("id");
                List<Integer> msgExists = jdbcTemplate.queryForList(
                    "SELECT 1 FROM archive_schema.arch_messages WHERE source_message_id = ? LIMIT 1",
                    Integer.class, sourceMessageId);
                if (!msgExists.isEmpty()) continue;

                jdbcTemplate.update("""
                    INSERT INTO archive_schema.arch_messages (
                        id, arch_chatroom_id, source_message_id, source_parent_message_id,
                        sender_type, sender_id, content, content_type, sequence_number, turn_number,
                        token_count, processing_time_ms, is_edited, edited_at, is_deleted, deleted_at,
                        source_created_at, source_updated_at, archived_at, metadata_json
                    ) VALUES (
                        gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), ?::jsonb
                    )
                    """,
                    archChatroomId, sourceMessageId, m.get("parent_message_id"),
                    m.get("sender_type"), m.get("sender_id"), m.get("content"),
                    m.get("content_type") != null ? m.get("content_type") : "text",
                    m.get("sequence_number") != null ? ((Number) m.get("sequence_number")).longValue() : 0L,
                    m.get("turn_number") != null ? ((Number) m.get("turn_number")).longValue() : 0L,
                    m.get("token_count"), m.get("processing_time_ms"),
                    m.get("is_edited") != null ? m.get("is_edited") : false,
                    m.get("edited_at"), m.get("is_deleted") != null ? m.get("is_deleted") : false,
                    m.get("deleted_at"), m.get("created_at"), m.get("updated_at"),
                    toJsonLiteral(m.get("metadata"))
                );
                messagesInserted++;

                UUID archMessageId = jdbcTemplate.queryForObject(
                    "SELECT id FROM archive_schema.arch_messages WHERE source_message_id = ?",
                    UUID.class, sourceMessageId);
                agentResultsInserted += archiveAgentResults(archMessageId, m.get("metadata"), m.get("created_at"));
            }
            log.info("[아카이브] arch_messages 이관 완료: chatroomId={}, archChatroomId={}, 메시지={}건, agent결과={}건", chatroomId, archChatroomId, messagesInserted, agentResultsInserted);
        } catch (Exception e) {
            log.warn("[아카이브] arch_messages 이관 중 예외(스킵): chatroomId={}, reason={}", chatroomId, e.getMessage(), e);
        }
    }

    private int archiveAgentResults(UUID archMessageId, Object metadataObj, Object sourceCreatedAt) {
        if (metadataObj == null) return 0;
        String metadataJson = metadataObj.toString();
        if (metadataJson.isBlank() || !metadataJson.trim().startsWith("{")) return 0;
        int inserted = 0;
        try {
            JsonNode meta = objectMapper.readTree(metadataJson);
            if (meta.has("userMessageAnalysis") && meta.get("userMessageAnalysis").has("intimacy")) {
                JsonNode intimacy = meta.get("userMessageAnalysis").get("intimacy");
                insertAgentResult(archMessageId, "intimacy", intimacy, meta, sourceCreatedAt);
                inserted++;
            }
            if (meta.has("botResponseAnalysis") && meta.get("botResponseAnalysis").has("vocabulary")) {
                JsonNode vocab = meta.get("botResponseAnalysis").get("vocabulary");
                if (vocab.has("words") && vocab.get("words").isArray()) {
                    com.fasterxml.jackson.databind.node.ObjectNode payload = objectMapper.createObjectNode();
                    payload.set("words", vocab.get("words"));
                    insertAgentResult(archMessageId, "voca", payload, meta, sourceCreatedAt);
                    inserted++;
                }
            }
        } catch (Exception e) {
            log.debug("[아카이브] Agent 결과 추출 스킵: archMessageId={}, reason={}", archMessageId, e.getMessage());
        }
        return inserted;
    }

    private void insertAgentResult(UUID archMessageId, String agentType, JsonNode payloadJson, JsonNode originalMeta, Object sourceCreatedAt) {
        try {
            String requestId = null;
            String provider = null;
            String model = null;
            Integer inputTokens = null;
            Integer outputTokens = null;
            if (originalMeta != null && originalMeta.has("usage")) {
                JsonNode u = originalMeta.get("usage");
                if (u.has("requestId")) requestId = u.get("requestId").asText();
                if (u.has("provider")) provider = u.get("provider").asText();
                if (u.has("model")) model = u.get("model").asText();
                if (u.has("inputTokens")) inputTokens = u.get("inputTokens").asInt();
                if (u.has("outputTokens")) outputTokens = u.get("outputTokens").asInt();
            }
            String payloadStr = payloadJson != null ? payloadJson.toString() : "{}";
            jdbcTemplate.update("""
                INSERT INTO archive_schema.arch_agent_results (
                    id, arch_message_id, agent_type, payload_json,
                    request_id, provider, model, input_tokens, output_tokens, source_created_at, archived_at
                ) VALUES (gen_random_uuid(), ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, now())
                """,
                archMessageId, agentType, payloadStr,
                requestId, provider, model, inputTokens, outputTokens, sourceCreatedAt
            );
        } catch (Exception e) {
            log.warn("[아카이브] arch_agent_results INSERT 중 예외(스킵): archMessageId={}, agentType={}, reason={}", archMessageId, agentType, e.getMessage(), e);
        }
    }

    private static String toNullableString(Object o) {
        if (o == null) return null;
        if (o instanceof org.postgresql.util.PGobject) {
            return ((org.postgresql.util.PGobject) o).getValue();
        }
        return o.toString();
    }

    private static String toJsonLiteral(Object o) {
        if (o == null) return "{}";
        String s = toNullableString(o);
        if (s == null || s.trim().isEmpty() || !s.trim().startsWith("{")) return "{}";
        return s;
    }

    private Map<String, Object> queryRoom(UUID chatroomId) {
        try {
            return jdbcTemplate.queryForMap("""
                SELECT id, user_id, chatbot_id, name, description, settings,
                       last_message_at, last_message_id, is_archived, is_deleted, created_at, updated_at
                FROM chat_schema.chatrooms WHERE id = ?
                """, chatroomId);
        } catch (Exception e) {
            log.warn("채팅방 조회 실패: chatroomId={}", chatroomId, e);
            return null;
        }
    }

    private Map<String, Object> queryUser(UUID userId) {
        if (userId == null) return null;
        try {
            return jdbcTemplate.queryForMap("SELECT id, email FROM user_schema.app_user WHERE id = ?", userId);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> queryChatbot(UUID chatbotId) {
        if (chatbotId == null) return null;
        try {
            return jdbcTemplate.queryForMap(
                "SELECT id, name, bot_type, intimacy_level FROM chat_schema.chatbots WHERE id = ?", chatbotId);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> queryIntimacyProgress(UUID chatroomId) {
        try {
            return jdbcTemplate.queryForMap(
                "SELECT intimacy_level FROM chat_schema.intimacy_progress WHERE chatroom_id = ?", chatroomId);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractConcept(String settingsJson) {
        if (settingsJson == null || settingsJson.isBlank()) return CONCEPT_DEFAULT;
        try {
            int i = settingsJson.indexOf("\"concept\"");
            if (i == -1) return CONCEPT_DEFAULT;
            int start = settingsJson.indexOf('"', i + 10) + 1;
            int end = settingsJson.indexOf('"', start);
            if (start > 0 && end > start) {
                return settingsJson.substring(start, end).toUpperCase();
            }
        } catch (Exception ignored) { }
        return CONCEPT_DEFAULT;
    }
}
