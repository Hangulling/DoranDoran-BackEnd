package com.dorandoran.chat.repository;

import com.dorandoran.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    // 채팅방 ID로 메시지 목록 찾기
    List<Message> findByRoomId(UUID roomId);
    
    // 채팅방 ID로 메시지 목록을 생성 시간 순으로 정렬하여 찾기
    List<Message> findByRoomIdOrderByCreatedAtAsc(UUID roomId);
    
    // 채팅방 ID와 삭제되지 않은 메시지 목록 찾기
    List<Message> findByRoomIdAndIsDeletedFalse(UUID roomId);
    
    // 채팅방 ID로 메시지 개수 세기
    long countByRoomId(UUID roomId);
    
    // 채팅방 ID로 메시지 검색 (대소문자 무시)
    List<Message> findByRoomIdAndContentContainingIgnoreCase(UUID roomId, String keyword);
    
    // 채팅방의 가장 최근 메시지 찾기
    Optional<Message> findTopByRoomIdOrderByCreatedAtDesc(UUID roomId);
}
