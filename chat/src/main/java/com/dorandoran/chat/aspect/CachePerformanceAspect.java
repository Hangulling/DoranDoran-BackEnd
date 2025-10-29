package com.dorandoran.chat.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Redis 캐시 성능 측정을 위한 AOP Aspect
 * @Cacheable 어노테이션이 적용된 메서드의 실행 시간을 측정하고 로깅
 */
@Aspect
@Component
@Slf4j
public class CachePerformanceAspect {
    
    /**
     * @Cacheable 메서드의 성능을 측정하고 로깅
     * 
     * @param joinPoint AOP 조인 포인트
     * @param cacheable @Cacheable 어노테이션
     * @return 메서드 실행 결과
     * @throws Throwable 메서드 실행 중 발생한 예외
     */
    @Around("@annotation(cacheable)")
    public Object logCachePerformance(ProceedingJoinPoint joinPoint, Cacheable cacheable) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();
        
        long startTime = System.currentTimeMillis();
        long startNanoTime = System.nanoTime();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            long executionNanoTime = System.nanoTime() - startNanoTime;
            
            // 실행 시간으로 캐시 히트/미스 추정 (10ms 미만이면 캐시 히트로 추정)
            String cacheStatus = executionTime < 10 ? "CACHE-HIT" : "CACHE-MISS";
            String cacheName = cacheable.value().length > 0 ? cacheable.value()[0] : "unknown";
            
            // 상세 로깅
            logCachePerformance(methodName, className, args, cacheStatus, cacheName, 
                              executionTime, executionNanoTime, result != null);
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[CACHE-ERROR] {}.{}({}) - {}ms - Error: {}", 
                     className, methodName, formatArgs(args), executionTime, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 캐시 성능 로깅
     * 
     * @param methodName 메서드명
     * @param className 클래스명
     * @param args 메서드 인자
     * @param cacheStatus 캐시 상태 (HIT/MISS)
     * @param cacheName 캐시 이름
     * @param executionTime 실행 시간 (밀리초)
     * @param executionNanoTime 실행 시간 (나노초)
     * @param hasResult 결과 존재 여부
     */
    private void logCachePerformance(String methodName, String className, Object[] args, 
                                   String cacheStatus, String cacheName, 
                                   long executionTime, long executionNanoTime, boolean hasResult) {
        
        // 기본 로깅 (INFO 레벨)
        log.info("[{}] {}.{}({}) - {}ms - Cache: {}", 
                cacheStatus, className, methodName, formatArgs(args), executionTime, cacheName);
        
        // 상세 로깅 (DEBUG 레벨)
        if (log.isDebugEnabled()) {
            log.debug("[{}] 상세 정보 - 메서드: {}.{}, 인자: {}, 캐시: {}, 실행시간: {}ms ({}ns), 결과존재: {}", 
                     cacheStatus, className, methodName, formatArgs(args), cacheName, 
                     executionTime, executionNanoTime, hasResult);
        }
        
        // 성능 경고 로깅 (WARN 레벨)
        if (executionTime > 100) {
            log.warn("[PERFORMANCE-WARNING] 느린 캐시 작업 감지 - {}.{}({}) - {}ms - Cache: {}", 
                    className, methodName, formatArgs(args), executionTime, cacheName);
        }
        
        // 캐시 미스 시 추가 정보 로깅
        if ("CACHE-MISS".equals(cacheStatus)) {
            log.info("[CACHE-MISS] 캐시 저장됨 - {}.{}({}) - {}ms - Cache: {}", 
                    className, methodName, formatArgs(args), executionTime, cacheName);
        }
    }
    
    /**
     * 메서드 인자를 문자열로 포맷팅
     * 
     * @param args 메서드 인자 배열
     * @return 포맷팅된 인자 문자열
     */
    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        
        return Arrays.stream(args)
                .map(this::formatArg)
                .collect(Collectors.joining(", "));
    }
    
    /**
     * 개별 인자를 문자열로 포맷팅
     * 
     * @param arg 인자 객체
     * @return 포맷팅된 문자열
     */
    private String formatArg(Object arg) {
        if (arg == null) {
            return "null";
        }
        
        // UUID는 전체 표시
        if (arg instanceof java.util.UUID) {
            return arg.toString();
        }
        
        // 문자열은 길이 제한
        if (arg instanceof String) {
            String str = (String) arg;
            return str.length() > 50 ? str.substring(0, 50) + "..." : str;
        }
        
        // 기타 객체는 클래스명과 해시코드
        return arg.getClass().getSimpleName() + "@" + Integer.toHexString(arg.hashCode());
    }
    
    /**
     * 캐시 성능 통계를 위한 메트릭 수집
     * (향후 확장 가능)
     */
    public static class CacheMetrics {
        private static final java.util.concurrent.atomic.AtomicLong totalHits = new java.util.concurrent.atomic.AtomicLong(0);
        private static final java.util.concurrent.atomic.AtomicLong totalMisses = new java.util.concurrent.atomic.AtomicLong(0);
        private static final java.util.concurrent.atomic.AtomicLong totalExecutionTime = new java.util.concurrent.atomic.AtomicLong(0);
        
        public static void recordHit(long executionTime) {
            totalHits.incrementAndGet();
            totalExecutionTime.addAndGet(executionTime);
        }
        
        public static void recordMiss(long executionTime) {
            totalMisses.incrementAndGet();
            totalExecutionTime.addAndGet(executionTime);
        }
        
        public static double getHitRatio() {
            long hits = totalHits.get();
            long misses = totalMisses.get();
            long total = hits + misses;
            return total > 0 ? (double) hits / total : 0.0;
        }
        
        public static long getAverageExecutionTime() {
            long total = totalHits.get() + totalMisses.get();
            return total > 0 ? totalExecutionTime.get() / total : 0;
        }
        
        public static void reset() {
            totalHits.set(0);
            totalMisses.set(0);
            totalExecutionTime.set(0);
        }
    }
}
