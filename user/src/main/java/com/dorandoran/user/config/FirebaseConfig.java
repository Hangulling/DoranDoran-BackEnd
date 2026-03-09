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
import java.util.Collections;
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
            log.debug("[FCM 401 디버그] FirebaseOptions 빌드: GoogleCredentials.createScoped(cloud-platform), setProjectId() 미사용");
            ByteArrayInputStream stream = new ByteArrayInputStream(normalized.getBytes(StandardCharsets.UTF_8));
            GoogleCredentials credentials = GoogleCredentials.fromStream(stream)
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/cloud-platform"));
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
            FirebaseApp app = FirebaseApp.initializeApp(options);
            logProjectInfo(normalized);
            log.info("[FCM 401 디버그] FirebaseApp.initializeApp() 성공, name={}", app.getName());
            return app;
        } catch (Exception e) {
            log.error("FirebaseApp 초기화 실패", e);
            throw new IllegalStateException("Firebase Admin 초기화 실패", e);
        }
    }

    /** 초기화 시 project_id, client_email만 로그 (키 노출 방지) */
    private void logProjectInfo(String json) {
        try {
            log.debug("[FCM 401 디버그] logProjectInfo: normalized JSON 길이={}", json.length());
            int pi = json.indexOf("\"project_id\"");
            int ci = json.indexOf("\"client_email\"");
            String pid = extractQuotedValue(json, pi);
            String email = extractQuotedValue(json, ci);
            log.info("Firebase Admin 초기화: project_id={}, client_email={}", pid, email);
        } catch (Exception e) {
            log.debug("Firebase project 정보 로그 생략", e);
        }
    }

    private static String extractQuotedValue(String json, int keyIndex) {
        if (keyIndex < 0) return "(unknown)";
        int colon = json.indexOf(':', keyIndex);
        int start = json.indexOf('"', colon);
        if (start < 0) return "(unknown)";
        int end = json.indexOf('"', start + 1);
        return end < 0 ? "(unknown)" : json.substring(start + 1, end);
    }

    /** Base64 또는 raw JSON 우선순위로 사용할 JSON 문자열 반환 */
    private String resolveJson() {
        if (firebaseAdminJsonBase64 != null && !firebaseAdminJsonBase64.trim().isEmpty()) {
            try {
                String base64Trimmed = firebaseAdminJsonBase64.trim();
                log.debug("[FCM 401 디버그] env FIREBASE_ADMIN_JSON_BASE64 길이(trim 후)={}", base64Trimmed.length());
                byte[] decoded = Base64.getDecoder().decode(base64Trimmed);
                String json = new String(decoded, StandardCharsets.UTF_8);
                boolean hasProjectId = json.contains("\"project_id\"");
                boolean hasClientEmail = json.contains("\"client_email\"");
                log.info("[FCM 401 디버그] Base64 디코딩 완료: decodedJsonLength={}, hasProjectId={}, hasClientEmail={}",
                    json.length(), hasProjectId, hasClientEmail);
                return json;
            } catch (IllegalArgumentException e) {
                log.warn("Firebase Admin JSON Base64 디코딩 실패, raw JSON 시도", e);
            }
        }
        log.debug("[FCM 401 디버그] firebase.admin.json-base64 비어있음, raw firebase.admin.json 사용 시도");
        return firebaseAdminJson;
    }
}
