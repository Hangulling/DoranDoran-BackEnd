# InactiveUserJob 구현 완료

> **작성일**: 2025-01-04  
> **상태**: 구현 완료

---

## 개요

일정 기간 이상 접속하지 않은 사용자를 자동으로 INACTIVE 상태로 변경하는 배치 작업입니다.

---

## 구현 내용

### 1. InactiveUserService

**위치**: `batch/src/main/java/com/dorandoran/batch/service/InactiveUserService.java`

**주요 기능**:
- `findInactiveUsers()`: 비활성 사용자 조회
- `deactivateUser()`: 단일 사용자 비활성화
- `deactivateUsers()`: 여러 사용자 일괄 비활성화
- `processInactiveUsers()`: 전체 프로세스 실행

**비즈니스 로직**:
- ACTIVE 상태인 사용자만 대상
- `last_conn_time`이 설정된 일수 이전인 사용자 조회
- ADMIN 역할 사용자 제외 옵션 (기본값: true)
- 상태를 INACTIVE로 변경

### 2. InactiveUserJob

**위치**: `batch/src/main/java/com/dorandoran/batch/job/InactiveUserJob.java`

**특징**:
- `CommandLineRunner` 구현
- `@ConditionalOnProperty`로 활성화 제어
- 설정값 기반 실행

**실행 조건**:
```bash
--inactive-user.enabled=true
```

### 3. 설정 추가

**위치**: `batch/src/main/resources/application.yml`

```yaml
inactive-user:
  enabled: false  # 기본값: false
  days-inactive: 90  # 90일 이상 접속하지 않은 사용자
  exclude-admin: true  # ADMIN 역할 제외
```

---

## 사용 방법

### 1. 단독 실행

```bash
java -jar batch.jar \
  --inactive-user.enabled=true \
  --inactive-user.days-inactive=90 \
  --inactive-user.exclude-admin=true
```

### 2. 스크립트 실행

```bash
# 단독 실행
bash scripts/backup/inactive-user-cleanup.sh

# 통합 실행 (모든 배치 작업)
bash scripts/backup/run-all-batch-jobs.sh
```

### 3. Cron 설정 (선택)

```bash
# 매주 일요일 새벽 3시 실행
0 3 * * 0 /home/ec2-user/inactive-user-cleanup.sh
```

---

## 비즈니스 요구사항

### 기본 정책

1. **비활성화 기준**: 90일 이상 접속하지 않은 사용자
2. **대상**: ACTIVE 상태인 사용자만
3. **제외**: ADMIN 역할 사용자 (설정 가능)
4. **처리**: 상태를 INACTIVE로 변경

### 데이터 보존

- INACTIVE 상태로 변경만 수행
- 사용자 데이터는 보존됨
- 필요시 수동으로 재활성화 가능

---

## 구현 세부사항

### SQL 쿼리

**비활성 사용자 조회**:
```sql
SELECT id
FROM user_schema.app_user
WHERE status = 'ACTIVE'
  AND last_conn_time < ?
  AND role != 'ROLE_ADMIN'  -- exclude-admin=true일 때
ORDER BY last_conn_time ASC
```

**상태 변경**:
```sql
UPDATE user_schema.app_user
SET status = 'INACTIVE',
    updated_at = NOW()
WHERE id = ?
```

### 트랜잭션 처리

- `@Transactional` 사용
- 일괄 처리 시 개별 사용자별 트랜잭션
- 실패 시 로깅만 하고 다음 사용자 계속 처리

### 로깅

- 처리 시작/완료 로그
- 대상 사용자 수 로그
- 성공/실패 사용자 수 로그
- 개별 사용자 처리 실패 시 WARN 레벨 로그

---

## 주의사항

### 이벤트 발행

현재 구현은 직접 DB 업데이트를 수행하므로 `UserStatusChangedEvent`가 발행되지 않습니다.

**이유**:
- Batch 서비스는 독립적으로 실행
- User 서비스와 직접 통신하지 않음
- 성능 및 의존성 최소화

**필요시 개선 방안**:
1. User 서비스 API 호출 (네트워크 오버헤드)
2. 이벤트 직접 발행 (복잡도 증가)
3. 현재 방식 유지 (권장)

### ADMIN 제외

기본적으로 ADMIN 역할 사용자는 자동 비활성화에서 제외됩니다.

**이유**:
- 관리자 계정은 장기간 미접속해도 유지 필요
- 보안 정책에 따라 수동 관리

### 데이터 정합성

- `last_conn_time`이 NULL인 경우 처리하지 않음
- 이미 INACTIVE 상태인 사용자는 건너뜀
- 존재하지 않는 사용자는 WARN 로그만 기록

---

## 테스트

### 수동 테스트

1. **테스트 사용자 생성**:
   ```sql
   UPDATE user_schema.app_user
   SET last_conn_time = NOW() - INTERVAL '91 days'
   WHERE email = 'test@example.com';
   ```

2. **Job 실행**:
   ```bash
   java -jar batch.jar --inactive-user.enabled=true
   ```

3. **결과 확인**:
   ```sql
   SELECT id, email, status, last_conn_time
   FROM user_schema.app_user
   WHERE email = 'test@example.com';
   ```

### 로그 확인

```bash
# 로그 파일 확인
tail -f /home/ec2-user/backups/inactive-user-cleanup.log

# 또는 통합 로그
tail -f /home/ec2-user/backups/batch-jobs.log
```

---

## 모니터링

### 주요 메트릭

- 처리된 사용자 수
- 실패한 사용자 수
- 실행 시간
- 마지막 실행 시간

### 로그 예시

```
[2025-01-04 03:00:00] 비활성 사용자 처리 시작
[2025-01-04 03:00:01] 비활성 사용자 처리 설정: daysInactive=90, excludeAdmin=true
[2025-01-04 03:00:02] 비활성화 대상 사용자: 15명
[2025-01-04 03:00:03] 사용자 비활성화 완료: userId=xxx-xxx-xxx
...
[2025-01-04 03:00:05] 비활성 사용자 처리 완료: 성공=15, 실패=0
[2025-01-04 03:00:05] === InactiveUser Job 완료: 비활성화된 사용자 수=15 ===
```

---

## 향후 개선 사항

### 1. 알림 기능 (선택)

비활성화 전 사용자에게 알림 발송:
- 30일 전 경고 알림
- 7일 전 최종 경고 알림

### 2. 통계 리포트

- 비활성화된 사용자 통계
- 기간별 비활성화 추이
- 역할별 비활성화 현황

### 3. 복구 기능

- 일괄 재활성화 기능
- 특정 사용자 재활성화

---

## 참고 문서

- `INACTIVE_USER_JOB_REQUIREMENTS.md`: 비즈니스 요구사항 확인 문서
- `BATCH_SERVICE_CRON_TO_JAR_MIGRATION.md`: 배치 서비스 실행 방식 변경 문서
- `run-all-batch-jobs.sh`: 통합 실행 스크립트

---

## 변경 이력

- **2025-01-04**: 초기 구현 완료
  - InactiveUserService 구현
  - InactiveUserJob 구현
  - 설정 및 스크립트 추가


