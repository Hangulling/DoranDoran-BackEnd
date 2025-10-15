package com.dorandoran.chat.repository;

import com.dorandoran.chat.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {
    // 사용자 ID로 채팅방 목록 찾기
    List<ChatRoom> findByUserId(UUID userId);

    // 사용자와 챗봇의 1:1 룸 단건 조회 (삭제되지 않은)
    Optional<ChatRoom> findByUserIdAndChatbotIdAndIsDeletedFalse(UUID userId, UUID chatbotId);

    // 사용자와 챗봇의 1:1 룸 단건 조회 (삭제 여부 무관)
    Optional<ChatRoom> findByUserIdAndChatbotId(UUID userId, UUID chatbotId);

    // 사용자 ID와 삭제되지 않은 채팅방 목록 찾기
    List<ChatRoom> findByUserIdAndIsDeletedFalseOrderByLastMessageAtDesc(UUID userId);
    
    // 사용자 ID와 삭제되지 않은 채팅방 목록 찾기 (페이징)
    Page<ChatRoom> findByUserIdAndIsDeletedFalseOrderByLastMessageAtDesc(UUID userId, Pageable pageable);
    
    // 사용자가 특정 채팅방에 접근 권한이 있는지 확인
    boolean existsByUserIdAndIdAndIsDeletedFalse(UUID userId, UUID chatroomId);
}
