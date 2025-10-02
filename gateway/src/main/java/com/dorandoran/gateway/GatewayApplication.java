package com.dorandoran.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway 애플리케이션
 * 포트: 8080
 */
@SpringBootApplication(scanBasePackages = {
    "com.dorandoran.gateway",
    "com.dorandoran.shared",
    "com.dorandoran.common"
}, exclude = {
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
