# Batch Service 기능 검토 보고서

> **검토일**: 2025-01-04  
> **대상**: DailyReportJob, InactiveUserJob, TokenCleanupJob

---

## 📊 검토 결과 요약

| 기능 | 현재 상태 | 구현 위치 | 필요성 | 권장 사항 |
|------|----------|----------|--------|----------|
| **DailyReportJob** | ⚠️ 미구현 | - | ✅ **필요** | 구현 권장 |
| **InactiveUserJob** | ⚠️ 미구현 | - | ⚠️ **검토 필요** | 비즈니스 요구사항 확인 |
| **TokenCleanupJob** | ✅ **이미 구현됨** | Auth Service | ✅ **필요** | 활성화 또는 Batch로 이동 |

---

## 🔍 상세 검토

### 1. DailyReportJob (일일 리포트 생성)

#### 현재 상태
- ❌ **구현되지 않음** (빈 파일)
- ✅ **수동 리포트는 존재**: `docs/maintenance/YYYY-MM-DD/server-log-analysis-report.md`
- ✅ **Shell 스크립트 존재**: `docs/RESOURCE_MONITORING_GUIDE.md`에 일일 리포트 스크립트 예시

#### 발견된 관련 기능
1. **수동 리포트 생성**:
   - `docs/maintenance/` 디렉토리에 날짜별 리포트 존재
   - 예: `2025-11-27/server-log-analysis-report.md`, `2025-12-01/server-log-analysis-report.md`
   - 내용: 서비스 상태, 사용자 접속 통계, 리소스 사용 현황 등

2. **리소스 모니터링 스크립트**:
   - `docs/RESOURCE_MONITORING_GUIDE.md`에 일일 리포트 생성 스크립트 예시
   - CPU, Memory, Disk, Docker Container 통계 수집

#### 필요성 평가
✅ **구현 권장**

**이유**:
1. 수동 리포트가 이미 존재하므로 자동화하면 유용
2. 일일 모니터링 데이터 수집 필요
3. 서비스 상태 추적 및 문제 조기 발견에 도움

#### 제안 구현 내용
```java
@Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul") // 매일 새벽 1시
public void generateDailyReport() {
    // 1. 서비스 상태 수집
    // 2. 사용자 접속 통계
    // 3. 리소스 사용 현황
    // 4. 오류 로그 분석
    // 5. 리포트 파일 생성 (Markdown)
}
```

**수집할 데이터**:
- 서비스별 요청 수, 응답 시간
- 사용자 접속 통계 (신규, 활성, 비활성)
- 리소스 사용량 (CPU, Memory, Disk)
- 오류 로그 통계
- Archive 진행 상태

---

### 2. InactiveUserJob (비활성 사용자 처리)

#### 현재 상태
- ❌ **구현되지 않음** (빈 파일)
- ✅ **User 엔티티에 `last_conn_time` 필드 존재**
- ✅ **User 상태 관리 기능 존재** (ACTIVE, INACTIVE, SUSPENDED)

#### 발견된 관련 기능
1. **User 상태 관리**:
   - `User.UserStatus`: ACTIVE, INACTIVE, SUSPENDED
   - `UserService.updateUserStatus()`: 상태 변경 기능
   - `UserService.deleteUser()`: 소프트 삭제 (INACTIVE로 변경)

2. **마지막 연결 시간**:
   - `User.lastConnTime`: 마지막 연결 시간 필드
   - `UserService.updateLastConnectionTime()`: 연결 시간 업데이트

3. **비활성 사용자 로그인 차단**:
   - `AuthService.login()`: INACTIVE 사용자 로그인 시도 차단
   - `AuthService.oauthLogin()`: OAuth 로그인도 동일하게 차단

#### 필요성 평가
⚠️ **비즈니스 요구사항 확인 필요**

**확인 필요 사항**:
1. **자동 비활성화 정책이 필요한가?**
   - 예: 90일 이상 접속하지 않은 사용자를 자동으로 INACTIVE로 변경
   - 또는: 관리자가 수동으로만 처리

2. **비활성화 기준**:
   - 몇 일 이상 접속하지 않으면 비활성화?
   - 비활성화 전 알림 발송이 필요한가?

3. **비활성화 후 처리**:
   - 데이터 보존 기간은?
   - 완전 삭제는 언제?

#### 제안 구현 내용 (구현 시)
```java
@Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul") // 매일 새벽 2시
public void processInactiveUsers() {
    // 1. 90일 이상 접속하지 않은 ACTIVE 사용자 조회
    // 2. INACTIVE로 상태 변경
    // 3. 이벤트 발행 (UserStatusChangedEvent)
    // 4. (선택) 알림 발송
}
```

**주의사항**:
- 비즈니스 정책에 따라 구현 여부 결정
- GDPR 등 개인정보 보호 규정 고려
- 사용자에게 사전 알림 필요 여부 확인

---

### 3. TokenCleanupJob (토큰 정리)

#### 현재 상태
- ✅ **이미 구현되어 있음!** (`auth/src/main/java/com/dorandoran/auth/config/CleanupScheduler.java`)
- ⚠️ **하지만 비활성화됨** (`@Component` 주석 처리)

#### 발견된 구현
```java
// auth/src/main/java/com/dorandoran/auth/config/CleanupScheduler.java
// @Component  // ← 주석 처리됨!
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class CleanupScheduler {
    private final RefreshTokenRepository refreshTokenRepository;
    
    // 매 30분마다 만료/폐기된 토큰 정리
    @Scheduled(cron = "0 */30 * * * *")
    public void cleanupExpired() {
        long deleted = refreshTokenRepository.deleteByUserIdAndRevokedIsTrueOrExpiresAtBefore(
            null, LocalDateTime.now()
        );
        log.debug("정리된 리프레시 토큰 수: {}", deleted);
    }
}
```

#### 토큰 관리 현황
1. **RefreshToken**:
   - PostgreSQL에 저장 (`auth_schema.refresh_tokens`)
   - 만료 시간: 7일
   - 정리 대상: 만료된 토큰, 폐기된 토큰

2. **TokenBlacklist**:
   - Redis에 저장 (TTL로 자동 만료)
   - Access Token 블랙리스트 (로그아웃 시)
   - 자동 정리 불필요 (Redis TTL)

3. **EmailVerificationToken**:
   - PostgreSQL에 저장
   - 만료 시간: 5분
   - 정리 필요 여부 확인 필요

4. **PasswordResetToken**:
   - PostgreSQL에 저장
   - 만료 시간: 설정 필요
   - 정리 필요 여부 확인 필요

#### 필요성 평가
✅ **활성화 또는 Batch로 이동 권장**

**이유**:
1. 이미 구현되어 있지만 비활성화됨
2. 만료된 토큰 정리는 필요함 (DB 용량 관리)
3. Auth Service에서 처리하거나 Batch Service로 이동 가능

#### 권장 사항

**옵션 1: Auth Service에서 활성화** (권장)
- `CleanupScheduler`의 `@Component` 주석 해제
- Auth Service에서 직접 처리 (토큰은 Auth의 책임)

**옵션 2: Batch Service로 이동**
- `TokenCleanupJob` 구현
- `TokenCleanupService` 구현
- Auth Service의 Repository 접근 필요 (Feign Client 또는 공유 모듈)

**옵션 3: 하이브리드**
- RefreshToken 정리는 Auth Service에서 (30분마다)
- EmailVerificationToken, PasswordResetToken 정리는 Batch Service에서 (일일)

---

## 📋 최종 권장 사항

### 즉시 구현 권장
1. ✅ **DailyReportJob**: 일일 리포트 자동 생성
   - 우선순위: **높음**
   - 구현 난이도: **중간**
   - 기대 효과: 모니터링 자동화, 문제 조기 발견

### 비즈니스 확인 후 결정
2. ⚠️ **InactiveUserJob**: 비활성 사용자 자동 처리
   - 우선순위: **중간**
   - 구현 난이도: **낮음**
   - 필요 여부: **비즈니스 정책 확인 필요**

### 활성화 또는 이동
3. ✅ **TokenCleanupJob**: 토큰 정리
   - 우선순위: **높음**
   - 구현 난이도: **낮음** (이미 구현됨)
   - 권장: **Auth Service에서 활성화** (옵션 1)

---

## 🎯 구현 우선순위

| 순위 | 기능 | 상태 | 조치 |
|------|------|------|------|
| 1 | **TokenCleanupJob** | 이미 구현됨 | Auth Service에서 활성화 |
| 2 | **DailyReportJob** | 미구현 | 구현 권장 |
| 3 | **InactiveUserJob** | 미구현 | 비즈니스 확인 후 결정 |

---

## 📝 다음 단계

1. **TokenCleanupJob**: `auth/config/CleanupScheduler.java`의 `@Component` 주석 해제
2. **DailyReportJob**: 구현 계획 수립 및 구현
3. **InactiveUserJob**: 비즈니스 요구사항 확인 후 결정


