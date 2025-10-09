package com.dorandoran.chat.repository;

import com.dorandoran.chat.entity.IntimacyProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntimacyProgressRepository extends JpaRepository<IntimacyProgress, UUID> {
    Optional<IntimacyProgress> findByChatRoomId(UUID chatroomId);
    List<IntimacyProgress> findByUserId(UUID userId);
}
