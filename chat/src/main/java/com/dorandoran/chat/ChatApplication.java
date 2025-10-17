package com.dorandoran.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * Chat 서비스 독립 실행 애플리케이션
 * 포트: 8083
 */
@SpringBootApplication(scanBasePackages = {
    "com.dorandoran.chat",
    "com.dorandoran.shared",
    "com.dorandoran.common"
})
@EntityScan(basePackages = {
    "com.dorandoran.chat.entity",
    "com.dorandoran.shared.entity",
    "com.dorandoran.common.entity"
})
public class ChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatApplication.class, args);
    }
}
