package com.dorandoran.user.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.admin.json:}")
    private String firebaseAdminJson;

    @Value("${firebase.admin.json-base64:}")
    private String firebaseAdminJsonBase64;

    @Bean
    public FirebaseApp firebaseApp() {
        String jsonToUse = resolveJson();
        if (jsonToUse == null || jsonToUse.trim().isEmpty()) {
            log.warn("Firebase Admin JSON이 설정되지 않아 Firebase 초기화를 건너뜁니다.");
            return null;
        }
        
        try {
            return FirebaseApp.getInstance();
        } catch (IllegalStateException e) {
            // 인스턴스가 없으면 새로 생성
        }
        
        try {
            String normalized = jsonToUse.replace("\\n", "\n");
            ByteArrayInputStream stream = new ByteArrayInputStream(normalized.getBytes(StandardCharsets.UTF_8));
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(stream))
                .build();
            return FirebaseApp.initializeApp(options);
        } catch (Exception e) {
            log.error("FirebaseApp 초기화 실패", e);
            throw new IllegalStateException("Firebase Admin 초기화 실패", e);
        }
    }

    /** Base64 또는 raw JSON 우선순위로 사용할 JSON 문자열 반환 */
    private String resolveJson() {
        if (firebaseAdminJsonBase64 != null && !firebaseAdminJsonBase64.trim().isEmpty()) {
            try {
                byte[] decoded = Base64.getDecoder().decode(firebaseAdminJsonBase64.trim());
                return new String(decoded, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                log.warn("Firebase Admin JSON Base64 디코딩 실패, raw JSON 시도", e);
            }
        }
        return firebaseAdminJson;
    }
}
