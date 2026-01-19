package com.dorandoran.chat.client;

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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 내부 서비스 간 통신이므로 X-User-Id 헤더는 선택적으로 처리
        // User Service에서 내부 호출 시 헤더를 선택적으로 받도록 수정 필요할 수 있음
        headers.set("X-User-Id", "00000000-0000-0000-0000-000000000000"); // 시스템 사용자 ID

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
}
