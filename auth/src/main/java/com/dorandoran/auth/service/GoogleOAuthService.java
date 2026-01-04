package com.dorandoran.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Google OAuth 2.0 서비스
 * Google ID Token 검증 및 사용자 정보 추출
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthService {
    
    @Value("${google.oauth.client-id}")
    private String clientId;
    
    @Value("${google.oauth.android-client-ids:}")
    private String androidClientIds;
    
    private GoogleIdTokenVerifier verifier;
    
    /**
     * Google ID Token 검증 및 사용자 정보 추출
     * 
     * @param idToken Google ID Token
     * @return Google 사용자 정보 (email, name, picture, sub 등)
     * @throws Exception Token 검증 실패 시
     */
    public GoogleUserInfo verifyIdToken(String idToken) throws Exception {
        log.debug("Google ID Token 검증 시작");
        
        // 허용된 Client ID 목록 생성 (Web + Android)
        List<String> allowedClientIds = getAllowedClientIds();
        
        // Verifier 초기화 (지연 초기화)
        if (verifier == null) {
            verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(allowedClientIds)
                    .build();
        }
        
        // ID Token 검증
        GoogleIdToken googleIdToken = verifier.verify(idToken);
        if (googleIdToken == null) {
            log.error("Google ID Token 검증 실패: Token이 유효하지 않습니다");
            throw new IllegalArgumentException("Google ID Token이 유효하지 않습니다");
        }
        
        // Payload 추출
        GoogleIdToken.Payload payload = googleIdToken.getPayload();
        
        // 발급자 확인
        String issuer = payload.getIssuer();
        if (!issuer.equals("https://accounts.google.com") && !issuer.equals("accounts.google.com")) {
            log.error("Google ID Token 발급자 확인 실패: issuer={}", issuer);
            throw new IllegalArgumentException("Google ID Token의 발급자가 올바르지 않습니다");
        }
        
        // 대상(audience) 확인
        String audience = (String) payload.getAudience();
        if (!allowedClientIds.contains(audience)) {
            log.error("Google ID Token 대상 확인 실패: audience={}, allowed={}", audience, allowedClientIds);
            throw new IllegalArgumentException("Google ID Token의 대상이 올바르지 않습니다");
        }
        
        log.debug("Google ID Token audience 확인 성공: audience={}", audience);
        
        // 만료 시간 확인 (GoogleIdToken.verify()에서 이미 확인하지만 명시적으로 체크)
        long expirationTimeSeconds = payload.getExpirationTimeSeconds();
        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        if (expirationTimeSeconds < currentTimeSeconds) {
            log.error("Google ID Token 만료: expirationTime={}, currentTime={}", 
                    expirationTimeSeconds, currentTimeSeconds);
            throw new IllegalArgumentException("Google ID Token이 만료되었습니다");
        }
        
        // 사용자 정보 추출
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");
        String sub = payload.getSubject(); // Google 사용자 고유 ID
        
        // 이름 분리 (given_name, family_name)
        String givenName = (String) payload.get("given_name");
        String familyName = (String) payload.get("family_name");
        
        log.info("Google ID Token 검증 성공: email={}, sub={}", email, sub);
        
        return new GoogleUserInfo(
                email,
                givenName != null ? givenName : "",
                familyName != null ? familyName : "",
                name != null ? name : (givenName + " " + familyName).trim(),
                picture,
                sub
        );
    }
    
    /**
     * 허용된 모든 Client ID 목록을 반환합니다.
     * Web Client ID와 Android Client ID들을 포함합니다.
     */
    private List<String> getAllowedClientIds() {
        List<String> clientIds = new ArrayList<>();
        
        // Web Client ID 추가
        if (clientId != null && !clientId.trim().isEmpty()) {
            clientIds.add(clientId.trim());
        }
        
        // Android Client ID들 추가
        if (androidClientIds != null && !androidClientIds.trim().isEmpty()) {
            List<String> androidIds = Arrays.stream(androidClientIds.split(","))
                    .map(String::trim)
                    .filter(id -> !id.isEmpty())
                    .collect(Collectors.toList());
            clientIds.addAll(androidIds);
        }
        
        log.debug("허용된 Google OAuth Client ID 목록: {}", clientIds);
        return clientIds;
    }
    
    /**
     * Google 사용자 정보 DTO
     */
    public record GoogleUserInfo(
            String email,
            String firstName,
            String lastName,
            String name,
            String picture,
            String sub  // Google 사용자 고유 ID (oauthId로 사용)
    ) {
    }
}

