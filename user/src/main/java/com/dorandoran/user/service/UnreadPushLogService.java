package com.dorandoran.user.service;

import com.dorandoran.user.entity.UnreadPushLog;
import com.dorandoran.user.repository.UnreadPushLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * 안읽음 푸시 로그 서비스.
 * 푸시 발송 성공 시 insert, 앱에서 조회/읽음 처리.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnreadPushLogService {

    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    private final UnreadPushLogRepository repository;

    @Transactional
    public UnreadPushLog insert(
        UUID userId,
        String pushType,
        UUID chatbotId,
        UUID chatroomId,
        UUID messageId,
        String concept,
        String topic,
        String startMessage,
        String title,
        String body,
        OffsetDateTime sentAt
    ) {
        UnreadPushLog entity = UnreadPushLog.builder()
            .userId(userId)
            .pushType(pushType)
            .chatbotId(chatbotId)
            .chatroomId(chatroomId)
            .messageId(messageId)
            .concept(concept)
            .topic(topic)
            .startMessage(startMessage)
            .title(title)
            .body(body)
            .sentAt(sentAt != null ? sentAt : OffsetDateTime.now(KST))
            .build();
        UnreadPushLog saved = repository.save(entity);
        log.debug("안읽음 푸시 로그 저장: id={}, userId={}, pushType={}", saved.getId(), userId, pushType);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<UnreadPushLog> findUnreadByUserId(UUID userId, Pageable pageable) {
        return repository.findByUserIdAndReadAtIsNullOrderBySentAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long countUnreadByUserId(UUID userId) {
        return repository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public int markRead(UUID userId, List<Long> ids) {
        OffsetDateTime now = OffsetDateTime.now(KST);
        if (ids == null || ids.isEmpty()) {
            return repository.markAllReadByUserId(userId, now);
        }
        return repository.markReadByIdInAndUserId(ids, userId, now);
    }

    @Transactional
    public boolean markReadById(UUID userId, Long id) {
        OffsetDateTime now = OffsetDateTime.now(KST);
        int updated = repository.markReadByIdAndUserId(id, userId, now);
        return updated > 0;
    }
}
