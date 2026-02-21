package com.dorandoran.chat.admin.controller;

import com.dorandoran.chat.admin.service.PromptFileReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 프롬프트 파일 내용 조회 Controller
 */
@RestController
@RequestMapping("/api/admin/prompts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Prompt File", description = "프롬프트 파일 조회 API")
public class PromptFileContentController {

    private final PromptFileReadService promptFileReadService;

    @GetMapping("/file-content")
    @Operation(summary = "파일 내용 조회", description = "현재 프롬프트 파일 내용을 조회합니다.")
    public ResponseEntity<Map<String, String>> getPromptFileContent(
            @RequestParam String agentType,
            @RequestParam String concept,
            @RequestParam Integer intimacyLevel
    ) {
        try {
            String content = promptFileReadService.readPromptContent(agentType, concept, intimacyLevel);
            if (content == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(Map.of("content", content));
        } catch (Exception e) {
            log.error("프롬프트 파일 내용 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
