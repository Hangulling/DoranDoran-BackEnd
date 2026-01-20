package com.dorandoran.batch.service;

import com.dorandoran.batch.entity.*;
import com.dorandoran.batch.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Archive 서비스
 * 운영 데이터를 Archive 스키마로 변환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveService {
    
    private final JdbcTemplate jdbcTemplate;
    private final ArchChatroomRepository archChatroomRepository;
    private final ArchMessageRepository archMessageRepository;
    private final ArchAgentResultRepository archAgentResultRepository;
    private final ArchIngestionStateRepository ingestionStateRepository;
    private final ArchStoreRepository archStoreRepository;
    private final ArchUsageEventRepository archUsageEventRepository;
    private final ArchIntimacyProgressRepository archIntimacyProgressRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 채팅방 아카이빙
     */
    @Transactional
    public UUID archiveChatroom(UUID chatroomId) {
        log.info("채팅방 아카이빙 시작: chatroomId={}", chatroomId);
        
        // 이미 아카이빙된 경우 스킵
        if (archChatroomRepository.existsBySourceChatroomId(chatroomId)) {
            log.info("이미 아카이빙됨: chatroomId={}", chatroomId);
            return archChatroomRepository.findBySourceChatroomId(chatroomId)
                .map(ArchChatroom::getId)
                .orElse(null);
        }
        
        // 운영 데이터 조회
        Map<String, Object> roomData = queryChatroomData(chatroomId);
        if (roomData == null) {
            log.warn("채팅방을 찾을 수 없음: chatroomId={}", chatroomId);
            return null;
        }
        
        // User, Chatbot, IntimacyProgress 조회
        Map<String, Object> userData = queryUserData((UUID) roomData.get("user_id"));
        Map<String, Object> chatbotData = queryChatbotData((UUID) roomData.get("chatbot_id"));
        Map<String, Object> progressData = queryIntimacyProgress(chatroomId);
        
        // ArchChatroom 생성
        ArchChatroom archChatroom = buildArchChatroom(roomData, userData, chatbotData, progressData);
        archChatroom = archChatroomRepository.save(archChatroom);
        
        UUID archChatroomId = archChatroom.getId();
        
        // 관련 데이터 아카이빙
        archiveStores(chatroomId, archChatroomId);
        archiveUsageEvents(chatroomId, archChatroomId);
        archiveIntimacyProgress(chatroomId, archChatroomId);
        
        log.info("채팅방 아카이빙 완료: chatroomId={}, archId={}", chatroomId, archChatroomId);
        return archChatroomId;
    }
    
    /**
     * 채팅방의 모든 메시지 아카이빙
     */
    @Transactional
    public void archiveMessages(UUID chatroomId, UUID archChatroomId) {
        log.info("메시지 아카이빙 시작: chatroomId={}, archChatroomId={}", chatroomId, archChatroomId);
        
        // Store ID 맵 조회
        Map<UUID, UUID> storeIdMap = queryStoreIdMap(chatroomId);
        
        // Usage Request ID 맵 조회
        Map<String, String> usageRequestIdMap = queryUsageRequestIdMap(chatroomId);
        
        // 메시지 조회 (페이징)
        // 모든 메시지를 아카이빙하기 위해 페이징 제한을 크게 설정
        int batchSize = 1000;
        int offset = 0;
        int totalArchived = 0;
        int maxOffset = 1000000; // 최대 100만 개 메시지까지 처리 (안전장치)
        
        while (true) {
            List<Map<String, Object>> messages = queryMessages(chatroomId, batchSize, offset);
            if (messages.isEmpty()) {
                break;
            }
            
            for (Map<String, Object> messageData : messages) {
                try {
                    archiveMessage(messageData, archChatroomId, storeIdMap, usageRequestIdMap);
                    totalArchived++;
                } catch (Exception e) {
                    log.error("메시지 아카이빙 실패: messageId={}", messageData.get("id"), e);
                }
            }
            
            offset += batchSize;
            
            // 안전장치: 너무 많은 메시지가 있으면 중단
            if (offset >= maxOffset) {
                log.warn("메시지가 너무 많아 중단: chatroomId={}, offset={}", chatroomId, offset);
                break;
            }
        }
        
        log.info("메시지 아카이빙 완료: chatroomId={}, totalArchived={}", chatroomId, totalArchived);
    }
    
    /**
     * 채팅방 데이터 조회
     */
    private Map<String, Object> queryChatroomData(UUID chatroomId) {
        String sql = """
            SELECT 
                id, user_id, chatbot_id, name, description,
                settings, context_data, last_message_at, last_message_id,
                is_archived, is_deleted, created_at, updated_at
            FROM chat_schema.chatrooms
            WHERE id = ?
            """;
        
        return jdbcTemplate.queryForMap(sql, chatroomId);
    }
    
    /**
     * 사용자 데이터 조회
     */
    private Map<String, Object> queryUserData(UUID userId) {
        if (userId == null) return null;
        
        String sql = """
            SELECT id, email
            FROM user_schema.app_user
            WHERE id = ?
            """;
        
        try {
            return jdbcTemplate.queryForMap(sql, userId);
        } catch (Exception e) {
            log.warn("사용자 조회 실패: userId={}", userId, e);
            return null;
        }
    }
    
    /**
     * 챗봇 데이터 조회
     */
    private Map<String, Object> queryChatbotData(UUID chatbotId) {
        if (chatbotId == null) return null;
        
        String sql = """
            SELECT id, name, bot_type, intimacy_level
            FROM chat_schema.chatbots
            WHERE id = ?
            """;
        
        try {
            return jdbcTemplate.queryForMap(sql, chatbotId);
        } catch (Exception e) {
            log.warn("챗봇 조회 실패: chatbotId={}", chatbotId, e);
            return null;
        }
    }
    
    /**
     * 친밀도 진행도 조회
     */
    private Map<String, Object> queryIntimacyProgress(UUID chatroomId) {
        String sql = """
            SELECT intimacy_level
            FROM chat_schema.intimacy_progress
            WHERE chatroom_id = ?
            """;
        
        try {
            return jdbcTemplate.queryForMap(sql, chatroomId);
        } catch (Exception e) {
            log.debug("친밀도 진행도 조회 실패 (무시 가능): chatroomId={}", chatroomId);
            return null;
        }
    }
    
    /**
     * 메시지 조회 (페이징)
     */
    private List<Map<String, Object>> queryMessages(UUID chatroomId, int limit, int offset) {
        String sql = """
            SELECT 
                id, parent_message_id, sender_type, sender_id,
                content, content_type, metadata,
                sequence_number, turn_number,
                token_count, processing_time_ms,
                is_edited, edited_at, is_deleted, deleted_at,
                created_at, updated_at
            FROM chat_schema.messages
            WHERE chatroom_id = ?
            ORDER BY sequence_number ASC
            LIMIT ? OFFSET ?
            """;
        
        return jdbcTemplate.queryForList(sql, chatroomId, limit, offset);
    }
    
    /**
     * Store ID 맵 조회 (message_id -> store_id)
     */
    private Map<UUID, UUID> queryStoreIdMap(UUID chatroomId) {
        String sql = """
            SELECT message_id, id
            FROM store_schema.stores
            WHERE message_id IN (
                SELECT id FROM chat_schema.messages WHERE chatroom_id = ?
            ) AND is_deleted = false
            """;
        
        Map<UUID, UUID> map = new HashMap<>();
        jdbcTemplate.query(sql, (rs, rowNum) -> {
            UUID messageId = (UUID) rs.getObject("message_id");
            UUID storeId = (UUID) rs.getObject("id");
            if (messageId != null && storeId != null) {
                map.put(messageId, storeId);
            }
            return null;
        }, chatroomId);
        
        return map;
    }
    
    /**
     * Usage Request ID 맵 조회
     * 
     * 주의: billing.ai_usage_events 테이블에는 message_id 컬럼이 없으므로,
     * 메시지의 metadata.usage.requestId에서 직접 추출합니다.
     * 이 메서드는 더 이상 사용되지 않으며, buildMetadataJson()에서 직접 처리합니다.
     * 
     * @deprecated 메시지 metadata에서 직접 추출하도록 변경됨
     */
    @Deprecated
    private Map<String, String> queryUsageRequestIdMap(UUID chatroomId) {
        // billing.ai_usage_events에는 message_id가 없으므로 빈 맵 반환
        // 실제 usageRequestId는 메시지의 metadata.usage.requestId에서 추출
        return new HashMap<>();
    }
    
    /**
     * ArchChatroom 빌드
     */
    private ArchChatroom buildArchChatroom(
            Map<String, Object> roomData,
            Map<String, Object> userData,
            Map<String, Object> chatbotData,
            Map<String, Object> progressData) {
        
        ArchChatroom.ArchChatroomBuilder builder = ArchChatroom.builder();
        
        // Source ID
        builder.sourceChatroomId((UUID) roomData.get("id"));
        
        // User 스냅샷
        if (userData != null) {
            builder.userId((UUID) userData.get("id"));
            builder.userEmailSnapshot((String) userData.get("email"));
        }
        
        // Chatbot 스냅샷
        if (chatbotData != null) {
            builder.chatbotId((UUID) chatbotData.get("id"));
            builder.chatbotNameSnapshot((String) chatbotData.get("name"));
            builder.chatbotTypeSnapshot((String) chatbotData.get("bot_type"));
            Object intimacyLevel = chatbotData.get("intimacy_level");
            if (intimacyLevel != null) {
                builder.chatbotIntimacyLevelSnapshot(((Number) intimacyLevel).intValue());
            }
        }
        
        // 평면 컬럼
        builder.name((String) roomData.get("name"));
        builder.description((String) roomData.get("description"));
        
        // concept 추출
        String settingsJson = (String) roomData.get("settings");
        String concept = extractConceptFromSettings(settingsJson);
        builder.concept(concept);
        
        // Meta JSONB 구성
        ObjectNode meta = objectMapper.createObjectNode();
        meta.put("concept", concept);
        
        // intimacyLevel
        int intimacyLevel = 1;
        if (progressData != null && progressData.get("intimacy_level") != null) {
            intimacyLevel = ((Number) progressData.get("intimacy_level")).intValue();
        } else if (chatbotData != null && chatbotData.get("intimacy_level") != null) {
            intimacyLevel = ((Number) chatbotData.get("intimacy_level")).intValue();
        }
        meta.put("intimacyLevel", intimacyLevel);
        
        // testModel
        if (settingsJson != null) {
            try {
                JsonNode settings = objectMapper.readTree(settingsJson);
                if (settings.has("testModel")) {
                    meta.put("testModel", settings.get("testModel").asText());
                }
                // settings 전체 저장
                meta.set("settings", settings);
            } catch (Exception e) {
                log.warn("Settings 파싱 실패", e);
                // 파싱 실패 시에도 원본 저장 시도
                try {
                    meta.put("settings", settingsJson);
                } catch (Exception e2) {
                    log.warn("Settings 원본 저장 실패", e2);
                }
            }
        }
        
        // contextData 복사
        String contextDataJson = (String) roomData.get("context_data");
        if (contextDataJson != null) {
            try {
                JsonNode contextData = objectMapper.readTree(contextDataJson);
                meta.set("contextData", contextData);
            } catch (Exception e) {
                log.warn("ContextData 파싱 실패", e);
            }
        }
        
        // source 정보
        ObjectNode source = objectMapper.createObjectNode();
        source.put("env", "prod");
        source.put("service", "chat");
        meta.set("source", source);
        
        builder.meta(meta);
        
        // 기타 필드
        builder.lastMessageAt((LocalDateTime) roomData.get("last_message_at"));
        builder.sourceLastMessageId((UUID) roomData.get("last_message_id"));
        builder.isArchived((Boolean) roomData.get("is_archived"));
        builder.isDeleted((Boolean) roomData.get("is_deleted"));
        builder.sourceCreatedAt((LocalDateTime) roomData.get("created_at"));
        builder.sourceUpdatedAt((LocalDateTime) roomData.get("updated_at"));
        builder.archivedAt(LocalDateTime.now());
        
        return builder.build();
    }
    
    /**
     * Settings에서 concept 추출
     */
    private String extractConceptFromSettings(String settingsJson) {
        if (settingsJson == null || settingsJson.isBlank()) {
            return "FRIEND";
        }
        
        try {
            JsonNode settings = objectMapper.readTree(settingsJson);
            if (settings.has("concept")) {
                JsonNode conceptNode = settings.get("concept");
                if (conceptNode.isTextual()) {
                    return conceptNode.asText().toUpperCase();
                }
            }
        } catch (Exception e) {
            log.warn("Settings에서 concept 추출 실패", e);
        }
        
        return "FRIEND";
    }
    
    /**
     * 메시지 아카이빙
     */
    @Transactional
    public void archiveMessage(
            Map<String, Object> messageData,
            UUID archChatroomId,
            Map<UUID, UUID> storeIdMap,
            Map<String, String> usageRequestIdMap) {
        
        UUID sourceMessageId = (UUID) messageData.get("id");
        
        // 이미 아카이빙된 경우 스킵
        if (archMessageRepository.existsBySourceMessageId(sourceMessageId)) {
            return;
        }
        
        ArchChatroom archChatroom = archChatroomRepository.findById(archChatroomId)
            .orElseThrow(() -> new RuntimeException("ArchChatroom not found: " + archChatroomId));
        
        // ArchMessage 생성
        ArchMessage archMessage = buildArchMessage(messageData, archChatroom, storeIdMap, usageRequestIdMap);
        archMessage = archMessageRepository.save(archMessage);
        
        // Agent 결과 분리 및 저장
        String metadataJson = (String) messageData.get("metadata");
        if (metadataJson != null && !metadataJson.isBlank()) {
            try {
                extractAndSaveAgentResults(archMessage, metadataJson, messageData);
            } catch (Exception e) {
                log.error("Agent 결과 추출 실패: messageId={}", sourceMessageId, e);
            }
        }
    }
    
    /**
     * ArchMessage 빌드
     */
    private ArchMessage buildArchMessage(
            Map<String, Object> messageData,
            ArchChatroom archChatroom,
            Map<UUID, UUID> storeIdMap,
            Map<String, String> usageRequestIdMap) {
        
        ArchMessage.ArchMessageBuilder builder = ArchMessage.builder();
        
        builder.archChatroom(archChatroom);
        builder.sourceMessageId((UUID) messageData.get("id"));
        builder.sourceParentMessageId((UUID) messageData.get("parent_message_id"));
        builder.senderType((String) messageData.get("sender_type"));
        builder.senderId((UUID) messageData.get("sender_id"));
        builder.content((String) messageData.get("content"));
        builder.contentType((String) messageData.get("content_type"));
        
        // sequence_number, turn_number (운영과 동일)
        Object seqNum = messageData.get("sequence_number");
        if (seqNum != null) {
            builder.sequenceNumber(((Number) seqNum).longValue());
        }
        Object turnNum = messageData.get("turn_number");
        if (turnNum != null) {
            builder.turnNumber(((Number) turnNum).longValue());
        }
        
        builder.tokenCount((Integer) messageData.get("token_count"));
        builder.processingTimeMs((Integer) messageData.get("processing_time_ms"));
        builder.isEdited((Boolean) messageData.get("is_edited"));
        builder.editedAt((LocalDateTime) messageData.get("edited_at"));
        builder.isDeleted((Boolean) messageData.get("is_deleted"));
        builder.deletedAt((LocalDateTime) messageData.get("deleted_at"));
        builder.sourceCreatedAt((LocalDateTime) messageData.get("created_at"));
        builder.sourceUpdatedAt((LocalDateTime) messageData.get("updated_at"));
        builder.archivedAt(LocalDateTime.now());
        
        // metadata_json 구성
        String metadataJson = (String) messageData.get("metadata");
        ObjectNode metadataJsonNode = buildMetadataJson(metadataJson, (UUID) messageData.get("id"), storeIdMap, usageRequestIdMap);
        builder.metadataJson(metadataJsonNode);
        
        return builder.build();
    }
    
    /**
     * metadata_json 구성
     */
    private ObjectNode buildMetadataJson(
            String metadataJson,
            UUID messageId,
            Map<UUID, UUID> storeIdMap,
            Map<String, String> usageRequestIdMap) {
        
        ObjectNode metadataJsonNode = objectMapper.createObjectNode();
        
        // analysis
        ObjectNode analysis = objectMapper.createObjectNode();
        analysis.put("language", "ko");
        ObjectNode safety = objectMapper.createObjectNode();
        safety.put("flag", false);
        safety.putNull("reason");
        analysis.set("safety", safety);
        metadataJsonNode.set("analysis", analysis);
        
        // link
        ObjectNode link = objectMapper.createObjectNode();
        
        // storeId
        UUID storeId = storeIdMap.get(messageId);
        if (storeId != null) {
            link.put("storeId", storeId.toString());
        }
        
        // usageRequestId: 메시지의 metadata.usage.requestId에서 추출
        // billing.ai_usage_events에는 message_id가 없으므로 metadata에서 직접 추출
        String usageRequestId = null;
        if (metadataJson != null && !metadataJson.isBlank()) {
            try {
                JsonNode originalMetadata = objectMapper.readTree(metadataJson);
                if (originalMetadata.has("usage")) {
                    JsonNode usage = originalMetadata.get("usage");
                    if (usage.has("requestId")) {
                        usageRequestId = usage.get("requestId").asText();
                    }
                }
            } catch (Exception e) {
                log.debug("Usage requestId 추출 실패 (무시 가능): messageId={}", messageId, e);
            }
        }
        
        // usageRequestIdMap에서도 확인 (하위 호환성)
        if (usageRequestId == null) {
            usageRequestId = usageRequestIdMap.get(messageId.toString());
        }
        
        if (usageRequestId != null) {
            link.put("usageRequestId", usageRequestId);
        }
        
        // agentResults는 나중에 추가 (Agent 결과 저장 후)
        metadataJsonNode.set("link", link);
        
        // originalMetadata 백업
        if (metadataJson != null && !metadataJson.isBlank()) {
            try {
                JsonNode originalMetadata = objectMapper.readTree(metadataJson);
                metadataJsonNode.set("originalMetadata", originalMetadata);
            } catch (Exception e) {
                log.warn("OriginalMetadata 파싱 실패", e);
            }
        }
        
        return metadataJsonNode;
    }
    
    /**
     * Agent 결과 추출 및 저장
     * 부분 파싱 및 트랜잭션 처리 강화
     */
    private void extractAndSaveAgentResults(
            ArchMessage archMessage,
            String metadataJson,
            Map<String, Object> messageData) {
        
        if (metadataJson == null || metadataJson.isBlank()) {
            log.debug("metadata가 없어 Agent 결과 추출 불가: messageId={}", archMessage.getSourceMessageId());
            return;
        }
        
        JsonNode metadata = null;
        try {
            metadata = objectMapper.readTree(metadataJson);
        } catch (Exception e) {
            log.error("Metadata JSON 파싱 실패: messageId={}, 원본 저장", archMessage.getSourceMessageId(), e);
            // 파싱 실패 시에도 originalMetadata에 원본 저장
            try {
                ObjectNode metadataJsonNode = (ObjectNode) archMessage.getMetadataJson();
                metadataJsonNode.put("originalMetadata", metadataJson);
                archMessage.setMetadataJson(metadataJsonNode);
                archMessageRepository.save(archMessage);
            } catch (Exception e2) {
                log.error("OriginalMetadata 저장 실패: messageId={}", archMessage.getSourceMessageId(), e2);
            }
            return;
        }
        
        Map<String, UUID> agentResultIds = new HashMap<>();
        
        try {
            // intimacy agent 결과 추출 (부분 파싱)
            if (metadata.has("userMessageAnalysis")) {
                JsonNode userAnalysis = metadata.get("userMessageAnalysis");
                if (userAnalysis != null && userAnalysis.has("intimacy")) {
                    try {
                        JsonNode intimacy = userAnalysis.get("intimacy");
                        UUID intimacyId = createAgentResult(archMessage, "intimacy", intimacy, messageData, metadata);
                        agentResultIds.put("intimacy", intimacyId);
                    } catch (Exception e) {
                        log.warn("Intimacy Agent 결과 추출 실패: messageId={}", archMessage.getSourceMessageId(), e);
                    }
                }
            }
            
            // vocabulary agent 결과 추출 (부분 파싱)
            if (metadata.has("botResponseAnalysis")) {
                JsonNode botAnalysis = metadata.get("botResponseAnalysis");
                if (botAnalysis != null && botAnalysis.has("vocabulary")) {
                    try {
                        JsonNode vocab = botAnalysis.get("vocabulary");
                        if (vocab.has("words") && vocab.get("words").isArray()) {
                            UUID vocabId = createVocabularyAgentResult(archMessage, vocab, messageData, metadata);
                            agentResultIds.put("voca", vocabId);
                        }
                    } catch (Exception e) {
                        log.warn("Vocabulary Agent 결과 추출 실패: messageId={}", archMessage.getSourceMessageId(), e);
                    }
                }
            }
            
            // metadata_json의 link.agentResults 업데이트 (트랜잭션 내에서 원자적 처리)
            if (!agentResultIds.isEmpty()) {
                ObjectNode metadataJsonNode = (ObjectNode) archMessage.getMetadataJson();
                ObjectNode link = (ObjectNode) metadataJsonNode.get("link");
                if (link == null) {
                    link = objectMapper.createObjectNode();
                    metadataJsonNode.set("link", link);
                }
                
                ObjectNode agentResults = objectMapper.createObjectNode();
                agentResultIds.forEach((type, id) -> agentResults.put(type, id.toString()));
                link.set("agentResults", agentResults);
                
                archMessage.setMetadataJson(metadataJsonNode);
                archMessageRepository.save(archMessage);
            }
            
        } catch (Exception e) {
            log.error("Agent 결과 처리 중 오류: messageId={}", archMessage.getSourceMessageId(), e);
            // 에러가 발생해도 원본 metadata는 저장되어 있으므로 계속 진행
        }
    }
    
    /**
     * Intimacy agent 결과 생성
     */
    private UUID createAgentResult(
            ArchMessage archMessage,
            String agentType,
            JsonNode payload,
            Map<String, Object> sourceMessage,
            JsonNode originalMetadata) {
        
        ArchAgentResult result = ArchAgentResult.builder()
            .archMessage(archMessage)
            .agentType(agentType)
            .payloadJson(payload)
            .sourceCreatedAt((LocalDateTime) sourceMessage.get("created_at"))
            .archivedAt(LocalDateTime.now())
            .build();
        
        // usage 정보에서 추출
        if (originalMetadata.has("usage")) {
            JsonNode usage = originalMetadata.get("usage");
            if (usage.has("requestId")) {
                result.setRequestId(usage.get("requestId").asText());
            }
            if (usage.has("provider")) {
                result.setProvider(usage.get("provider").asText());
            }
            if (usage.has("model")) {
                result.setModel(usage.get("model").asText());
            }
            if (usage.has("inputTokens")) {
                result.setInputTokens(usage.get("inputTokens").asInt());
            }
            if (usage.has("outputTokens")) {
                result.setOutputTokens(usage.get("outputTokens").asInt());
            }
        }
        
        result = archAgentResultRepository.save(result);
        return result.getId();
    }
    
    /**
     * Vocabulary agent 결과 생성 (여러 단어는 배열로 저장)
     */
    private UUID createVocabularyAgentResult(
            ArchMessage archMessage,
            JsonNode vocabNode,
            Map<String, Object> sourceMessage,
            JsonNode originalMetadata) {
        
        // words 배열을 그대로 payload로 저장
        ObjectNode payload = objectMapper.createObjectNode();
        if (vocabNode.has("words")) {
            payload.set("words", vocabNode.get("words"));
        }
        
        ArchAgentResult result = ArchAgentResult.builder()
            .archMessage(archMessage)
            .agentType("voca")
            .payloadJson(payload)
            .sourceCreatedAt((LocalDateTime) sourceMessage.get("created_at"))
            .archivedAt(LocalDateTime.now())
            .build();
        
        result = archAgentResultRepository.save(result);
        return result.getId();
    }
    
    /**
     * 아카이빙 대상 채팅방 조회
     * 
     * 모든 데이터를 이관하기 위해 날짜 필터를 제거하고,
     * is_archived=true 또는 is_deleted=true인 채팅방을 대상으로 합니다.
     * 
     * @param daysOld 사용하지 않음 (하위 호환성을 위해 유지)
     * @param limit 최대 조회 개수
     * @return 아카이빙 대상 채팅방 ID 목록
     */
    public List<UUID> findChatroomsToArchive(int daysOld, int limit) {
        // 모든 데이터 이관을 위해 날짜 필터 제거
        // is_archived=true 또는 is_deleted=true인 모든 채팅방을 대상으로 함
        String sql = """
            SELECT id
            FROM chat_schema.chatrooms
            WHERE (is_archived = true OR is_deleted = true)
              AND id NOT IN (
                  SELECT source_chatroom_id FROM archive_schema.arch_chatrooms
              )
            ORDER BY last_message_at ASC NULLS LAST, created_at ASC
            LIMIT ?
            """;
        
        return jdbcTemplate.queryForList(sql, UUID.class, limit);
    }
    
    /**
     * Store 데이터 아카이빙
     */
    @Transactional
    public void archiveStores(UUID chatroomId, UUID archChatroomId) {
        log.info("Store 아카이빙 시작: chatroomId={}, archChatroomId={}", chatroomId, archChatroomId);
        
        String sql = """
            SELECT id, message_id, user_id, content, corrected_content, 
                   ai_response, bot_type, is_deleted, deleted_at, created_at, updated_at
            FROM store_schema.stores
            WHERE chatroom_id = ? AND is_deleted = false
            """;
        
        List<Map<String, Object>> stores = jdbcTemplate.queryForList(sql, chatroomId);
        
        ArchChatroom archChatroom = archChatroomRepository.findById(archChatroomId)
            .orElseThrow(() -> new RuntimeException("ArchChatroom not found: " + archChatroomId));
        
        int archivedCount = 0;
        for (Map<String, Object> storeData : stores) {
            UUID sourceStoreId = (UUID) storeData.get("id");
            
            // 이미 아카이빙된 경우 스킵
            if (archStoreRepository.existsBySourceStoreId(sourceStoreId)) {
                continue;
            }
            
            try {
                ArchStore archStore = buildArchStore(storeData, archChatroom);
                archStoreRepository.save(archStore);
                archivedCount++;
            } catch (Exception e) {
                log.error("Store 아카이빙 실패: storeId={}", sourceStoreId, e);
            }
        }
        
        log.info("Store 아카이빙 완료: chatroomId={}, archivedCount={}", chatroomId, archivedCount);
    }
    
    /**
     * ArchStore 빌드
     */
    private ArchStore buildArchStore(Map<String, Object> storeData, ArchChatroom archChatroom) {
        String aiResponseJson = (String) storeData.get("ai_response");
        JsonNode aiResponse = null;
        if (aiResponseJson != null && !aiResponseJson.isBlank()) {
            try {
                aiResponse = objectMapper.readTree(aiResponseJson);
            } catch (Exception e) {
                log.warn("AI Response 파싱 실패: storeId={}", storeData.get("id"), e);
            }
        }
        
        return ArchStore.builder()
            .archChatroom(archChatroom)
            .sourceStoreId((UUID) storeData.get("id"))
            .sourceMessageId((UUID) storeData.get("message_id"))
            .userId((UUID) storeData.get("user_id"))
            .content((String) storeData.get("content"))
            .correctedContent((String) storeData.get("corrected_content"))
            .aiResponse(aiResponse != null ? aiResponse : objectMapper.createObjectNode())
            .botType((String) storeData.get("bot_type"))
            .isDeleted((Boolean) storeData.get("is_deleted"))
            .deletedAt((LocalDateTime) storeData.get("deleted_at"))
            .sourceCreatedAt((LocalDateTime) storeData.get("created_at"))
            .sourceUpdatedAt((LocalDateTime) storeData.get("updated_at"))
            .archivedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * Usage 이벤트 아카이빙
     */
    @Transactional
    public void archiveUsageEvents(UUID chatroomId, UUID archChatroomId) {
        log.info("Usage 이벤트 아카이빙 시작: chatroomId={}, archChatroomId={}", chatroomId, archChatroomId);
        
        String sql = """
            SELECT id, event_time, user_id, provider, model, request_id,
                   input_tokens, output_tokens, cost_in, cost_out, meta
            FROM billing.ai_usage_events
            WHERE chatroom_id = ?
            ORDER BY event_time ASC
            """;
        
        List<Map<String, Object>> usageEvents = jdbcTemplate.queryForList(sql, chatroomId);
        
        ArchChatroom archChatroom = archChatroomRepository.findById(archChatroomId)
            .orElseThrow(() -> new RuntimeException("ArchChatroom not found: " + archChatroomId));
        
        int archivedCount = 0;
        for (Map<String, Object> eventData : usageEvents) {
            UUID sourceUsageEventId = (UUID) eventData.get("id");
            
            // 이미 아카이빙된 경우 스킵
            if (archUsageEventRepository.existsBySourceUsageEventId(sourceUsageEventId)) {
                continue;
            }
            
            try {
                ArchUsageEvent archUsageEvent = buildArchUsageEvent(eventData, archChatroom);
                archUsageEventRepository.save(archUsageEvent);
                archivedCount++;
            } catch (Exception e) {
                log.error("Usage 이벤트 아카이빙 실패: usageEventId={}", sourceUsageEventId, e);
            }
        }
        
        log.info("Usage 이벤트 아카이빙 완료: chatroomId={}, archivedCount={}", chatroomId, archivedCount);
    }
    
    /**
     * ArchUsageEvent 빌드
     */
    private ArchUsageEvent buildArchUsageEvent(Map<String, Object> eventData, ArchChatroom archChatroom) {
        String metaJson = (String) eventData.get("meta");
        JsonNode meta = null;
        if (metaJson != null && !metaJson.isBlank()) {
            try {
                meta = objectMapper.readTree(metaJson);
            } catch (Exception e) {
                log.debug("Meta 파싱 실패 (무시 가능): usageEventId={}", eventData.get("id"), e);
            }
        }
        
        // OffsetDateTime을 LocalDateTime으로 변환
        Object eventTimeObj = eventData.get("event_time");
        LocalDateTime eventTime = null;
        if (eventTimeObj != null) {
            if (eventTimeObj instanceof java.time.OffsetDateTime) {
                eventTime = ((java.time.OffsetDateTime) eventTimeObj).toLocalDateTime();
            } else if (eventTimeObj instanceof LocalDateTime) {
                eventTime = (LocalDateTime) eventTimeObj;
            } else if (eventTimeObj instanceof java.sql.Timestamp) {
                eventTime = ((java.sql.Timestamp) eventTimeObj).toLocalDateTime();
            }
        }
        
        // BigDecimal 변환
        java.math.BigDecimal costIn = null;
        java.math.BigDecimal costOut = null;
        Object costInObj = eventData.get("cost_in");
        Object costOutObj = eventData.get("cost_out");
        if (costInObj != null) {
            if (costInObj instanceof java.math.BigDecimal) {
                costIn = (java.math.BigDecimal) costInObj;
            } else if (costInObj instanceof Number) {
                costIn = java.math.BigDecimal.valueOf(((Number) costInObj).doubleValue());
            }
        }
        if (costOutObj != null) {
            if (costOutObj instanceof java.math.BigDecimal) {
                costOut = (java.math.BigDecimal) costOutObj;
            } else if (costOutObj instanceof Number) {
                costOut = java.math.BigDecimal.valueOf(((Number) costOutObj).doubleValue());
            }
        }
        
        return ArchUsageEvent.builder()
            .archChatroom(archChatroom)
            .sourceUsageEventId((UUID) eventData.get("id"))
            .userId((UUID) eventData.get("user_id"))
            .eventTime(eventTime)
            .provider((String) eventData.get("provider"))
            .model((String) eventData.get("model"))
            .requestId((String) eventData.get("request_id"))
            .inputTokens((Integer) eventData.get("input_tokens"))
            .outputTokens((Integer) eventData.get("output_tokens"))
            .costIn(costIn != null ? costIn : java.math.BigDecimal.ZERO)
            .costOut(costOut != null ? costOut : java.math.BigDecimal.ZERO)
            .meta(meta)
            .archivedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * Intimacy Progress 아카이빙
     */
    @Transactional
    public void archiveIntimacyProgress(UUID chatroomId, UUID archChatroomId) {
        log.info("Intimacy Progress 아카이빙 시작: chatroomId={}, archChatroomId={}", chatroomId, archChatroomId);
        
        String sql = """
            SELECT id, user_id, intimacy_level, total_corrections, 
                   last_feedback, last_updated, progress_data
            FROM chat_schema.intimacy_progress
            WHERE chatroom_id = ?
            """;
        
        try {
            Map<String, Object> progressData = jdbcTemplate.queryForMap(sql, chatroomId);
            
            UUID sourceIntimacyProgressId = (UUID) progressData.get("id");
            
            // 이미 아카이빙된 경우 스킵
            if (archIntimacyProgressRepository.existsBySourceIntimacyProgressId(sourceIntimacyProgressId)) {
                log.info("이미 아카이빙됨: intimacyProgressId={}", sourceIntimacyProgressId);
                return;
            }
            
            ArchChatroom archChatroom = archChatroomRepository.findById(archChatroomId)
                .orElseThrow(() -> new RuntimeException("ArchChatroom not found: " + archChatroomId));
            
            ArchIntimacyProgress archIntimacyProgress = buildArchIntimacyProgress(progressData, archChatroom);
            archIntimacyProgressRepository.save(archIntimacyProgress);
            
            log.info("Intimacy Progress 아카이빙 완료: chatroomId={}", chatroomId);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            log.debug("Intimacy Progress 없음 (무시 가능): chatroomId={}", chatroomId);
        } catch (Exception e) {
            log.error("Intimacy Progress 아카이빙 실패: chatroomId={}", chatroomId, e);
        }
    }
    
    /**
     * ArchIntimacyProgress 빌드
     */
    private ArchIntimacyProgress buildArchIntimacyProgress(Map<String, Object> progressData, ArchChatroom archChatroom) {
        String progressDataJson = (String) progressData.get("progress_data");
        JsonNode progressDataNode = null;
        if (progressDataJson != null && !progressDataJson.isBlank()) {
            try {
                progressDataNode = objectMapper.readTree(progressDataJson);
            } catch (Exception e) {
                log.warn("Progress Data 파싱 실패: intimacyProgressId={}", progressData.get("id"), e);
            }
        }
        
        return ArchIntimacyProgress.builder()
            .archChatroom(archChatroom)
            .sourceIntimacyProgressId((UUID) progressData.get("id"))
            .userId((UUID) progressData.get("user_id"))
            .intimacyLevel((Integer) progressData.get("intimacy_level"))
            .totalCorrections((Integer) progressData.get("total_corrections"))
            .lastFeedback((String) progressData.get("last_feedback"))
            .lastUpdated((LocalDateTime) progressData.get("last_updated"))
            .progressData(progressDataNode)
            .archivedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * 진행 상태 업데이트
     */
    @Transactional
    public void updateIngestionState(
            String jobName,
            UUID lastChatroomId,
            UUID lastMessageId,
            LocalDateTime lastMessageCreatedAt,
            String status,
            String note) {
        
        jdbcTemplate.update(
            "SELECT archive_schema.update_ingestion_state(?, ?, ?, ?, ?, ?)",
            jobName, lastChatroomId, lastMessageId, lastMessageCreatedAt, status, note
        );
    }
}

