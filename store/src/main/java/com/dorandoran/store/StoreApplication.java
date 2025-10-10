package com.dorandoran.store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Store Service Application
 * 보관함 서비스 - 사용자가 저장한 표현 관리
 */
@SpringBootApplication
@EnableFeignClients
@EnableJpaRepositories
public class StoreApplication {
  public static void main(String[] args) {
    SpringApplication.run(StoreApplication.class, args);
  }
}