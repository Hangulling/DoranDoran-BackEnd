package com.dorandoran.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * User 서비스 독립 실행 애플리케이션
 * 포트: 8082
 */
@SpringBootApplication(scanBasePackages = {
    "com.dorandoran.user",
    "com.dorandoran.shared",
    "com.dorandoran.common"
})
@EnableJpaRepositories(basePackages = "com.dorandoran.user.repository")
@EntityScan(basePackages = "com.dorandoran.user.entity")
public class UserApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
