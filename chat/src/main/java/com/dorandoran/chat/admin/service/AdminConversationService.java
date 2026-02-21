package com.dorandoran.chat.admin.service;

import com.dorandoran.chat.admin.dto.AdminConversationDetailResponse;
import com.dorandoran.chat.admin.dto.AdminConversationListItem;
import com.dorandoran.chat.admin.dto.AdminConversationListResponse;
import com.dorandoran.chat.admin.dto.AdminConversationMessageResponse;
import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.entity.Message;
import com.dorandoran.chat.repository.ChatRoomRepository;
import com.dorandoran.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 관리자 대화 조회 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminConversationService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Transactional(readOnly = true)
    public AdminConversationListResponse getConversations(
            UUID userId,
            String userEmail,
            LocalDateTime from,
            LocalDateTime to,
            String roomKey,
            Integer intimacyLevel,
            String dataSource,
            int page,
            int size
    ) {
        if ("archive".equalsIgnoreCase(dataSource)) {
            return getArchiveConversations(userId, userEmail, from, to, roomKey, intimacyLevel, page, size);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<ChatRoom> roomPage = chatRoomRepository.findAdminConversations(
            userId, from, to, roomKey, intimacyLevel, pageable
        );

        List<AdminConversationListItem> content = roomPage.getContent().stream()
            .map(room -> AdminConversationListItem.builder()
                .conversationId(room.getId())
                .userId(room.getUser() != null ? room.getUser().getId() : null)
                .roomKey(extractRoomKey(room))
                .intimacyLevel(room.getChatbot() != null ? room.getChatbot().getIntimacyLevel() : null)
                .lastMessageAt(room.getLastMessageAt())
                .lastSequenceNumber(room.getLastMessage() != null ? room.getLastMessage().getSequenceNumber() : null)
                .build())
            .collect(Collectors.toList());

        return AdminConversationListResponse.builder()
            .content(content)
            .page(AdminConversationListResponse.PageInfo.builder()
                .number(roomPage.getNumber())
                .size(roomPage.getSize())
                .totalPages(roomPage.getTotalPages())
                .totalElements(roomPage.getTotalElements())
                .build())
            .build();
    }

    @Transactional(readOnly = true)
    public AdminConversationListResponse getConversations(
            UUID userId,
            LocalDateTime from,
            LocalDateTime to,
            String roomKey,
            Integer intimacyLevel,
            int page,
            int size
    ) {
        return getConversations(userId, null, from, to, roomKey, intimacyLevel, "chat", page, size);
    }

    @Transactional(readOnly = true)
    public AdminConversationDetailResponse getConversationDetail(UUID conversationId, String dataSource) {
        if ("archive".equalsIgnoreCase(dataSource)) {
            return getArchiveConversationDetail(conversationId);
        }

        List<Message> messages = messageRepository.findByChatRoomIdOrderBySequenceNumberAsc(conversationId);

        List<AdminConversationMessageResponse> timeline = messages.stream()
            .map(message -> AdminConversationMessageResponse.builder()
                .messageId(message.getId())
                .senderType(message.getSenderType())
                .content(message.getContent())
                .contentType(message.getContentType())
                .metadata(message.getMetadata())
                .sequenceNumber(message.getSequenceNumber())
                .turnNumber(message.getTurnNumber())
                .createdAt(message.getCreatedAt())
                .build())
            .collect(Collectors.toList());

        return AdminConversationDetailResponse.builder()
            .conversationId(conversationId)
            .timeline(timeline)
            .build();
    }

    @Transactional(readOnly = true)
    public AdminConversationDetailResponse getConversationDetail(UUID conversationId) {
        return getConversationDetail(conversationId, "chat");
    }

    private String extractRoomKey(ChatRoom room) {
        if (room.getSettings() == null || !room.getSettings().has("concept")) {
            return null;
        }
        return room.getSettings().get("concept").asText(null);
    }

    private AdminConversationListResponse getArchiveConversations(
            UUID userId,
            String userEmail,
            LocalDateTime from,
            LocalDateTime to,
            String roomKey,
            Integer intimacyLevel,
            int page,
            int size
    ) {
        int offset = page * size;

        String baseWhere = "WHERE cr.is_deleted = false " +
            "AND (:userId IS NULL OR cr.user_id = :userId) " +
            "AND (:userEmail IS NULL OR cr.user_email_snapshot ILIKE '%' || :userEmail || '%') " +
            "AND (:from IS NULL OR cr.last_message_at >= :from) " +
            "AND (:to IS NULL OR cr.last_message_at <= :to) " +
            "AND (:roomKey IS NULL OR cr.concept = :roomKey) " +
            "AND (:intimacyLevel IS NULL OR cr.chatbot_intimacy_level_snapshot = :intimacyLevel)";

        String listSql = "SELECT cr.id AS conversation_id, cr.user_id, cr.concept AS room_key, " +
            "cr.chatbot_intimacy_level_snapshot AS intimacy_level, cr.last_message_at, " +
            "(SELECT am.sequence_number FROM archive_schema.arch_messages am " +
            " WHERE am.arch_chatroom_id = cr.id ORDER BY am.sequence_number DESC LIMIT 1) AS last_sequence_number " +
            "FROM archive_schema.arch_chatrooms cr " +
            baseWhere +
            " ORDER BY cr.last_message_at DESC NULLS LAST " +
            "LIMIT :size OFFSET :offset";

        String countSql = "SELECT COUNT(*) FROM archive_schema.arch_chatrooms cr " + baseWhere;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("userEmail", (userEmail != null && !userEmail.isBlank()) ? userEmail : null)
            .addValue("from", from)
            .addValue("to", to)
            .addValue("roomKey", (roomKey != null && !roomKey.isBlank()) ? roomKey : null)
            .addValue("intimacyLevel", intimacyLevel)
            .addValue("size", size)
            .addValue("offset", offset);

        List<AdminConversationListItem> content = namedParameterJdbcTemplate.query(
            listSql,
            params,
            (rs, rowNum) -> AdminConversationListItem.builder()
                .conversationId(UUID.fromString(rs.getString("conversation_id")))
                .userId(rs.getString("user_id") != null ? UUID.fromString(rs.getString("user_id")) : null)
                .roomKey(rs.getString("room_key"))
                .intimacyLevel((Integer) rs.getObject("intimacy_level"))
                .lastMessageAt(rs.getTimestamp("last_message_at") != null ? rs.getTimestamp("last_message_at").toLocalDateTime() : null)
                .lastSequenceNumber(rs.getObject("last_sequence_number") != null ? rs.getLong("last_sequence_number") : null)
                .build()
        );

        Long totalElements = namedParameterJdbcTemplate.queryForObject(countSql, params, Long.class);
        int totalPages = totalElements == null ? 0 : (int) Math.ceil(totalElements / (double) size);

        return AdminConversationListResponse.builder()
            .content(content)
            .page(AdminConversationListResponse.PageInfo.builder()
                .number(page)
                .size(size)
                .totalPages(totalPages)
                .totalElements(totalElements == null ? 0 : totalElements)
                .build())
            .build();
    }

    private AdminConversationDetailResponse getArchiveConversationDetail(UUID conversationId) {
        String sql = "SELECT source_message_id, sender_type, content, content_type, " +
            "metadata_json::text AS metadata, sequence_number, turn_number, source_created_at " +
            "FROM archive_schema.arch_messages " +
            "WHERE arch_chatroom_id = :conversationId " +
            "ORDER BY sequence_number ASC";

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("conversationId", conversationId);

        List<AdminConversationMessageResponse> timeline = namedParameterJdbcTemplate.query(
            sql,
            params,
            (rs, rowNum) -> AdminConversationMessageResponse.builder()
                .messageId(rs.getString("source_message_id") != null
                    ? UUID.fromString(rs.getString("source_message_id"))
                    : null)
                .senderType(rs.getString("sender_type"))
                .content(rs.getString("content"))
                .contentType(rs.getString("content_type"))
                .metadata(rs.getString("metadata"))
                .sequenceNumber(rs.getLong("sequence_number"))
                .turnNumber(rs.getLong("turn_number"))
                .createdAt(rs.getTimestamp("source_created_at") != null
                    ? rs.getTimestamp("source_created_at").toLocalDateTime()
                    : null)
                .build()
        );

        return AdminConversationDetailResponse.builder()
            .conversationId(conversationId)
            .timeline(timeline)
            .build();
    }
}
