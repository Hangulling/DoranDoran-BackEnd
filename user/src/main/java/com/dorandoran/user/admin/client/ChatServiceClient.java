package com.dorandoran.user.admin.client;

import com.dorandoran.user.admin.dto.PromptTestRequest;
import com.dorandoran.user.admin.dto.PromptTestResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

/**
 * Chat Service 클라이언트
 * RestTemplate을 사용하여 Chat Service와 통신
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatServiceClient {

    private final RestTemplate restTemplate;
    private final ChatServiceRequestSigner requestSigner;

    @Value("${chat.service.url:http://localhost:8083}")
    private String chatServiceUrl;

    /**
     * 프롬프트 테스트 실행
     */
    public PromptTestResponse testAgent(PromptTestRequest request) {
        log.info("=== [ChatServiceClient] Chat Service 프롬프트 테스트 호출 시작 ===");
        log.info("  - Chat Service URL: {}", chatServiceUrl);
        log.info("  - 요청 URL: {}/api/admin/prompts/test", chatServiceUrl);
        log.info("  - agentType: {}", request.getAgentType());
        log.info("  - concept: {}", request.getConcept());
        log.info("  - intimacyLevel: {}", request.getIntimacyLevel());
        log.info("  - inputText 길이: {}자", request.getInputText() != null ? request.getInputText().length() : 0);
        
        String url = chatServiceUrl + "/api/admin/prompts/test";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.addAll(requestSigner.createHeaders("user-service"));
        
        HttpEntity<PromptTestRequest> entity = new HttpEntity<>(request, headers);
        
        try {
            long startTime = System.currentTimeMillis();
            ResponseEntity<PromptTestResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                PromptTestResponse.class
            );
            long endTime = System.currentTimeMillis();
            
            log.info("=== [ChatServiceClient] Chat Service 프롬프트 테스트 호출 완료 ===");
            log.info("  - HTTP 상태 코드: {}", response.getStatusCode());
            log.info("  - 호출 소요 시간: {}ms", endTime - startTime);
            
            PromptTestResponse body = response.getBody();
            if (body != null) {
                log.info("  - 응답 latencyMs: {}ms", body.getLatencyMs());
                log.info("  - 응답 outputText 길이: {}자", body.getOutputText() != null ? body.getOutputText().length() : 0);
                log.info("  - 응답 tokens: {}", body.getTokens());
            }
            
            return body;
        } catch (Exception e) {
            log.error("=== [ChatServiceClient] Chat Service 프롬프트 테스트 호출 실패 ===", e);
            log.error("  - 오류 메시지: {}", e.getMessage());
            throw new RuntimeException("Chat Service를 사용할 수 없습니다. 잠시 후 다시 시도해주세요.", e);
        }
    }

    /**
     * 현재 프롬프트 파일 내용 조회
     * (Chat Service에 해당 엔드포인트가 구현되어 있다고 가정)
     */
    public String getPromptFileContent(String agentType, String concept, Integer intimacyLevel) {
        String url = String.format("%s/api/admin/prompts/file-content?agentType=%s&concept=%s&intimacyLevel=%d",
            chatServiceUrl, agentType, concept, intimacyLevel);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.addAll(requestSigner.createHeaders("user-service"));
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
            );

            Map<String, String> body = response.getBody();
            return body != null ? body.get("content") : null;
        } catch (Exception e) {
            log.error("Chat Service 프롬프트 파일 내용 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Chat Service를 사용할 수 없습니다. 잠시 후 다시 시도해주세요.", e);
        }
    }

    /**
     * 프롬프트 파일 동기화
     * @param agentType 에이전트 타입 (예: INTIMACY_ANALYSIS)
     * @param concept 컨셉 (예: friend)
     * @param intimacyLevel 친밀도 레벨 (1 또는 3)
     * @param content 프롬프트 내용
     */
    public boolean syncPromptFile(String agentType, String concept, Integer intimacyLevel, String content) {
        log.info("=== [ChatServiceClient] Chat Service 프롬프트 파일 동기화 호출 시작 ===");
        log.info("  - Chat Service URL: {}", chatServiceUrl);
        log.info("  - 요청 URL: {}/api/admin/prompts/sync", chatServiceUrl);
        log.info("  - agentType: {}", agentType);
        log.info("  - concept: {}", concept);
        log.info("  - intimacyLevel: {}", intimacyLevel);
        log.info("  - content 길이: {}자", content != null ? content.length() : 0);
        
        String url = chatServiceUrl + "/api/admin/prompts/sync";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.addAll(requestSigner.createHeaders("user-service"));
        
        Map<String, Object> requestBody = Map.of(
            "agentType", agentType,
            "concept", concept,
            "intimacyLevel", intimacyLevel,
            "content", content
        );
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        try {
            long startTime = System.currentTimeMillis();
            ResponseEntity<Void> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Void.class
            );
            long endTime = System.currentTimeMillis();
            
            log.info("=== [ChatServiceClient] Chat Service 프롬프트 파일 동기화 호출 완료 ===");
            log.info("  - HTTP 상태 코드: {}", response.getStatusCode());
            log.info("  - 호출 소요 시간: {}ms", endTime - startTime);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("=== [ChatServiceClient] Chat Service 프롬프트 파일 동기화 호출 실패 ===", e);
            log.error("  - 오류 메시지: {}", e.getMessage());
            // 파일 동기화 실패는 로그만 남기고 예외를 던지지 않음 (DB 활성화는 유지)
            log.warn("프롬프트 파일 동기화 실패했지만 DB 활성화는 유지됩니다.");
            return false;
        }
    }

    /**
     * 관리자 대화 목록 조회
     */
    public Map<String, Object> getAdminConversations(
            UUID userId,
            String userEmail,
            String from,
            String to,
            String roomKey,
            Integer intimacyLevel,
            String dataSource,
            int page,
            int size
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(chatServiceUrl + "/api/admin/conversations");
        if (userId != null) {
            builder.queryParam("userId", userId.toString());
        }
        if (userEmail != null && !userEmail.isBlank()) {
            builder.queryParam("userEmail", userEmail);
        }
        if (from != null) {
            builder.queryParam("from", from);
        }
        if (to != null) {
            builder.queryParam("to", to);
        }
        if (roomKey != null) {
            builder.queryParam("roomKey", roomKey);
        }
        if (intimacyLevel != null) {
            builder.queryParam("intimacyLevel", intimacyLevel);
        }
        if (dataSource != null) {
            builder.queryParam("dataSource", dataSource);
        }
        builder.queryParam("page", page).queryParam("size", size);

        String url = builder.toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.addAll(requestSigner.createHeaders("user-service"));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Chat Service 대화 목록 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Chat Service를 사용할 수 없습니다. 잠시 후 다시 시도해주세요.", e);
        }
    }

    public Map<String, Object> getAdminConversations(
            UUID userId,
            String from,
            String to,
            String roomKey,
            Integer intimacyLevel,
            int page,
            int size
    ) {
        return getAdminConversations(userId, null, from, to, roomKey, intimacyLevel, "chat", page, size);
    }

    /**
     * 관리자 대화 상세 조회
     */
    public Map<String, Object> getAdminConversationDetail(UUID conversationId) {
        return getAdminConversationDetail(conversationId, "chat");
    }

    /**
     * 관리자 대화 상세 조회 (데이터 소스 지정)
     */
    public Map<String, Object> getAdminConversationDetail(UUID conversationId, String dataSource) {
        String url = UriComponentsBuilder.fromHttpUrl(chatServiceUrl + "/api/admin/conversations/" + conversationId)
            .queryParam("dataSource", dataSource)
            .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.addAll(requestSigner.createHeaders("user-service"));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Chat Service 대화 상세 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Chat Service를 사용할 수 없습니다. 잠시 후 다시 시도해주세요.", e);
        }
    }
}
