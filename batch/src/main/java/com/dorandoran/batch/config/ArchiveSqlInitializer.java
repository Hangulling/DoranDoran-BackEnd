package com.dorandoran.batch.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

/**
 * Archive SQL 함수 초기화
 * JAR 실행 시 SQL 함수를 자동으로 생성/업데이트
 */
@Slf4j
@Component
public class ArchiveSqlInitializer implements ApplicationRunner {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Archive SQL 함수 초기화 시작");
        
        try {
            // SQL 함수 스크립트 읽기
            ClassPathResource resource = new ClassPathResource("db/archive/archive_functions.sql");
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            
            // SQL 실행 (세미콜론으로 분리)
            String[] statements = sql.split(";");
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    try {
                        jdbcTemplate.execute(trimmed);
                        log.debug("SQL 실행 완료: {}", trimmed.substring(0, Math.min(50, trimmed.length())));
                    } catch (Exception e) {
                        log.warn("SQL 실행 실패 (무시 가능): {}", e.getMessage());
                    }
                }
            }
            
            log.info("✅ Archive SQL 함수 초기화 완료");
        } catch (Exception e) {
            log.error("❌ Archive SQL 함수 초기화 실패", e);
            throw e;
        }
    }
}


