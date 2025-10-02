package com.dorandoran.chat.repository;

import com.dorandoran.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    // 채팅방 ID로 메시지 목록 찾기
    List<Message> findByRoomId(UUID roomId);
    
    // 채팅방 ID와 삭제되지 않은 메시지 목록 찾기
    List<Message> findByRoomIdAndIsDeletedFalse(UUID roomId);
}
