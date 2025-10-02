package com.dorandoran.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Chat 서비스 독립 실행 애플리케이션
 * 포트: 8083
 */
@SpringBootApplication(scanBasePackages = {
    "com.dorandoran.chat",
    "com.dorandoran.shared",
    "com.dorandoran.common",
    "com.dorandoran.infra.persistence"
})
public class ChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatApplication.class, args);
    }
}
