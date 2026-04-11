package com.dorandoran.user.admin.controller;

import com.dorandoran.user.admin.client.ChatServiceClient;
import com.dorandoran.user.entity.User;
import com.dorandoran.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 관리자 대화 조회 Controller (User Service -> Chat Service)
 */
// TODO: ChatLogController와 유사하지만 데이터 소스와 목적이 다름 - 둘 다 필요
// - AdminConversationController: Chat Service (Feign Client) → 실시간 chat_schema 데이터 조회
// - ChatLogController: 직접 Repository 조회 → 아카이브 archive_schema 데이터 조회
// 역할 구분:
//   * AdminConversationController: 현재 진행 중인 대화 관리 (실시간 모니터링)
//   * ChatLogController: 과거 아카이브 데이터 분석 (Agent 결과 포함)
@RestController
@RequestMapping("/api/admin/conversations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Conversations", description = "관리자 대화 조회 API")
public class AdminConversationController {

    private final ChatServiceClient chatServiceClient;
    private final UserRepository userRepository;

    // TODO: ChatLogController.searchChatLogs()와 유사하지만 데이터 소스가 다름
    // - 이 메서드: Chat Service의 실시간 데이터 (chat_schema)
    // - searchChatLogs(): 아카이브 데이터 (archive_schema)
    @GetMapping
    @Operation(summary = "대화 목록 조회", description = "관리자용 대화 목록을 조회합니다.")
    public ResponseEntity<Map<String, Object>> getConversations(
        @RequestParam(required = false) String userEmail,
        @RequestParam(required = false) String from,
        @RequestParam(required = false) String to,
        @RequestParam(required = false) String roomKey,
        @RequestParam(required = false) Integer intimacyLevel,
        @RequestParam(required = false, defaultValue = "chat") String dataSource,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            UUID userId = null;
            String archiveUserEmail = null;
            if (userEmail != null && !userEmail.isBlank()) {
                if ("archive".equalsIgnoreCase(dataSource)) {
                    archiveUserEmail = userEmail;
                } else {
                    User user = userRepository.findByEmailIgnoreCase(
                            userEmail.trim().toLowerCase(Locale.ROOT))
                        .orElse(null);
                    if (user == null) {
                        return ResponseEntity.ok(Map.of(
                            "content", java.util.List.of(),
                            "page", Map.of("number", page, "size", size, "totalPages", 0, "totalElements", 0)
                        ));
                    }
                    userId = user.getId();
                }
            }

            Map<String, Object> response = chatServiceClient.getAdminConversations(
                userId, archiveUserEmail, from, to, roomKey, intimacyLevel, dataSource, page, size
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("대화 목록 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // TODO: ChatLogController.getMessageTimeline()과 유사하지만 데이터 소스가 다름
    // - 이 메서드: Chat Service의 실시간 데이터
    // - getMessageTimeline(): 아카이브 데이터 + Agent 결과 상세 파싱
    @GetMapping("/{conversationId}")
    @Operation(summary = "대화 상세 조회", description = "관리자용 대화 상세를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getConversationDetail(
        @PathVariable UUID conversationId,
        @RequestParam(required = false, defaultValue = "chat") String dataSource
    ) {
        try {
            Map<String, Object> response = chatServiceClient.getAdminConversationDetail(conversationId, dataSource);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("대화 상세 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}