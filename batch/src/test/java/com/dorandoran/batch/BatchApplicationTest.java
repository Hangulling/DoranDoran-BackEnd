package com.dorandoran.batch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Batch 서비스 기본 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
class BatchApplicationTest {

    @Test
    @DisplayName("Batch 애플리케이션 컨텍스트 로드 테스트")
    void contextLoads() {
        // 애플리케이션 컨텍스트가 정상적으로 로드되는지 확인
    }

    @Test
    @DisplayName("Batch 애플리케이션 시작 테스트")
    void applicationStarts() {
        // 애플리케이션이 정상적으로 시작되는지 확인
        // 실제 구현이 완료되면 더 구체적인 테스트 추가 예정
    }
}
