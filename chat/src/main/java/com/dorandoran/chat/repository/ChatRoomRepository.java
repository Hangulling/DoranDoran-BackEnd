package com.dorandoran.chat.repository;

import com.dorandoran.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {
    // 사용자 ID로 채팅방 목록 찾기
    List<ChatRoom> findByUserId(UUID userId);
    
    // 사용자 ID와 삭제되지 않은 채팅방 목록 찾기
    List<ChatRoom> findByUserIdAndIsDeletedFalse(UUID userId);
}
