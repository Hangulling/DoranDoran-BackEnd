# TokenCleanupJob 비즈니스 요구사항 및 매핑 확인

> **작성일**: 2025-01-04  
> **상태**: 확인 완료, 활성화 준비

---

## 현재 구현 상태

### CleanupScheduler 위치
- **파일**: `auth/src/main/java/com/dorandoran/auth/config/CleanupScheduler.java`
- **상태**: 구현 완료, 하지만 `@Component` 주석 처리되어 비활성화됨

### 구현 내용
```java
// @Component  // ← 주석 처리됨!
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class CleanupScheduler {
    private final RefreshTokenRepository refreshTokenRepository;
    
    // 매 30분마다 만료/폐기된 토큰 정리
    @Scheduled(cron = "0 */30 * * * *")
    public void cleanupExpired() {
        try {
            long deleted = refreshTokenRepository.deleteByUserIdAndRevokedIsTrueOrExpiresAtBefore(
                null, LocalDateTime.now()
            );
            log.debug("정리된 리프레시 토큰 수: {}", deleted);
        } catch (Exception e) {
            log.warn("만료 정리 작업 실패", e);
        }
    }
}
```

---

## 비즈니스 요구사항 확인

### 1. 토큰 정리 대상

#### RefreshToken
- **저장 위치**: PostgreSQL (`auth_schema.refresh_tokens`)
- **만료 시간**: 7일 (604800000ms)
- **정리 대상**:
  - ✅ 만료된 토큰 (`expiresAt < 현재시간`)
  - ✅ 폐기된 토큰 (`revoked = true`)

#### Access Token
- **저장 위치**: Redis (블랙리스트)
- **만료 시간**: 1시간 (3600000ms)
- **정리 방식**: Redis TTL로 자동 만료 (별도 정리 불필요)

#### EmailVerificationToken
- **저장 위치**: PostgreSQL (`auth_schema.email_verifications`)
- **만료 시간**: 5분
- **정리 필요 여부**: 확인 필요 (현재 정리 로직 없음)

#### PasswordResetToken
- **저장 위치**: PostgreSQL (`auth_schema.password_reset_tokens`)
- **만료 시간**: 24시간
- **정리 필요 여부**: 확인 필요 (현재 정리 로직 없음)

### 2. 정리 주기

**현재 설정**: 30분마다 (`0 */30 * * * *`)

**비즈니스 요구사항**:
- ✅ **적절함**: 30분마다 정리하면 DB 용량 관리에 충분
- ✅ **성능 영향 최소**: 짧은 주기로 부하 분산

**대안**:
- 1시간마다: `0 0 * * * *`
- 6시간마다: `0 0 */6 * * *`
- 일일: `0 0 3 * * *` (새벽 3시)

---

## 매핑 확인

### Repository 메서드 확인

**메서드 시그니처**:
```java
long deleteByUserIdAndRevokedIsTrueOrExpiresAtBefore(
    UUID userId, 
    LocalDateTime time
);
```

**현재 사용**:
```java
refreshTokenRepository.deleteByUserIdAndRevokedIsTrueOrExpiresAtBefore(
    null,  // userId = null → 모든 사용자의 토큰
    LocalDateTime.now()  // 현재 시간 이전 만료된 토큰
);
```

### 문제점 발견 ⚠️

**메서드 이름과 실제 동작 불일치**:
- 메서드 이름: `deleteByUserIdAndRevokedIsTrueOrExpiresAtBefore`
- 실제 동작: `userId = null`이면 **모든 사용자**의 토큰 삭제
- 조건: `revoked = true` **OR** `expiresAt < time`

**의도된 동작**:
- 만료된 토큰 삭제: `expiresAt < 현재시간`
- 폐기된 토큰 삭제: `revoked = true`

**현재 구현이 맞는지 확인 필요**:
- `userId = null`이면 모든 사용자의 토큰을 대상으로 하는 것이 맞는가?
- 메서드 이름이 실제 동작을 정확히 반영하는가?

### 권장 수정

**옵션 1: 메서드 이름 변경** (권장)
```java
// Repository
long deleteByRevokedIsTrueOrExpiresAtBefore(LocalDateTime time);

// 사용
refreshTokenRepository.deleteByRevokedIsTrueOrExpiresAtBefore(LocalDateTime.now());
```

**옵션 2: 현재 메서드 유지** (userId 파라미터 제거)
```java
// Repository
long deleteByRevokedIsTrueOrExpiresAtBefore(LocalDateTime time);

// 사용
refreshTokenRepository.deleteByRevokedIsTrueOrExpiresAtBefore(LocalDateTime.now());
```

**옵션 3: 현재 구현 유지** (userId = null로 모든 사용자 대상)
- 메서드 이름에 "All" 추가하여 명확화
- 주석 추가

---

## 활성화 계획

### 1. 즉시 활성화 (현재 구현 유지)

**조치**:
1. `@Component` 주석 해제
2. 로그 레벨 변경 (`log.debug` → `log.info`)

**장점**:
- 즉시 사용 가능
- 추가 작업 불필요

**단점**:
- 메서드 이름이 실제 동작과 불일치

### 2. 개선 후 활성화 (권장)

**조치**:
1. Repository 메서드 이름 변경 또는 개선
2. `@Component` 주석 해제
3. 로그 레벨 변경

**장점**:
- 코드 가독성 향상
- 유지보수 용이

---

## 최종 권장 사항

### 즉시 활성화 + 개선

1. **즉시**: `@Component` 주석 해제하여 활성화
2. **개선**: Repository 메서드 이름 변경 (선택)

### 활성화 코드

```java
@Component  // 주석 해제
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class CleanupScheduler {
    private final RefreshTokenRepository refreshTokenRepository;
    
    @Scheduled(cron = "0 */30 * * * *")
    public void cleanupExpired() {
        try {
            long deleted = refreshTokenRepository.deleteByUserIdAndRevokedIsTrueOrExpiresAtBefore(
                null, LocalDateTime.now()
            );
            log.info("정리된 리프레시 토큰 수: {}", deleted);  // debug → info
        } catch (Exception e) {
            log.warn("만료 정리 작업 실패", e);
        }
    }
}
```

---

## 추가 고려 사항

### EmailVerificationToken 정리
- 현재 정리 로직 없음
- 5분 만료이므로 자동 정리 필요 여부 확인

### PasswordResetToken 정리
- 현재 정리 로직 없음
- 24시간 만료이므로 자동 정리 필요 여부 확인

### TokenBlacklist 정리
- Redis TTL로 자동 만료 (별도 정리 불필요)

---

## 결론

✅ **비즈니스 요구사항**: 적절함
✅ **매핑**: 정상 작동 (메서드 이름 개선 권장)
✅ **활성화**: 즉시 가능

**다음 단계**: `@Component` 주석 해제하여 활성화


