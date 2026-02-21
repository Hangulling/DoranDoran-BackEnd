package com.dorandoran.chat.repository;

import com.dorandoran.chat.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {
    // 사용자 ID로 채팅방 목록 찾기
    List<ChatRoom> findByUser_Id(UUID userId);

    // 사용자와 챗봇의 1:1 룸 단건 조회 (삭제되지 않은)
    Optional<ChatRoom> findByUser_IdAndChatbot_IdAndIsDeletedFalse(UUID userId, UUID chatbotId);

    // 사용자와 챗봇의 1:1 룸 단건 조회 (삭제 여부 무관)
    Optional<ChatRoom> findByUser_IdAndChatbot_Id(UUID userId, UUID chatbotId);

    // 사용자 ID와 삭제되지 않은 채팅방 목록 찾기
    List<ChatRoom> findByUser_IdAndIsDeletedFalseOrderByLastMessageAtDesc(UUID userId);
    
    // 사용자 ID와 삭제되지 않은 채팅방 목록 찾기 (페이징)
    Page<ChatRoom> findByUser_IdAndIsDeletedFalseOrderByLastMessageAtDesc(UUID userId, Pageable pageable);
    
    // 사용자가 특정 채팅방에 접근 권한이 있는지 확인
    boolean existsByUser_IdAndIdAndIsDeletedFalse(UUID userId, UUID chatroomId);

    // 사용자가 특정 채팅방에 접근 권한이 있는지 확인 - 보관함 사용
    boolean existsByUserIdAndIdAndIsDeletedFalse(UUID userId, UUID chatroomId);
    
    // 사용자 ID와 삭제되지 않은 채팅방 목록 찾기 (테스트 모델 필터 포함, 페이징)
    @Query(value = "SELECT * FROM chat_schema.chatrooms cr " +
           "WHERE cr.user_id = :userId::uuid AND cr.is_deleted = false " +
           "AND (cr.settings->>'testModel') = :testModel " +
           "ORDER BY cr.last_message_at DESC NULLS LAST",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM chat_schema.chatrooms cr " +
                       "WHERE cr.user_id = :userId::uuid AND cr.is_deleted = false " +
                       "AND (cr.settings->>'testModel') = :testModel")
    Page<ChatRoom> findByUser_IdAndIsDeletedFalseAndTestModelOrderByLastMessageAtDesc(
        @Param("userId") UUID userId, 
        @Param("testModel") String testModel, 
        Pageable pageable
    );

    @Query(value = "SELECT cr.* FROM chat_schema.chatrooms cr " +
           "JOIN chat_schema.chatbots cb ON cr.chatbot_id = cb.id " +
           "WHERE cr.is_deleted = false " +
           "AND (:userId IS NULL OR cr.user_id = :userId::uuid) " +
           "AND (:from IS NULL OR cr.last_message_at >= :from) " +
           "AND (:to IS NULL OR cr.last_message_at <= :to) " +
           "AND (:roomKey IS NULL OR (cr.settings->>'concept') = :roomKey) " +
           "AND (:intimacyLevel IS NULL OR cb.intimacy_level = :intimacyLevel) " +
           "ORDER BY cr.last_message_at DESC NULLS LAST",
           countQuery = "SELECT COUNT(*) FROM chat_schema.chatrooms cr " +
                       "JOIN chat_schema.chatbots cb ON cr.chatbot_id = cb.id " +
                       "WHERE cr.is_deleted = false " +
                       "AND (:userId IS NULL OR cr.user_id = :userId::uuid) " +
                       "AND (:from IS NULL OR cr.last_message_at >= :from) " +
                       "AND (:to IS NULL OR cr.last_message_at <= :to) " +
                       "AND (:roomKey IS NULL OR (cr.settings->>'concept') = :roomKey) " +
                       "AND (:intimacyLevel IS NULL OR cb.intimacy_level = :intimacyLevel)",
           nativeQuery = true)
    Page<ChatRoom> findAdminConversations(
        @Param("userId") UUID userId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        @Param("roomKey") String roomKey,
        @Param("intimacyLevel") Integer intimacyLevel,
        Pageable pageable
    );
}
