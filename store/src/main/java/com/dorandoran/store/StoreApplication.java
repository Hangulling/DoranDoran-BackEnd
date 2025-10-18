package com.dorandoran.store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Store Service Application
 * 보관함 서비스 - 사용자가 저장한 표현 관리
 */
@SpringBootApplication
@EnableFeignClients // Feign Client 활성화 (Chat Service 호출용)
@EnableJpaAuditing  // JPA Auditing 활성화 (CreatedDate, LastModifiedDate 자동 관리)
public class StoreApplication {

  /**
   * Store Service 시작점
   *
   * @param args 커맨드 라인 인자
   */
  public static void main(String[] args) {
    SpringApplication.run(StoreApplication.class, args);
  }
}