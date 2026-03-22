package com.dorandoran.user.service;

import com.dorandoran.user.admin.client.ChatServiceRequestSigner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Auth 서비스 통합 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceIntegration {
    
    private final RestTemplate restTemplate;
    private final ChatServiceRequestSigner requestSigner;
    
    @Value("${auth.service.url:http://localhost:8081}")
    private String authServiceUrl;
    
    /**
     * 이메일 인증 완료 여부 확인
     */
    @SuppressWarnings("unchecked")
    public boolean isEmailVerified(String email) {
        try {
            String url = authServiceUrl + "/api/auth/email/check?email=" + 
                    java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);
            log.debug("Auth 서비스 호출 - 이메일 인증 확인: url={}", url);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            org.springframework.http.HttpEntity<?> entity = new org.springframework.http.HttpEntity<>(headers);
            org.springframework.core.ParameterizedTypeReference<java.util.Map<String, Object>> responseType = 
                    new org.springframework.core.ParameterizedTypeReference<java.util.Map<String, Object>>() {};
            ResponseEntity<java.util.Map<String, Object>> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.GET, entity, responseType);
            
            log.debug("Auth 서비스 응답 - status={}, body={}", response.getStatusCode(), response.getBody());
            
            if (response.getStatusCode().is2xxSuccessful()) {
                java.util.Map<String, Object> body = response.getBody();
                if (body != null) {
                    Object dataObj = body.get("data");
                    log.debug("Auth 서비스 응답 파싱 - data={}, data type={}", dataObj, dataObj != null ? dataObj.getClass() : null);
                    if (dataObj instanceof java.util.Map) {
                        java.util.Map<String, Object> data = (java.util.Map<String, Object>) dataObj;
                        Object verifiedObj = data.get("verified");
                        log.debug("Auth 서비스 응답 파싱 - verified={}, verified type={}", verifiedObj, verifiedObj != null ? verifiedObj.getClass() : null);
                        if (verifiedObj instanceof Boolean) {
                            boolean result = Boolean.TRUE.equals(verifiedObj);
                            log.info("이메일 인증 확인 결과: email={}, verified={}", email, result);
                            return result;
                        }
                    }
                }
            }
            log.warn("이메일 인증 확인 실패 - 응답 파싱 불가: email={}, status={}, body={}", email, response.getStatusCode(), response.getBody());
            return false;
        } catch (Exception e) {
            log.error("Auth 서비스 호출 실패 - 이메일 인증 확인: email={}, error={}", email, e.getMessage(), e);
            throw new RuntimeException("이메일 인증 확인에 실패했습니다.", e);
        }
    }
    
    /**
     * 이메일 인증 데이터 삭제 (회원가입 완료 후)
     */
    public void deleteEmailVerification(String email) {
        try {
            // Redis TTL로 자동 삭제되므로 명시적 삭제는 선택사항
            // 필요시 Auth 서비스에 DELETE 엔드포인트 추가 가능
            log.debug("이메일 인증 데이터는 TTL로 자동 삭제됩니다: email={}", email);
        } catch (Exception e) {
            log.warn("이메일 인증 데이터 삭제 실패: email={}, error={}", email, e.getMessage());
        }
    }

    /**
     * 회원 탈퇴(하드 삭제) 시 해당 사용자 토큰 즉시 무효화 (Auth 내부 API, HMAC).
     */
    public void invalidateTokensForUser(String userId) {
        try {
            String url = authServiceUrl + "/api/auth/internal/users/" + userId + "/invalidate-tokens";
            HttpHeaders headers = new HttpHeaders();
            headers.addAll(requestSigner.createHeaders("user-service"));
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Void> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Void.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Auth 토큰 무효화 응답 비정상: userId={}, status={}", userId, response.getStatusCode());
            }
        } catch (Exception e) {
            log.warn("Auth 토큰 무효화 호출 실패: userId={}, error={}", userId, e.getMessage());
        }
    }
}

