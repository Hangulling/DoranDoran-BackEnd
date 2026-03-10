# 사용자 완전 삭제 배치 작업 설계

> **작성일**: 2026-01-28  
> **상태**: 설계 완료, 구현 대기

---

## 목차

1. [개요](#개요)
2. [배치 작업 설계](#배치-작업-설계)
3. [구현 계획](#구현-계획)
4. [설정 및 실행](#설정-및-실행)
5. [모니터링 및 알림](#모니터링-및-알림)

---

## 개요

### 목적

INACTIVE 상태로 일정 기간(예: 30일) 유지된 사용자를 자동으로 완전 삭제하는 배치 작업입니다.

### 비즈니스 요구사항

- **대상**: INACTIVE 상태로 30일 이상 유지된 사용자
- **처리**: 완전 삭제 (물리적 DELETE)
- **보존**: 채팅 기록은 아카이브에 보존
- **실행 주기**: 주 1회 (일요일 새벽 3시)

---

## 배치 작업 설계

### 전체 플로우

```
[배치 작업 시작]
    ↓
[1. 삭제 대상 사용자 조회]
    ├─ INACTIVE 상태
    ├─ inactive_at이 30일 이전
    └─ ADMIN 역할 제외 (선택)
    ↓
[2. 각 사용자별 처리]
    ├─ [2-1. 아카이브]
    │   ├─ 채팅방 ID 목록 조회
    │   ├─ 각 채팅방 아카이브
    │   └─ 메시지 아카이브
    │
    ├─ [2-2. 완전 삭제]
    │   └─ UserService.hardDeleteUser() 호출
    │
    └─ [2-3. 결과 기록]
        ├─ 성공/실패 로깅
        └─ 통계 업데이트
    ↓
[3. 배치 작업 완료]
    ├─ 처리 결과 리포트 생성
    └─ 알림 발송 (필요시)
```

### 단계별 상세 설계

#### **단계 1: 삭제 대상 사용자 조회**

**SQL 쿼리**:
```sql
SELECT id, email, inactive_at
FROM user_schema.app_user
WHERE status = 'INACTIVE'
    AND inactive_at IS NOT NULL
    AND inactive_at < NOW() - INTERVAL '30 days'
    AND role != 'ROLE_ADMIN'  -- ADMIN 제외 (설정 가능)
ORDER BY inactive_at ASC
LIMIT 1000  -- 배치 크기 제한
```

**제외 조건**:
- ADMIN 역할 사용자 (설정 가능)
- 최근 결제한 사용자 (선택)
- 특정 플래그가 있는 사용자 (선택)

#### **단계 2: 각 사용자별 처리**

**트랜잭션 전략**:
- 각 사용자별로 독립적인 트랜잭션
- 실패해도 다음 사용자 계속 처리
- 실패한 사용자는 로그에 기록

**에러 처리**:
- 아카이브 실패 → 해당 사용자 스킵, 다음 사용자 계속
- 삭제 실패 → 해당 사용자 스킵, 다음 사용자 계속
- 전체 실패 시 → 배치 작업 중단, 알림 발송

#### **단계 3: 결과 기록**

**통계**:
- 처리된 사용자 수
- 성공한 사용자 수
- 실패한 사용자 수
- 실패한 사용자 ID 목록

**로그**:
- 배치 작업 시작/완료 시간
- 각 사용자별 처리 결과
- 실패 원인 상세 로그

---

## 구현 계획

### 1. UserDeletionService

**파일**: `batch/src/main/java/com/dorandoran/batch/service/UserDeletionService.java`

```java
package com.dorandoran.batch.service;

import com.dorandoran.batch.client.UserServiceClient;
import com.dorandoran.batch.client.ChatServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 사용자 완전 삭제 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeletionService {
    
    private final JdbcTemplate jdbcTemplate;
    private final UserServiceClient userServiceClient;
    private final ChatServiceClient chatServiceClient;
    private final ArchiveService archiveService;
    
    /**
     * INACTIVE 상태로 일정 기간 유지된 사용자를 완전 삭제
     * 
     * @param daysInactive INACTIVE 상태로 유지된 일수 (기본값: 30일)
     * @param excludeAdmin ADMIN 역할 사용자 제외 여부 (기본값: true)
     * @param batchSize 한 번에 처리할 최대 사용자 수 (기본값: 1000)
     * @return 삭제 결과 통계
     */
    @Transactional
    public DeletionResult deleteInactiveUsers(int daysInactive, boolean excludeAdmin, int batchSize) {
        log.info("비활성 사용자 완전 삭제 시작: daysInactive={}, excludeAdmin={}, batchSize={}", 
                daysInactive, excludeAdmin, batchSize);
        
        // 1. 삭제 대상 사용자 조회
        List<UUID> targetUserIds = findInactiveUsersForDeletion(daysInactive, excludeAdmin, batchSize);
        log.info("삭제 대상 사용자 수: {}", targetUserIds.size());
        
        if (targetUserIds.isEmpty()) {
            log.info("삭제 대상 사용자가 없습니다.");
            return DeletionResult.empty();
        }
        
        int successCount = 0;
        int failedCount = 0;
        List<UUID> failedUserIds = new ArrayList<>();
        
        // 2. 각 사용자에 대해 완전 삭제 수행
        for (UUID userId : targetUserIds) {
            try {
                deleteUserSafely(userId);
                successCount++;
                log.info("사용자 완전 삭제 성공: userId={}", userId);
            } catch (Exception e) {
                failedCount++;
                failedUserIds.add(userId);
                log.error("사용자 완전 삭제 실패: userId={}, error={}", userId, e.getMessage(), e);
                // 실패해도 다음 사용자 계속 처리
            }
        }
        
        log.info("비활성 사용자 완전 삭제 완료: 성공={}, 실패={}", successCount, failedCount);
        
        return DeletionResult.of(targetUserIds.size(), successCount, failedCount, failedUserIds);
    }
    
    /**
     * 삭제 대상 사용자 조회
     */
    private List<UUID> findInactiveUsersForDeletion(int daysInactive, boolean excludeAdmin, int batchSize) {
        String sql = """
            SELECT id
            FROM user_schema.app_user
            WHERE status = 'INACTIVE'
                AND inactive_at IS NOT NULL
                AND inactive_at < NOW() - INTERVAL ? || ' days'
            """ + (excludeAdmin ? " AND role != 'ROLE_ADMIN'" : "") + """
            ORDER BY inactive_at ASC
            LIMIT ?
            """;
        
        return jdbcTemplate.queryForList(sql, UUID.class, daysInactive, batchSize);
    }
    
    /**
     * 안전한 사용자 삭제 (아카이브 포함)
     */
    @Transactional
    public void deleteUserSafely(UUID userId) {
        log.info("사용자 안전 삭제 시작: userId={}", userId);
        
        // 1. 아카이브 (채팅 기록 보존)
        archiveUserChatrooms(userId);
        
        // 2. 완전 삭제 (UserService API 호출)
        userServiceClient.hardDeleteUser(userId);
        
        log.info("사용자 안전 삭제 완료: userId={}", userId);
    }
    
    /**
     * 해당 유저의 모든 채팅방을 아카이브에 보존
     */
    private void archiveUserChatrooms(UUID userId) {
        log.info("채팅방 아카이브 시작: userId={}", userId);
        
        // Chat 서비스에서 채팅방 ID 목록 조회
        List<UUID> chatroomIds = chatServiceClient.getUserChatroomIds(userId);
        
        if (chatroomIds.isEmpty()) {
            log.info("아카이브할 채팅방이 없음: userId={}", userId);
            return;
        }
        
        int archivedCount = 0;
        int failedCount = 0;
        
        for (UUID chatroomId : chatroomIds) {
            try {
                UUID archChatroomId = archiveService.archiveChatroom(chatroomId);
                if (archChatroomId != null) {
                    archiveService.archiveMessages(chatroomId, archChatroomId);
                    archivedCount++;
                    log.debug("채팅방 아카이브 완료: chatroomId={}, archChatroomId={}", chatroomId, archChatroomId);
                } else {
                    failedCount++;
                    log.warn("채팅방 아카이브 실패 (null 반환): chatroomId={}", chatroomId);
                }
            } catch (Exception e) {
                failedCount++;
                log.error("채팅방 아카이브 실패: chatroomId={}, error={}", chatroomId, e.getMessage(), e);
                throw new RuntimeException("채팅방 아카이브 실패: " + e.getMessage(), e);
            }
        }
        
        log.info("채팅방 아카이브 완료: userId={}, 성공={}, 실패={}", userId, archivedCount, failedCount);
        
        if (failedCount > 0) {
            throw new RuntimeException("일부 채팅방 아카이브 실패: failedCount=" + failedCount);
        }
    }
    
    /**
     * 삭제 결과 통계
     */
    public static class DeletionResult {
        private final int totalCount;
        private final int successCount;
        private final int failedCount;
        private final List<UUID> failedUserIds;
        
        private DeletionResult(int totalCount, int successCount, int failedCount, List<UUID> failedUserIds) {
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.failedUserIds = failedUserIds;
        }
        
        public static DeletionResult empty() {
            return new DeletionResult(0, 0, 0, List.of());
        }
        
        public static DeletionResult of(int totalCount, int successCount, int failedCount, List<UUID> failedUserIds) {
            return new DeletionResult(totalCount, successCount, failedCount, failedUserIds);
        }
        
        // Getters
        public int getTotalCount() { return totalCount; }
        public int getSuccessCount() { return successCount; }
        public int getFailedCount() { return failedCount; }
        public List<UUID> getFailedUserIds() { return failedUserIds; }
    }
}
```

### 2. UserDeletionJob

**파일**: `batch/src/main/java/com/dorandoran/batch/job/UserDeletionJob.java`

```java
package com.dorandoran.batch.job;

import com.dorandoran.batch.service.UserDeletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 사용자 완전 삭제 배치 작업
 * 
 * 실행 조건:
 * --user-deletion.enabled=true
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "user-deletion.enabled", havingValue = "true", matchIfMissing = false)
public class UserDeletionJob implements CommandLineRunner {
    
    private final UserDeletionService userDeletionService;
    
    @Value("${user-deletion.days-inactive:30}")
    private int daysInactive;
    
    @Value("${user-deletion.exclude-admin:true}")
    private boolean excludeAdmin;
    
    @Value("${user-deletion.batch-size:1000}")
    private int batchSize;
    
    @Override
    public void run(String... args) {
        log.info("=== User Deletion Job 시작 ===");
        log.info("설정: daysInactive={}, excludeAdmin={}, batchSize={}", 
                daysInactive, excludeAdmin, batchSize);
        
        long startTime = System.currentTimeMillis();
        
        try {
            UserDeletionService.DeletionResult result = 
                userDeletionService.deleteInactiveUsers(daysInactive, excludeAdmin, batchSize);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            log.info("=== User Deletion Job 완료 ===");
            log.info("처리 결과: 전체={}, 성공={}, 실패={}", 
                    result.getTotalCount(), result.getSuccessCount(), result.getFailedCount());
            log.info("실행 시간: {}ms ({}초)", duration, duration / 1000.0);
            
            if (!result.getFailedUserIds().isEmpty()) {
                log.warn("실패한 사용자 ID 목록: {}", result.getFailedUserIds());
            }
            
        } catch (Exception e) {
            log.error("=== User Deletion Job 실패 ===", e);
            throw e;
        }
    }
}
```

### 3. 설정 파일

**파일**: `batch/src/main/resources/application.yml`

```yaml
user-deletion:
  enabled: false  # 기본값: false (수동 실행)
  days-inactive: 30  # INACTIVE 상태로 30일 이상 유지된 사용자 삭제
  exclude-admin: true  # ADMIN 역할 사용자 제외
  batch-size: 1000  # 한 번에 처리할 최대 사용자 수
```

### 4. 클라이언트 인터페이스

**파일**: `batch/src/main/java/com/dorandoran/batch/client/UserServiceClient.java`

```java
package com.dorandoran.batch.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * User Service 클라이언트
 */
@FeignClient(name = "user-service", url = "${user.service.url:http://dorandoran-user:8082}")
public interface UserServiceClient {
    
    /**
     * 사용자 완전 삭제
     */
    @DeleteMapping("/api/users/{userId}/hard")
    void hardDeleteUser(@PathVariable("userId") String userId);
}
```

**파일**: `batch/src/main/java/com/dorandoran/batch/client/ChatServiceClient.java`

```java
package com.dorandoran.batch.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

/**
 * Chat Service 클라이언트
 */
@FeignClient(name = "chat-service", url = "${chat.service.url:http://dorandoran-chat:8083}")
public interface ChatServiceClient {
    
    /**
     * 해당 유저의 모든 채팅방 ID 목록 조회
     */
    @GetMapping("/api/chatrooms/user/{userId}/ids")
    List<UUID> getUserChatroomIds(@PathVariable("userId") String userId);
}
```

---

## 설정 및 실행

### 1. 단독 실행

```bash
java -jar batch.jar \
  --user-deletion.enabled=true \
  --user-deletion.days-inactive=30 \
  --user-deletion.exclude-admin=true \
  --user-deletion.batch-size=1000
```

### 2. 스크립트 실행

**파일**: `scripts/batch/user-deletion.sh`

```bash
#!/bin/bash

# 사용자 완전 삭제 배치 작업 실행 스크립트

JAR_PATH="/home/ec2-user/batch.jar"
LOG_DIR="/home/ec2-user/logs"
LOG_FILE="${LOG_DIR}/user-deletion-$(date +%Y%m%d-%H%M%S).log"

mkdir -p "${LOG_DIR}"

echo "=== User Deletion Job 시작: $(date) ===" | tee -a "${LOG_FILE}"

java -jar "${JAR_PATH}" \
  --user-deletion.enabled=true \
  --user-deletion.days-inactive=30 \
  --user-deletion.exclude-admin=true \
  --user-deletion.batch-size=1000 \
  2>&1 | tee -a "${LOG_FILE}"

EXIT_CODE=${PIPESTATUS[0]}

echo "=== User Deletion Job 완료: $(date), Exit Code: ${EXIT_CODE} ===" | tee -a "${LOG_FILE}"

exit ${EXIT_CODE}
```

### 3. Cron 설정 (선택)

```bash
# 매주 일요일 새벽 3시 실행
0 3 * * 0 /home/ec2-user/scripts/batch/user-deletion.sh
```

---

## 모니터링 및 알림

### 주요 메트릭

- 처리된 사용자 수
- 성공한 사용자 수
- 실패한 사용자 수
- 실행 시간
- 마지막 실행 시간
- 아카이브 성공률

### 로그 예시

```
[2026-01-28 03:00:00] === User Deletion Job 시작 ===
[2026-01-28 03:00:00] 설정: daysInactive=30, excludeAdmin=true, batchSize=1000
[2026-01-28 03:00:01] 삭제 대상 사용자 수: 15
[2026-01-28 03:00:02] 채팅방 아카이브 시작: userId=xxx-xxx-xxx
[2026-01-28 03:00:05] 채팅방 아카이브 완료: userId=xxx-xxx-xxx, 성공=3, 실패=0
[2026-01-28 03:00:06] 사용자 완전 삭제 성공: userId=xxx-xxx-xxx
...
[2026-01-28 03:00:30] === User Deletion Job 완료 ===
[2026-01-28 03:00:30] 처리 결과: 전체=15, 성공=15, 실패=0
[2026-01-28 03:00:30] 실행 시간: 30000ms (30.0초)
```

### 알림 설정 (선택)

- 실패한 사용자가 있을 경우 알림 발송
- 배치 작업 실패 시 알림 발송
- 처리 결과 리포트 이메일 발송

---

## 주의사항

### 1. 데이터 복구 불가능성

- 완전 삭제된 사용자 데이터는 운영 DB에서 복구 불가능
- 아카이브에 보존된 데이터만 복구 가능
- **반드시 아카이브 완료 후 삭제 진행**

### 2. 대량 삭제 시 성능

- 많은 채팅방을 가진 사용자의 경우 아카이브 시간이 오래 걸릴 수 있음
- 배치 작업 시 일괄 처리로 인한 DB 부하 가능
- **권장**: 배치 작업은 비피크 시간대에 실행

### 3. 법적 요구사항

- GDPR 등 개인정보 보호 규정 준수 필요
- 사용자 동의 확인 필요
- 삭제 전 알림 발송 여부 검토 필요

---

## 다음 단계

1. **Chat 서비스 API 구현**: `/api/chatrooms/user/{userId}/ids` 엔드포인트 추가
2. **User 서비스 API 구현**: `/api/users/{userId}/hard` 엔드포인트 구현 완료
3. **Batch 서비스 구현**: `UserDeletionService` 및 `UserDeletionJob` 구현
4. **테스트**: 단위 테스트 및 통합 테스트
5. **모니터링 설정**: 삭제 작업 모니터링 및 알림 설정
