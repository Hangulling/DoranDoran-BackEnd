package com.dorandoran.user.admin.repository;

import com.dorandoran.user.admin.entity.ArchChatroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * ArchChatroom Repository
 *
 * Archive 스키마의 채팅방 조회 (읽기 전용)
 */
@Repository
public interface ArchChatroomRepository extends JpaRepository<ArchChatroom, UUID> {

}