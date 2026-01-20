package com.dorandoran.chat.admin.controller;

import com.dorandoran.chat.admin.service.PromptFileSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 프롬프트 파일 동기화 Controller
 */
@RestController
@RequestMapping("/api/admin/prompts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Prompt Sync", description = "프롬프트 파일 동기화 API")
public class PromptSyncController {

    private final PromptFileSyncService promptFileSyncService;

    @PostMapping("/sync")
    @Operation(summary = "프롬프트 파일 동기화", description = "DB의 active 프롬프트를 파일 시스템에 동기화합니다.")
    public ResponseEntity<Void> syncPromptFile(@RequestBody Map<String, Object> request) {
        log.info("프롬프트 파일 동기화 요청 수신");
        log.info("  - agentType: {}", request.get("agentType"));
        log.info("  - concept: {}", request.get("concept"));
        log.info("  - intimacyLevel: {}", request.get("intimacyLevel"));
        log.info("  - content 길이: {}자", request.get("content") != null ? request.get("content").toString().length() : 0);

        try {
            String agentType = (String) request.get("agentType");
            String concept = (String) request.get("concept");
            Integer intimacyLevel = (Integer) request.get("intimacyLevel");
            String content = (String) request.get("content");

            if (agentType == null || concept == null || intimacyLevel == null || content == null) {
                log.error("필수 파라미터 누락: agentType={}, concept={}, intimacyLevel={}, content={}", 
                    agentType, concept, intimacyLevel, content != null ? "있음" : "없음");
                return ResponseEntity.badRequest().build();
            }

            promptFileSyncService.syncPromptToFile(agentType, concept, intimacyLevel, content);

            log.info("프롬프트 파일 동기화 완료");
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("프롬프트 파일 동기화 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
