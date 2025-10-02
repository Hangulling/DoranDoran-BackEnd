package com.dorandoran.store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Store 서비스 독립 실행 애플리케이션
 * 포트: 8084
 */
@SpringBootApplication(scanBasePackages = {
    "com.dorandoran.store",
    "com.dorandoran.shared",
    "com.dorandoran.common",
    "com.dorandoran.infra.persistence"
})
public class StoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(StoreApplication.class, args);
    }
}
