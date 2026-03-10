package com.dorandoran.user.client;

import com.dorandoran.user.admin.client.ChatServiceRequestSigner;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Chat Service의 푸시 문구 생성 API(`/api/chat/push-text`) 호출 클라이언트.
 * User Service의 배치(NotificationDispatchService)에서 사용한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatPushTextClient {

    private final RestTemplate restTemplate;
    private final ChatServiceRequestSigner requestSigner;

    @Value("${chat.service.url:http://localhost:8083}")
    private String chatServiceUrl;

    /**
     * Chat Service에 LLM 기반 푸시 body 생성을 요청한다.
     *
     * @return 생성된 body 텍스트, 실패 시 null
     */
    public String generatePushBody(UUID userId,
                                   UUID chatbotId,
                                   String topic,
                                   String concept,
                                   int intimacyLevel) {
        String url = chatServiceUrl + "/api/chat/push-text";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // HMAC 인증 헤더: 실제 유저 UUID를 사용해도 되고, 필요 시 service-id로도 변경 가능
        headers.addAll(requestSigner.createHeaders(userId.toString()));

        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("chatbotId", chatbotId);
        body.put("topic", topic);
        body.put("concept", concept);
        body.put("intimacyLevel", intimacyLevel);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            long start = System.currentTimeMillis();
            ResponseEntity<PushTextResponseDto> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                PushTextResponseDto.class
            );
            long end = System.currentTimeMillis();

            log.info("[ChatPushTextClient] push-text 호출 완료: status={}, elapsedMs={}",
                response.getStatusCode(), (end - start));

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("[ChatPushTextClient] push-text 응답이 비정상입니다. status={}, body=null",
                    response.getStatusCode());
                return null;
            }

            String result = response.getBody().getBody();
            if (result == null || result.isBlank()) {
                log.warn("[ChatPushTextClient] push-text 응답 body가 비어 있습니다.");
                return null;
            }
            return result;
        } catch (Exception e) {
            log.error("[ChatPushTextClient] push-text 호출 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Chat Service의 PushTextResponse에 대응하는 최소 DTO.
     */
    @lombok.Getter
    @lombok.Setter
    public static class PushTextResponseDto {
        private String body;
    }
}

