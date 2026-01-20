package com.dorandoran.user.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.admin.json:}")
    private String firebaseAdminJson;

    @Bean
    @ConditionalOnProperty(name = "firebase.admin.json")
    public FirebaseApp firebaseApp(ObjectProvider<FirebaseApp> existing) {
        if (existing.getIfAvailable() != null) {
            return existing.getIfAvailable();
        }
        try {
            String normalized = firebaseAdminJson.replace("\\n", "\n");
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
}
