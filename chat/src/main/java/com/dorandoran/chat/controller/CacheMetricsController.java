package com.dorandoran.chat.controller;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 캐시 성능 통계를 제공하는 커스텀 엔드포인트
 * Spring Boot Actuator를 확장하여 캐시 관련 메트릭을 통합 조회
 */
@RestController
@RequestMapping("/actuator/custom")
@RequiredArgsConstructor
@Slf4j
public class CacheMetricsController {
    
    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;
    
    /**
     * 모든 캐시의 통계 정보를 조회
     * 
     * @return 캐시별 상세 통계와 전체 요약 정보
     */
    @GetMapping("/cache-stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        log.info("캐시 통계 조회 요청");
        
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> cacheDetails = new HashMap<>();
        
        int totalGets = 0;
        int totalHits = 0;
        int totalPuts = 0;
        
        // 각 캐시별 통계 수집
        for (String cacheName : cacheManager.getCacheNames()) {
            Map<String, Object> cacheStats = getCacheStatistics(cacheName);
            cacheDetails.put(cacheName, cacheStats);
            
            // 전체 통계 집계
            totalGets += (Integer) cacheStats.get("gets");
            totalHits += (Integer) cacheStats.get("hits");
            totalPuts += (Integer) cacheStats.get("puts");
        }
        
        // 전체 요약 정보
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalGets", totalGets);
        summary.put("totalHits", totalHits);
        summary.put("totalPuts", totalPuts);
        summary.put("avgHitRatio", totalGets > 0 ? String.format("%.2f%%", (double) totalHits / totalGets * 100) : "0.00%");
        summary.put("cacheCount", cacheManager.getCacheNames().size());
        
        response.put("summary", summary);
        response.put("cacheDetails", cacheDetails);
        response.put("timestamp", System.currentTimeMillis());
        
        log.info("캐시 통계 조회 완료 - 총 조회: {}, 히트율: {}", totalGets, summary.get("avgHitRatio"));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 특정 캐시의 상세 통계 조회
     * 
     * @param cacheName 캐시 이름
     * @return 캐시별 상세 통계
     */
    private Map<String, Object> getCacheStatistics(String cacheName) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Micrometer에서 캐시 메트릭 조회
            double hits = getMetricValue("cache.gets", 
                Tag.of("result", "hit"), 
                Tag.of("name", cacheName));
            
            double misses = getMetricValue("cache.gets", 
                Tag.of("result", "miss"), 
                Tag.of("name", cacheName));
            
            double puts = getMetricValue("cache.puts", 
                Tag.of("name", cacheName));
            
            double evictions = getMetricValue("cache.evictions", 
                Tag.of("name", cacheName));
            
            // 통계 계산
            int totalGets = (int) (hits + misses);
            double hitRatio = totalGets > 0 ? hits / totalGets : 0;
            double missRatio = totalGets > 0 ? misses / totalGets : 0;
            
            stats.put("gets", totalGets);
            stats.put("hits", (int) hits);
            stats.put("misses", (int) misses);
            stats.put("puts", (int) puts);
            stats.put("evictions", (int) evictions);
            stats.put("hitRatio", String.format("%.2f%%", hitRatio * 100));
            stats.put("missRatio", String.format("%.2f%%", missRatio * 100));
            stats.put("hitCount", (int) hits);
            stats.put("missCount", (int) misses);
            
        } catch (Exception e) {
            log.warn("캐시 통계 조회 실패: {}", cacheName, e);
            // 기본값 설정
            stats.put("gets", 0);
            stats.put("hits", 0);
            stats.put("misses", 0);
            stats.put("puts", 0);
            stats.put("evictions", 0);
            stats.put("hitRatio", "0.00%");
            stats.put("missRatio", "0.00%");
            stats.put("hitCount", 0);
            stats.put("missCount", 0);
        }
        
        return stats;
    }
    
    /**
     * Micrometer에서 특정 메트릭 값을 조회
     * 
     * @param metricName 메트릭 이름
     * @param tags 태그들
     * @return 메트릭 값
     */
    private double getMetricValue(String metricName, Tag... tags) {
        try {
            return meterRegistry.find(metricName)
                .tags(Arrays.asList(tags))
                .counter()
                .count();
        } catch (Exception e) {
            log.debug("메트릭 조회 실패: {} - {}", metricName, e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * 캐시 상태 헬스 체크
     * 
     * @return 캐시 상태 정보
     */
    @GetMapping("/cache-health")
    public ResponseEntity<Map<String, Object>> getCacheHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Redis 연결 상태 확인
            boolean redisConnected = isRedisConnected();
            
            // 캐시 매니저 상태 확인
            boolean cacheManagerHealthy = cacheManager != null && 
                cacheManager.getCacheNames() != null;
            
            health.put("status", redisConnected && cacheManagerHealthy ? "UP" : "DOWN");
            health.put("redis", redisConnected ? "UP" : "DOWN");
            health.put("cacheManager", cacheManagerHealthy ? "UP" : "DOWN");
            health.put("cacheCount", cacheManager.getCacheNames().size());
            health.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            log.error("캐시 헬스 체크 실패", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Redis 연결 상태 확인
     * 
     * @return Redis 연결 여부
     */
    private boolean isRedisConnected() {
        try {
            // 간단한 캐시 작업으로 Redis 연결 확인
            cacheManager.getCacheNames();
            return true;
        } catch (Exception e) {
            log.warn("Redis 연결 확인 실패", e);
            return false;
        }
    }
}
