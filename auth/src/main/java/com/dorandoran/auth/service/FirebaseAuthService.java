package com.dorandoran.auth.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Firebase Authentication 서비스
 * Firebase ID Token 검증 및 사용자 정보 추출
 */
@Service
@Slf4j
public class FirebaseAuthService {
    
    @Value("${firebase.project-id:}")
    private String projectId;
    
    @Value("${firebase.client-email:}")
    private String clientEmail;
    
    @Value("${firebase.private-key:}")
    private String privateKey;
    
    private FirebaseAuth firebaseAuth;
    
    @PostConstruct
    public void initialize() {
        if (projectId == null || projectId.trim().isEmpty() ||
            clientEmail == null || clientEmail.trim().isEmpty() ||
            privateKey == null || privateKey.trim().isEmpty()) {
            log.warn("Firebase 설정이 완전하지 않습니다. Firebase Auth 기능이 비활성화됩니다.");
            log.warn("projectId: {}, clientEmail: {}, privateKey: {}", 
                    projectId != null && !projectId.isEmpty() ? "설정됨" : "미설정",
                    clientEmail != null && !clientEmail.isEmpty() ? "설정됨" : "미설정",
                    privateKey != null && !privateKey.isEmpty() ? "설정됨" : "미설정");
            return;
        }
        
        try {
            // Firebase Admin SDK 초기화
            // 이미 초기화된 경우 스킵
            if (FirebaseApp.getApps().isEmpty()) {
                // private key의 이스케이프된 개행 문자를 실제 개행 문자로 변환
                String normalizedPrivateKey = normalizePrivateKey(privateKey);
                
                FirebaseOptions options = FirebaseOptions.builder()
                        .setProjectId(projectId)
                        .setCredentials(
                                com.google.auth.oauth2.GoogleCredentials.fromStream(
                                        createCredentialsJsonStream(normalizedPrivateKey)
                                )
                        )
                        .build();
                
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK 초기화 완료: projectId={}", projectId);
            } else {
                log.info("Firebase Admin SDK가 이미 초기화되어 있습니다.");
            }
            
            firebaseAuth = FirebaseAuth.getInstance();
            log.info("Firebase Auth 인스턴스 생성 완료");
            
        } catch (Exception e) {
            log.error("Firebase Admin SDK 초기화 실패", e);
            throw new RuntimeException("Firebase Admin SDK 초기화에 실패했습니다.", e);
        }
    }
    
    /**
     * 환경 변수에서 받은 private key의 이스케이프된 개행 문자를 실제 개행 문자로 변환
     * Docker 환경변수에서 전달될 때 여러 형태로 올 수 있음
     */
    private String normalizePrivateKey(String key) {
        if (key == null) {
            return null;
        }
        // 여러 형태의 줄바꿈 표현을 실제 개행 문자로 변환
        // 1. 이중 이스케이프 (\\\\n) -> 실제 개행
        // 2. 단일 이스케이프 (\n) -> 실제 개행
        // 3. 이미 실제 개행이 있는 경우는 그대로 유지
        String normalized = key.replace("\\\\n", "\n")  // 이중 이스케이프 먼저 처리
                               .replace("\\n", "\n")    // 단일 이스케이프 처리
                               .replace("\\\\r", "\r")
                               .replace("\\r", "\r");
        
        // BEGIN/END PRIVATE KEY 라인 확인 및 정리
        if (!normalized.contains("-----BEGIN PRIVATE KEY-----")) {
            log.warn("Private Key 형식이 올바르지 않습니다. BEGIN/END 마커가 없습니다.");
        }
        
        return normalized;
    }
    
    /**
     * 서비스 계정 정보로부터 JSON 스트림 생성
     */
    private ByteArrayInputStream createCredentialsJsonStream(String normalizedPrivateKey) {
        // private_key의 개행 문자를 JSON 문자열에서 이스케이프 처리
        // 실제 개행 문자(\n)를 JSON 문자열 내에서 \\n으로 변환
        String escapedPrivateKey = normalizedPrivateKey.replace("\\", "\\\\")  // 백슬래시 먼저 이스케이프
                                                        .replace("\n", "\\n")   // 개행 문자 이스케이프
                                                        .replace("\r", "\\r")   // 캐리지 리턴 이스케이프
                                                        .replace("\"", "\\\""); // 따옴표 이스케이프
        
        String credentialsJson = String.format(
                "{\n" +
                "  \"type\": \"service_account\",\n" +
                "  \"project_id\": \"%s\",\n" +
                "  \"private_key_id\": \"\",\n" +
                "  \"private_key\": \"%s\",\n" +
                "  \"client_email\": \"%s\",\n" +
                "  \"client_id\": \"\",\n" +
                "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
                "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
                "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
                "  \"client_x509_cert_url\": \"\",\n" +
                "  \"universe_domain\": \"googleapis.com\"\n" +
                "}",
                projectId,
                escapedPrivateKey,
                clientEmail
        );
        
        return new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Firebase ID Token 검증 및 사용자 정보 추출
     * 
     * @param idToken Firebase ID Token
     * @return Firebase 사용자 정보 (email, name, picture, uid 등)
     * @throws Exception Token 검증 실패 시
     */
    public FirebaseUserInfo verifyIdToken(String idToken) throws Exception {
        log.debug("Firebase ID Token 검증 시작");
        
        if (firebaseAuth == null) {
            throw new IllegalStateException("Firebase Auth가 초기화되지 않았습니다. 설정을 확인해주세요.");
        }
        
        try {
            // Firebase ID Token 검증
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
            
            // 사용자 정보 추출
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();
            String picture = decodedToken.getPicture();
            
            // 커스텀 클레임에서 추가 정보 추출 (있는 경우)
            String givenName = decodedToken.getClaims().get("given_name") != null ? 
                    decodedToken.getClaims().get("given_name").toString() : null;
            String familyName = decodedToken.getClaims().get("family_name") != null ? 
                    decodedToken.getClaims().get("family_name").toString() : null;
            
            // 이름이 없으면 given_name과 family_name 조합
            if (name == null || name.isEmpty()) {
                if (givenName != null || familyName != null) {
                    name = ((givenName != null ? givenName : "") + " " + 
                            (familyName != null ? familyName : "")).trim();
                }
            }
            
            log.info("Firebase ID Token 검증 성공: email={}, uid={}", email, uid);
            
            return new FirebaseUserInfo(
                    email != null ? email : "",
                    givenName != null ? givenName : "",
                    familyName != null ? familyName : "",
                    name != null ? name : "",
                    picture,
                    uid  // Firebase UID를 OAuth ID로 사용
            );
            
        } catch (FirebaseAuthException e) {
            log.error("Firebase ID Token 검증 실패: {}", e.getMessage());
            throw new IllegalArgumentException("Firebase ID Token이 유효하지 않습니다: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Firebase ID Token 검증 중 예상치 못한 오류 발생", e);
            throw new Exception("Firebase ID Token 검증 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * Firebase 사용자 정보 DTO
     */
    public record FirebaseUserInfo(
            String email,
            String firstName,
            String lastName,
            String name,
            String picture,
            String uid  // Firebase 사용자 고유 ID (oauthId로 사용)
    ) {
    }
}

