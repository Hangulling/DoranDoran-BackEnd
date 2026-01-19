package com.dorandoran.batch.job;

import com.dorandoran.batch.service.ArchiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Archive Job
 * JAR 실행 시 아카이빙 수행
 * 
 * 사용법:
 * java -jar batch.jar --archive.enabled=true --archive.days-old=90 --archive.limit=100
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "archive.enabled", havingValue = "true", matchIfMissing = false)
public class ArchiveJob implements CommandLineRunner {
    
    private final ArchiveService archiveService;
    
    @Value("${archive.days-old:90}")
    private int daysOld;
    
    @Value("${archive.limit:100}")
    private int limit;
    
    @Value("${archive.job-name:default-archive-job}")
    private String jobName;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("=== Archive Job 시작 ===");
        
        log.info("아카이빙 설정: daysOld={}, limit={}, jobName={}", daysOld, limit, jobName);
        
        try {
            // 아카이빙 대상 조회
            List<UUID> chatroomIds = archiveService.findChatroomsToArchive(daysOld, limit);
            
            if (chatroomIds.isEmpty()) {
                log.info("아카이빙 대상이 없습니다.");
                return;
            }
            
            log.info("아카이빙 대상: {}개 채팅방", chatroomIds.size());
            
            int successCount = 0;
            int failCount = 0;
            UUID lastChatroomId = null;
            UUID lastMessageId = null;
            LocalDateTime lastMessageCreatedAt = null;
            
            // 각 채팅방 아카이빙
            for (UUID chatroomId : chatroomIds) {
                try {
                    log.info("채팅방 아카이빙 시작: chatroomId={}", chatroomId);
                    
                    // 채팅방 아카이빙
                    UUID archChatroomId = archiveService.archiveChatroom(chatroomId);
                    if (archChatroomId == null) {
                        log.warn("채팅방 아카이빙 실패: chatroomId={}", chatroomId);
                        failCount++;
                        continue;
                    }
                    
                    // 메시지 아카이빙
                    archiveService.archiveMessages(chatroomId, archChatroomId);
                    
                    lastChatroomId = chatroomId;
                    successCount++;
                    
                    log.info("채팅방 아카이빙 완료: chatroomId={}, archChatroomId={}", chatroomId, archChatroomId);
                    
                } catch (Exception e) {
                    log.error("채팅방 아카이빙 실패: chatroomId={}", chatroomId, e);
                    failCount++;
                }
            }
            
            // 진행 상태 업데이트
            archiveService.updateIngestionState(
                jobName,
                lastChatroomId,
                lastMessageId,
                lastMessageCreatedAt,
                "COMPLETED",
                String.format("성공: %d, 실패: %d", successCount, failCount)
            );
            
            log.info("=== Archive Job 완료: 성공={}, 실패={} ===", successCount, failCount);
            
        } catch (Exception e) {
            log.error("Archive Job 실패", e);
            
            // 진행 상태 업데이트 (실패)
            archiveService.updateIngestionState(
                jobName,
                null,
                null,
                null,
                "FAILED",
                "오류: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())
            );
            
            throw e;
        }
    }
}

