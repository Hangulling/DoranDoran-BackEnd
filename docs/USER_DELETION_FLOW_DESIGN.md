# 사용자 완전 삭제 플로우 설계안

> **작성일**: 2026-01-28  
> **상태**: 설계 완료, 구현 대기

---

## 목차

1. [개요](#개요)
2. [현재 상태 분석](#현재-상태-분석)
3. [완전 삭제 플로우 설계](#완전-삭제-플로우-설계)
4. [FK 제약 조건 변경 계획](#fk-제약-조건-변경-계획)
5. [마이그레이션 스크립트](#마이그레이션-스크립트)
6. [구현 계획](#구현-계획)
7. [배치 작업 설계](#배치-작업-설계)
8. [주의사항 및 리스크](#주의사항-및-리스크)

---

## 개요

### 목적

사용자 탈퇴 시 **소프트 삭제(INACTIVE 상태 변경)** 대신 **완전 삭제(물리적 DELETE)**를 수행하되, 
**채팅 기록 등 중요한 데이터는 아카이브에 보존**하는 안전한 삭제 플로우를 설계합니다.

### 핵심 원칙

1. **데이터 보존**: 채팅 기록은 반드시 아카이브에 보존
2. **안전한 삭제**: FK 제약 조건을 고려한 순차적 삭제
3. **원자성**: 트랜잭션으로 전체 플로우의 일관성 보장
4. **복구 가능성**: 아카이브를 통한 데이터 복구 가능

---

## 현재 상태 분석

### 현재 구현 (소프트 삭제)

```java
// UserService.deleteUser()
user.updateStatus(User.UserStatus.INACTIVE);
userRepository.save(user);
```

- **동작**: `status`를 `INACTIVE`로 변경만 수행
- **데이터**: 모든 데이터가 운영 DB에 그대로 남아있음
- **문제점**: 
  - 개인정보 보호 규정(GDPR 등) 준수 어려움
  - 장기간 데이터 누적
  - 실제 삭제를 원하는 사용자 요구 미충족

### FK 제약 조건 현황

#### 1. **ON DELETE CASCADE** (자동 삭제됨)
- `chat_schema.chatrooms.user_id` → `user_schema.app_user(id)`
- `chat_schema.messages.chatroom_id` → `chat_schema.chatrooms(id)` (간접 삭제)
- `chat_schema.intimacy_progress.user_id` → `user_schema.app_user(id)`
- `chat_schema.intimacy_progress.chatroom_id` → `chat_schema.chatrooms(id)` (간접 삭제)

**영향**: 유저 삭제 시 **모든 채팅방, 메시지, 친밀도 기록이 운영 DB에서 즉시 삭제됨**

#### 2. **ON DELETE SET NULL** (user_id만 NULL로 변경)
- `billing.ai_usage_events.user_id` → `user_schema.app_user(id)`
- `chat_schema.chatbots.created_by` → `user_schema.app_user(id)`

**영향**: 유저 삭제 시 **과금 기록과 챗봇은 남지만, user_id/created_by만 NULL**

#### 3. **ON DELETE 없음 (RESTRICT)** (삭제 차단)
- `auth_schema.refresh_tokens.user_id` → `user_schema.app_user(id)`
- `auth_schema.email_verifications.user_id` → `user_schema.app_user(id)`
- `auth_schema.login_attempts.user_id` → `user_schema.app_user(id)`
- `auth_schema.password_reset_tokens.user_id` → `user_schema.app_user(id)`
- `auth_schema.auth_events.user_id` → `user_schema.app_user(id)`
- `user_schema.profiles.user_id` → `user_schema.app_user(id)`
- `user_schema.settings.user_id` → `user_schema.app_user(id)`

**영향**: 유저 삭제 전에 **이 테이블들의 데이터를 먼저 정리해야 함**

---

## 완전 삭제 플로우 설계

### 전체 플로우 다이어그램

```
[탈퇴 요청]
    ↓
[1. 아카이브 단계]
    ├─ 해당 유저의 모든 채팅방 조회
    ├─ 각 채팅방을 archive_schema로 아카이빙
    └─ 아카이브 완료 확인
    ↓
[2. Auth 스키마 정리]
    ├─ refresh_tokens 삭제
    ├─ email_verifications 삭제
    ├─ login_attempts 삭제
    ├─ password_reset_tokens 삭제
    └─ auth_events 삭제
    ↓
[3. User 스키마 정리]
    ├─ profiles 삭제
    ├─ settings 삭제
    └─ 기타 user_schema 내 연관 데이터 삭제
    ↓
[4. User 삭제]
    └─ app_user DELETE (CASCADE로 chatrooms, messages, intimacy_progress 자동 삭제)
    ↓
[완료]
```

### 단계별 상세 설계

#### **단계 1: 아카이브 (채팅 기록 보존)**

**목적**: 채팅 기록을 운영 DB에서 삭제하기 전에 아카이브에 보존

**프로세스**:
1. 해당 유저의 모든 채팅방 조회 (`chat_schema.chatrooms WHERE user_id = ?`)
2. 각 채팅방에 대해 `ArchiveService.archiveChatroom()` 호출
3. 아카이브 완료 확인

**주의사항**:
- 아카이브 실패 시 전체 플로우 중단 (트랜잭션 롤백)
- 아카이브 완료 후 운영 DB에서 삭제 진행

**코드 위치**: `batch/src/main/java/com/dorandoran/batch/service/ArchiveService.java`

#### **단계 2: Auth 스키마 정리**

**목적**: FK 제약 조건으로 인한 삭제 실패 방지

**삭제 대상**:
- `auth_schema.refresh_tokens` (WHERE user_id = ?)
- `auth_schema.email_verifications` (WHERE user_id = ?)
- `auth_schema.login_attempts` (WHERE user_id = ?)
- `auth_schema.password_reset_tokens` (WHERE user_id = ?)
- `auth_schema.auth_events` (WHERE user_id = ?)

**처리 방식**: 
- 단순 DELETE 쿼리 실행
- 실패 시 전체 플로우 중단

#### **단계 3: User 스키마 정리**

**목적**: FK 제약 조건으로 인한 삭제 실패 방지

**삭제 대상**:
- `user_schema.profiles` (WHERE user_id = ?)
- `user_schema.settings` (WHERE user_id = ?)
- 기타 user_schema 내 연관 데이터 (필요시)

**처리 방식**:
- JPA 엔티티를 통한 삭제 또는 직접 SQL 실행
- 실패 시 전체 플로우 중단

#### **단계 4: User 삭제**

**목적**: 최종적으로 `app_user` 레코드 삭제

**프로세스**:
1. `userRepository.deleteById(userId)` 실행
2. CASCADE로 자동 삭제되는 데이터:
   - `chat_schema.chatrooms` (해당 유저 소유)
   - `chat_schema.messages` (해당 채팅방의 메시지)
   - `chat_schema.intimacy_progress` (해당 유저/채팅방)

**주의사항**:
- 이미 아카이브된 데이터이므로 운영 DB에서 삭제되어도 문제없음
- `billing.ai_usage_events.user_id`는 SET NULL로 자동 처리됨

---

## FK 제약 조건 변경 계획

### 변경 필요성

현재 일부 FK가 `ON DELETE RESTRICT`(기본값)로 설정되어 있어, 
유저 삭제 전에 수동으로 연관 데이터를 정리해야 합니다.

### 변경 전략

#### **옵션 A: FK를 CASCADE/SET NULL로 변경 (권장)**

**장점**:
- 삭제 플로우 단순화
- DB 레벨에서 자동 처리
- 코드 복잡도 감소

**단점**:
- 의도치 않은 데이터 삭제 위험
- FK 변경 시 기존 데이터 영향 검토 필요

#### **옵션 B: 코드에서 수동 정리 후 삭제 (현재 설계)**

**장점**:
- 명시적 제어 가능
- 로깅 및 모니터링 용이
- 안전성 높음

**단점**:
- 코드 복잡도 증가
- 순서 관리 필요

### 권장 변경 사항

#### **ON DELETE CASCADE로 변경 권장**
- `auth_schema.refresh_tokens.user_id` → **CASCADE** (토큰은 유저 삭제 시 함께 삭제되어야 함)
- `auth_schema.email_verifications.user_id` → **CASCADE** (인증 데이터는 유저 삭제 시 불필요)
- `auth_schema.login_attempts.user_id` → **CASCADE** (로그인 시도 기록은 유저 삭제 시 불필요)
- `auth_schema.password_reset_tokens.user_id` → **CASCADE** (비밀번호 재설정 토큰은 유저 삭제 시 불필요)
- `user_schema.profiles.user_id` → **CASCADE** (프로필은 유저와 함께 삭제되어야 함)
- `user_schema.settings.user_id` → **CASCADE** (설정은 유저와 함께 삭제되어야 함)

#### **ON DELETE SET NULL 유지**
- `auth_schema.auth_events.user_id` → **SET NULL** (감사 로그는 보존하되 user_id만 NULL)

**이유**: 감사 로그는 법적 요구사항으로 보존이 필요할 수 있음

---

## 마이그레이션 스크립트

### V6__update_user_deletion_fk_constraints.sql

```sql
-- ============================================
-- 사용자 완전 삭제를 위한 FK 제약 조건 변경
-- ============================================

-- 1. auth_schema.refresh_tokens: ON DELETE CASCADE
ALTER TABLE auth_schema.refresh_tokens
    DROP CONSTRAINT IF EXISTS fk5a9ypl7oycxycfscqnsepj5t8;

ALTER TABLE auth_schema.refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_user 
    FOREIGN KEY (user_id) 
    REFERENCES user_schema.app_user(id) 
    ON DELETE CASCADE;

-- 2. auth_schema.email_verifications: ON DELETE CASCADE
ALTER TABLE auth_schema.email_verifications
    DROP CONSTRAINT IF EXISTS fk5vri8t8tr81le36apgppy94ch;

ALTER TABLE auth_schema.email_verifications
    ADD CONSTRAINT fk_email_verifications_user 
    FOREIGN KEY (user_id) 
    REFERENCES user_schema.app_user(id) 
    ON DELETE CASCADE;

-- 3. auth_schema.login_attempts: ON DELETE CASCADE
ALTER TABLE auth_schema.login_attempts
    DROP CONSTRAINT IF EXISTS fkeix6yqtdy2p9t2vj8hs6ji8of;

ALTER TABLE auth_schema.login_attempts
    ADD CONSTRAINT fk_login_attempts_user 
    FOREIGN KEY (user_id) 
    REFERENCES user_schema.app_user(id) 
    ON DELETE CASCADE;

-- 4. auth_schema.password_reset_tokens: ON DELETE CASCADE
ALTER TABLE auth_schema.password_reset_tokens
    DROP CONSTRAINT IF EXISTS fkj9so57i2ys7gbwiljqyrivrnb;

ALTER TABLE auth_schema.password_reset_tokens
    ADD CONSTRAINT fk_password_reset_tokens_user 
    FOREIGN KEY (user_id) 
    REFERENCES user_schema.app_user(id) 
    ON DELETE CASCADE;

-- 5. auth_schema.auth_events: ON DELETE SET NULL (감사 로그 보존)
ALTER TABLE auth_schema.auth_events
    DROP CONSTRAINT IF EXISTS fkq2wxphtsj555bl7w9yvcr08q8;

ALTER TABLE auth_schema.auth_events
    ADD CONSTRAINT fk_auth_events_user 
    FOREIGN KEY (user_id) 
    REFERENCES user_schema.app_user(id) 
    ON DELETE SET NULL;

-- 6. user_schema.profiles: ON DELETE CASCADE
ALTER TABLE user_schema.profiles
    DROP CONSTRAINT IF EXISTS fko9irkw5uae1s5s10pmstcvipw;

ALTER TABLE user_schema.profiles
    ADD CONSTRAINT fk_profiles_user 
    FOREIGN KEY (user_id) 
    REFERENCES user_schema.app_user(id) 
    ON DELETE CASCADE;

-- 7. user_schema.settings: ON DELETE CASCADE
ALTER TABLE user_schema.settings
    DROP CONSTRAINT IF EXISTS fk5w7p1w60kfsalo61akkmfirv3;

ALTER TABLE user_schema.settings
    ADD CONSTRAINT fk_settings_user 
    FOREIGN KEY (user_id) 
    REFERENCES user_schema.app_user(id) 
    ON DELETE CASCADE;

-- 변경 사항 확인 쿼리
-- SELECT 
--     tc.table_schema,
--     tc.table_name,
--     tc.constraint_name,
--     rc.delete_rule
-- FROM information_schema.table_constraints tc
-- JOIN information_schema.referential_constraints rc 
--     ON tc.constraint_name = rc.constraint_name
-- WHERE tc.constraint_type = 'FOREIGN KEY'
--     AND rc.unique_constraint_name IN (
--         SELECT constraint_name 
--         FROM information_schema.table_constraints 
--         WHERE table_schema = 'user_schema' 
--             AND table_name = 'app_user'
--     )
-- ORDER BY tc.table_schema, tc.table_name;
```

---

## 구현 계획

### 1. UserService에 완전 삭제 메서드 추가

**파일**: `user/src/main/java/com/dorandoran/user/service/UserService.java`

```java
/**
 * 사용자 완전 삭제 (물리적 DELETE)
 * 
 * 플로우:
 * 1. 해당 유저의 모든 채팅방을 아카이브에 보존
 * 2. Auth 스키마 연관 데이터 삭제 (FK 제약 조건 해결)
 * 3. User 스키마 연관 데이터 삭제 (FK 제약 조건 해결)
 * 4. app_user 삭제 (CASCADE로 chatrooms, messages, intimacy_progress 자동 삭제)
 * 
 * @param id 사용자 UUID
 * @throws DoranDoranException 아카이브 실패 또는 삭제 실패 시
 */
@Transactional
public void hardDeleteUser(UUID id) {
    log.info("사용자 완전 삭제 시작: userId={}", id);
    
    // 1. 사용자 존재 확인
    User user = userRepository.findById(id)
        .orElseThrow(() -> new DoranDoranException(ErrorCode.USER_NOT_FOUND));
    
    // 2. 아카이브 단계 (채팅 기록 보존)
    archiveUserChatrooms(id);
    
    // 3. Auth 스키마 정리 (FK 제약 조건 해결)
    cleanupAuthSchemaData(id);
    
    // 4. User 스키마 정리 (FK 제약 조건 해결)
    cleanupUserSchemaData(id);
    
    // 5. User 삭제 (CASCADE로 연관 데이터 자동 삭제)
    userRepository.deleteById(id);
    
    log.info("사용자 완전 삭제 완료: userId={}", id);
}

/**
 * 해당 유저의 모든 채팅방을 아카이브에 보존
 */
private void archiveUserChatrooms(UUID userId) {
    log.info("채팅방 아카이브 시작: userId={}", userId);
    
    // Chat 서비스 호출하여 해당 유저의 모든 채팅방 조회
    // 또는 직접 DB 쿼리로 조회
    List<UUID> chatroomIds = chatServiceClient.getUserChatroomIds(userId);
    
    int archivedCount = 0;
    int failedCount = 0;
    
    for (UUID chatroomId : chatroomIds) {
        try {
            archiveService.archiveChatroom(chatroomId);
            archiveService.archiveMessages(chatroomId, /* archChatroomId */);
            archivedCount++;
            log.debug("채팅방 아카이브 완료: chatroomId={}", chatroomId);
        } catch (Exception e) {
            failedCount++;
            log.error("채팅방 아카이브 실패: chatroomId={}, error={}", chatroomId, e.getMessage(), e);
            throw new DoranDoranException(ErrorCode.INTERNAL_SERVER_ERROR, 
                "채팅방 아카이브 실패: " + e.getMessage());
        }
    }
    
    log.info("채팅방 아카이브 완료: userId={}, 성공={}, 실패={}", userId, archivedCount, failedCount);
}

/**
 * Auth 스키마 연관 데이터 삭제
 */
private void cleanupAuthSchemaData(UUID userId) {
    log.info("Auth 스키마 정리 시작: userId={}", userId);
    
    // FK 제약 조건이 CASCADE로 변경되면 이 메서드는 불필요하지만,
    // 안전을 위해 명시적으로 삭제하는 것도 가능
    
    // refresh_tokens, email_verifications, login_attempts, 
    // password_reset_tokens는 CASCADE로 자동 삭제됨
    // auth_events는 SET NULL로 자동 처리됨
    
    log.info("Auth 스키마 정리 완료: userId={}", userId);
}

/**
 * User 스키마 연관 데이터 삭제
 */
private void cleanupUserSchemaData(UUID userId) {
    log.info("User 스키마 정리 시작: userId={}", userId);
    
    // FK 제약 조건이 CASCADE로 변경되면 이 메서드는 불필요하지만,
    // 안전을 위해 명시적으로 삭제하는 것도 가능
    
    // profiles, settings는 CASCADE로 자동 삭제됨
    
    log.info("User 스키마 정리 완료: userId={}", userId);
}
```

### 2. UserController에 완전 삭제 엔드포인트 추가

**파일**: `user/src/main/java/com/dorandoran/user/controller/UserController.java`

```java
/**
 * 회원탈퇴 (완전 삭제 - 물리적 DELETE)
 * 
 * 주의: 이 작업은 되돌릴 수 없습니다.
 * 채팅 기록은 아카이브에 보존되지만, 운영 DB에서는 완전히 삭제됩니다.
 */
@DeleteMapping("/{userId}/hard")
public ResponseEntity<Void> hardDeleteUser(@PathVariable String userId) {
    log.info("회원탈퇴(완전 삭제) 요청: userId={}", userId);
    
    try {
        userService.hardDeleteUser(UUID.fromString(userId));
        log.info("회원탈퇴(완전 삭제) 완료: userId={}", userId);
        return ResponseEntity.ok().build();
    } catch (IllegalArgumentException e) {
        log.error("잘못된 사용자 ID: userId={}, error={}", userId, e.getMessage());
        return ResponseEntity.badRequest().build();
    } catch (Exception e) {
        log.error("회원탈퇴(완전 삭제) 실패: userId={}, error={}", userId, e.getMessage());
        return ResponseEntity.internalServerError().build();
    }
}
```

### 3. ChatServiceClient에 채팅방 ID 조회 메서드 추가

**파일**: `user/src/main/java/com/dorandoran/user/client/ChatServiceClient.java`

```java
/**
 * 해당 유저의 모든 채팅방 ID 조회
 */
@GetMapping("/api/chatrooms/user/{userId}")
List<UUID> getUserChatroomIds(@PathVariable("userId") String userId);
```

---

## 배치 작업 설계

### 목적

INACTIVE 상태로 일정 기간(예: 30일) 유지된 사용자를 자동으로 완전 삭제하는 배치 작업

### 설계

**파일**: `batch/src/main/java/com/dorandoran/batch/service/UserDeletionService.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDeletionService {
    
    private final UserServiceClient userServiceClient;
    private final ArchiveService archiveService;
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * INACTIVE 상태로 일정 기간 유지된 사용자를 완전 삭제
     * 
     * @param daysInactive INACTIVE 상태로 유지된 일수 (기본값: 30일)
     */
    @Transactional
    public void deleteInactiveUsers(int daysInactive) {
        log.info("비활성 사용자 완전 삭제 시작: daysInactive={}", daysInactive);
        
        // 1. 삭제 대상 사용자 조회
        List<UUID> targetUserIds = findInactiveUsersForDeletion(daysInactive);
        log.info("삭제 대상 사용자 수: {}", targetUserIds.size());
        
        int successCount = 0;
        int failedCount = 0;
        
        // 2. 각 사용자에 대해 완전 삭제 수행
        for (UUID userId : targetUserIds) {
            try {
                deleteUserSafely(userId);
                successCount++;
                log.info("사용자 완전 삭제 성공: userId={}", userId);
            } catch (Exception e) {
                failedCount++;
                log.error("사용자 완전 삭제 실패: userId={}, error={}", userId, e.getMessage(), e);
                // 실패해도 다음 사용자 계속 처리
            }
        }
        
        log.info("비활성 사용자 완전 삭제 완료: 성공={}, 실패={}", successCount, failedCount);
    }
    
    /**
     * 삭제 대상 사용자 조회
     */
    private List<UUID> findInactiveUsersForDeletion(int daysInactive) {
        String sql = """
            SELECT id
            FROM user_schema.app_user
            WHERE status = 'INACTIVE'
                AND inactive_at IS NOT NULL
                AND inactive_at < NOW() - INTERVAL '? days'
            ORDER BY inactive_at ASC
            """;
        
        return jdbcTemplate.queryForList(sql, UUID.class, daysInactive);
    }
    
    /**
     * 안전한 사용자 삭제 (아카이브 포함)
     */
    private void deleteUserSafely(UUID userId) {
        // 1. 아카이브
        archiveUserChatrooms(userId);
        
        // 2. 완전 삭제 (UserService API 호출 또는 직접 SQL)
        userServiceClient.hardDeleteUser(userId);
    }
    
    /**
     * 해당 유저의 모든 채팅방을 아카이브에 보존
     */
    private void archiveUserChatrooms(UUID userId) {
        // Chat 서비스에서 채팅방 ID 목록 조회
        List<UUID> chatroomIds = chatServiceClient.getUserChatroomIds(userId);
        
        for (UUID chatroomId : chatroomIds) {
            UUID archChatroomId = archiveService.archiveChatroom(chatroomId);
            if (archChatroomId != null) {
                archiveService.archiveMessages(chatroomId, archChatroomId);
            }
        }
    }
}
```

**파일**: `batch/src/main/java/com/dorandoran/batch/job/UserDeletionJob.java`

```java
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "user-deletion.enabled", havingValue = "true", matchIfMissing = false)
public class UserDeletionJob implements CommandLineRunner {
    
    private final UserDeletionService userDeletionService;
    
    @Value("${user-deletion.days-inactive:30}")
    private int daysInactive;
    
    @Override
    public void run(String... args) {
        log.info("=== User Deletion Job 시작 ===");
        log.info("설정: daysInactive={}", daysInactive);
        
        try {
            userDeletionService.deleteInactiveUsers(daysInactive);
            log.info("=== User Deletion Job 완료 ===");
        } catch (Exception e) {
            log.error("=== User Deletion Job 실패 ===", e);
            throw e;
        }
    }
}
```

**설정 파일**: `batch/src/main/resources/application.yml`

```yaml
user-deletion:
  enabled: false  # 기본값: false (수동 실행)
  days-inactive: 30  # INACTIVE 상태로 30일 이상 유지된 사용자 삭제
```

---

## 주의사항 및 리스크

### 1. 데이터 복구 불가능성

- 완전 삭제된 사용자 데이터는 **운영 DB에서 복구 불가능**
- 아카이브에 보존된 데이터만 복구 가능
- **반드시 아카이브 완료 후 삭제 진행**

### 2. 아카이브 실패 시나리오

- 아카이브 실패 시 전체 플로우 중단 (트랜잭션 롤백)
- 사용자는 여전히 INACTIVE 상태로 유지
- 관리자가 수동으로 재시도 필요

### 3. 대량 삭제 시 성능

- 많은 채팅방을 가진 사용자의 경우 아카이브 시간이 오래 걸릴 수 있음
- 배치 작업 시 일괄 처리로 인한 DB 부하 가능
- **권장**: 배치 작업은 비피크 시간대에 실행

### 4. 법적 요구사항

- GDPR 등 개인정보 보호 규정 준수 필요
- 사용자 동의 확인 필요
- 삭제 전 알림 발송 여부 검토 필요

### 5. 모니터링 및 알림

- 삭제 작업 실패 시 알림 발송
- 삭제된 사용자 수 통계 수집
- 아카이브 성공률 모니터링

---

## 다음 단계

1. **비즈니스 승인**: 완전 삭제 플로우 및 FK 변경 승인
2. **마이그레이션 실행**: `V6__update_user_deletion_fk_constraints.sql` 실행
3. **코드 구현**: `UserService.hardDeleteUser()` 및 관련 메서드 구현
4. **테스트**: 
   - 단위 테스트 (각 단계별)
   - 통합 테스트 (전체 플로우)
   - 성능 테스트 (대량 데이터)
5. **배치 작업 구현**: `UserDeletionService` 및 `UserDeletionJob` 구현
6. **모니터링 설정**: 삭제 작업 모니터링 및 알림 설정
7. **문서화**: 운영 매뉴얼 작성

---

## 참고 문서

- `INACTIVE_USER_JOB_REQUIREMENTS.md`: 비활성 사용자 처리 요구사항
- `INACTIVE_USER_JOB_IMPLEMENTATION.md`: 비활성 사용자 배치 작업 구현
- `ARCHIVE_SQL_MAPPING_VERIFICATION_REPORT.md`: 아카이브 매핑 검증 리포트
