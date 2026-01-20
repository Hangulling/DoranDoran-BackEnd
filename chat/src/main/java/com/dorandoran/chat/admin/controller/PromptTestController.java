package com.dorandoran.chat.admin.controller;

import com.dorandoran.chat.admin.dto.PromptTestRequest;
import com.dorandoran.chat.admin.dto.PromptTestResponse;
import com.dorandoran.chat.admin.service.PromptTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * 프롬프트 테스트 Controller
 */
@RestController
@RequestMapping("/api/admin/prompts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Prompt Test", description = "프롬프트 테스트 API")
public class PromptTestController {

    private final PromptTestService promptTestService;

    @PostMapping("/test")
    @Operation(summary = "프롬프트 테스트 실행", description = "지정된 Agent에 대해 프롬프트를 테스트합니다.")
    public Mono<ResponseEntity<PromptTestResponse>> testPrompt(@RequestBody PromptTestRequest request) {
        log.info("=== [Chat Service Controller] 프롬프트 테스트 요청 수신 ===");
        log.info("  - agentType: {}", request.getAgentType());
        log.info("  - concept: {}", request.getConcept());
        log.info("  - intimacyLevel: {}", request.getIntimacyLevel());
        log.info("  - inputText 길이: {}자", request.getInputText() != null ? request.getInputText().length() : 0);
        log.info("  - inputText 미리보기: {}", request.getInputText() != null && request.getInputText().length() > 100 
            ? request.getInputText().substring(0, 100) + "..." : request.getInputText());
        log.info("  - ⚠️ 주의: Agent는 DB 우선, 파일 fallback으로 프롬프트를 읽습니다.");
        log.info("  - fallback 파일 경로: prompts/{}/{}_{}.txt",
            request.getAgentType().toLowerCase().replace("_", "/"),
            request.getConcept().toLowerCase(),
            request.getIntimacyLevel());
        
        return promptTestService.testPrompt(request)
            .doOnNext(response -> {
                log.info("=== [Chat Service Controller] 프롬프트 테스트 완료 ===");
                log.info("  - 응답 latencyMs: {}ms", response.getLatencyMs());
                log.info("  - 응답 outputText 길이: {}자", response.getOutputText() != null ? response.getOutputText().length() : 0);
                log.info("  - 응답 tokens: {}", response.getTokens());
            })
            .map(ResponseEntity::ok)
            .onErrorResume(error -> {
                log.error("=== [Chat Service Controller] 프롬프트 테스트 실패 ===", error);
                log.error("  - 오류 메시지: {}", error.getMessage());
                return Mono.just(ResponseEntity.internalServerError().build());
            });
    }
}
