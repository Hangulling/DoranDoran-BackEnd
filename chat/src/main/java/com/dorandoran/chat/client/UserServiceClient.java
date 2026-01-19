package com.dorandoran.chat.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.dorandoran.shared.security.HmacVerifier;
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
 * User Service 클라이언트
 * RestTemplate을 사용하여 User Service와 통신
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${user.service.url:http://localhost:8082}")
    private String userServiceUrl;

    @Value("${gateway.jwt.hmac-secret:}")
    private String hmacSecret;

    /**
     * Active 프롬프트 조회
     * @param agentType 에이전트 타입 (예: INTIMACY_ANALYSIS)
     * @param concept 컨셉 (예: friend)
     * @param intimacyLevel 친밀도 레벨 (1 또는 3)
     * @param env 환경 (prod, dev, test)
     * @return 프롬프트 내용, 없으면 null
     */
    public String getActivePrompt(String agentType, String concept, Integer intimacyLevel, String env) {
        if (env == null || env.isEmpty()) {
            env = "prod";
        }

        String url = String.format("%s/api/admin/prompts/active?agentType=%s&concept=%s&intimacyLevel=%d&env=%s",
            userServiceUrl, agentType, concept, intimacyLevel, env);

        HttpHeaders headers = buildHmacHeaders("00000000-0000-0000-0000-000000000000");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            log.debug("User Service에서 active 프롬프트 조회: agentType={}, concept={}, intimacyLevel={}, env={}",
                agentType, concept, intimacyLevel, env);

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Object contentObj = body.get("content");
                if (contentObj != null) {
                    String content = contentObj.toString();
                    log.debug("Active 프롬프트 조회 성공: 길이={}자", content.length());
                    return content;
                }
            }

            log.debug("Active 프롬프트 없음: agentType={}, concept={}, intimacyLevel={}, env={}",
                agentType, concept, intimacyLevel, env);
            return null;

        } catch (Exception e) {
            log.warn("User Service에서 active 프롬프트 조회 실패 (fallback 사용): agentType={}, concept={}, intimacyLevel={}, env={}, error={}",
                agentType, concept, intimacyLevel, env, e.getMessage());
            return null;
        }
    }

    /**
     * 퍼펙트 누적 횟수 증가
     */
    public void incrementPerfect(UUID userId) {
        String url = String.format("%s/api/users/%s/stats/perfect", userServiceUrl, userId);
        HttpHeaders headers = buildHmacHeaders("00000000-0000-0000-0000-000000000000");
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        } catch (Exception e) {
            log.warn("퍼펙트 누적 호출 실패: userId={}, error={}", userId, e.getMessage());
        }
    }

    private HttpHeaders buildHmacHeaders(String userId) {
        long ts = System.currentTimeMillis();
        String message = userId + "|" + ts;
        String sign = HmacVerifier.hmacSha256Hex(hmacSecret, message);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId);
        headers.set("X-Auth-Ts", Long.toString(ts));
        headers.set("X-Auth-Sign", sign);
        return headers;
    }
}
