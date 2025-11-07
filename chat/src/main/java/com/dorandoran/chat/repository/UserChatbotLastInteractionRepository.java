package com.dorandoran.chat.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface UserChatbotLastInteractionRepository extends Repository<Object, UUID> {

    // upsert: userId, chatbotId 기준으로 last_interaction_at / last_room_id 갱신
    @Query(value = """
        INSERT INTO chat_schema.user_chatbot_last_interaction
            (user_id, chatbot_id, last_interaction_at, last_room_id)
        VALUES (:userId, :chatbotId, :ts, :roomId)
        ON CONFLICT (user_id, chatbot_id)
        DO UPDATE SET
            last_interaction_at = GREATEST(EXCLUDED.last_interaction_at, chat_schema.user_chatbot_last_interaction.last_interaction_at),
            last_room_id = CASE
                WHEN EXCLUDED.last_interaction_at IS NOT NULL AND (
                    chat_schema.user_chatbot_last_interaction.last_interaction_at IS NULL OR
                    EXCLUDED.last_interaction_at >= chat_schema.user_chatbot_last_interaction.last_interaction_at
                ) THEN EXCLUDED.last_room_id
                ELSE chat_schema.user_chatbot_last_interaction.last_room_id
            END
        """
        , nativeQuery = true)
    void upsert(@Param("userId") UUID userId,
                @Param("chatbotId") UUID chatbotId,
                @Param("ts") OffsetDateTime lastInteractionAt,
                @Param("roomId") UUID lastRoomId);

    // 상위 N 조회: 사용자 기준 최신 상호작용 순
    @Query(value = """
        SELECT chatbot_id, last_interaction_at, last_room_id
        FROM chat_schema.user_chatbot_last_interaction
        WHERE user_id = :userId
        ORDER BY last_interaction_at DESC NULLS LAST
        LIMIT :limit
        """
        , nativeQuery = true)
    List<Object[]> findTopByUserOrder(@Param("userId") UUID userId, @Param("limit") int limit);
}




