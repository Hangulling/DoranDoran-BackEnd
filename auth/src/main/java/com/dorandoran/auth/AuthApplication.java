package com.dorandoran.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Auth 서비스 독립 실행 애플리케이션
 * 포트: 8081
 */
@SpringBootApplication(scanBasePackages = {
    "com.dorandoran.auth",
    "com.dorandoran.shared",
    "com.dorandoran.common",
    "com.dorandoran.infra.persistence"
})
@EnableJpaRepositories(basePackages = "com.dorandoran.infra.persistence.repository")
@EntityScan(basePackages = "com.dorandoran.infra.persistence.entity")
@EnableFeignClients
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
