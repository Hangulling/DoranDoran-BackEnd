package com.dorandoran.batch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Batch 서비스 스케줄러 설정
 * Spring의 @Scheduled 어노테이션을 활성화합니다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
