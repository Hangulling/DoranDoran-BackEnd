package com.dorandoran.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Batch 서비스 독립 실행 애플리케이션
 * 포트: 8085
 */
@SpringBootApplication(scanBasePackages = {
    "com.dorandoran.batch",
    "com.dorandoran.shared",
    "com.dorandoran.common",
    "com.dorandoran.infra.persistence"
})
public class BatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }
}
