package com.dorandoran.user.repository;

import com.dorandoran.user.entity.UnreadPushLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface UnreadPushLogRepository extends JpaRepository<UnreadPushLog, Long> {

    Page<UnreadPushLog> findByUserIdAndReadAtIsNullOrderBySentAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(UUID userId);

    @Modifying
    @Query("UPDATE UnreadPushLog u SET u.readAt = :readAt WHERE u.id = :id AND u.userId = :userId")
    int markReadByIdAndUserId(@Param("id") Long id, @Param("userId") UUID userId, @Param("readAt") OffsetDateTime readAt);

    @Modifying
    @Query("UPDATE UnreadPushLog u SET u.readAt = :readAt WHERE u.id IN :ids AND u.userId = :userId")
    int markReadByIdInAndUserId(@Param("ids") List<Long> ids, @Param("userId") UUID userId, @Param("readAt") OffsetDateTime readAt);

    @Modifying
    @Query("UPDATE UnreadPushLog u SET u.readAt = :readAt WHERE u.userId = :userId AND u.readAt IS NULL")
    int markAllReadByUserId(@Param("userId") UUID userId, @Param("readAt") OffsetDateTime readAt);
}
