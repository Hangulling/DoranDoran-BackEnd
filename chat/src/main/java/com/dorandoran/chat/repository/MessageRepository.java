package com.dorandoran.chat.repository;

import com.dorandoran.chat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    // 채팅방 ID로 메시지 목록 찾기
    List<Message> findByChatRoomId(UUID chatroomId);

    // 채팅방 ID로 메시지 목록을 시퀀스 순으로 정렬하여 찾기
    List<Message> findByChatRoomIdOrderBySequenceNumberAsc(UUID chatroomId);

    // 채팅방 ID와 삭제되지 않은 메시지 목록 찾기
    List<Message> findByChatRoomIdAndIsDeletedFalse(UUID chatroomId);

    // 채팅방 ID로 메시지 개수 세기
    long countByChatRoomId(UUID chatroomId);

    // 채팅방 ID로 메시지 검색 (대소문자 무시)
    List<Message> findByChatRoomIdAndContentContainingIgnoreCase(UUID chatroomId, String keyword);

    // 채팅방의 가장 최근 메시지 찾기 (시퀀스 기준)
    Optional<Message> findTopByChatRoomIdOrderBySequenceNumberDesc(UUID chatroomId);
    
    // 채팅방 ID로 메시지 목록을 시퀀스 순으로 정렬하여 찾기 (페이징)
    Page<Message> findByChatRoomIdOrderBySequenceNumberAsc(UUID chatroomId, Pageable pageable);
}
