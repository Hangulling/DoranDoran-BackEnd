# Batch Service 업무 점검 보고서

> **점검일**: 2025-01-04  
> **대상**: Batch Service의 모든 Job, Scheduler, Service 매핑 상태

---

## 📊 전체 현황

### ✅ 구현 완료된 업무

| 업무 | 구현 방식 | 실행 방법 | 상태 |
|------|----------|----------|------|
| **ChatroomCleanupScheduler** | `@Scheduled` | 자동 (매일 새벽 3시) | ✅ 정상 |
| **ArchiveJob** | `CommandLineRunner` | Cron (매일 새벽 4시) | ✅ 정상 |

### ⚠️ 빈 파일 (구현되지 않음)

| 파일 | 예상 용도 | 상태 | 권장 사항 |
|------|----------|------|----------|
| `ChatroomCleanupJob.java` | 채팅방 정리 Job | ❌ 빈 파일 | 삭제 권장 (Scheduler로 대체됨) |
| `DailyReportJob.java` | 일일 리포트 생성 | ❌ 빈 파일 | 구현 필요 또는 삭제 |
| `InactiveUserJob.java` | 비활성 사용자 처리 | ❌ 빈 파일 | 구현 필요 또는 삭제 |
| `TokenCleanupJob.java` | 토큰 정리 | ❌ 빈 파일 | 구현 필요 또는 삭제 |
| `MessageArchivingJob.java` | 메시지 아카이빙 | ❌ 빈 파일 | 삭제 권장 (ArchiveJob으로 대체됨) |
| `ChatroomCleanupService.java` | 채팅방 정리 서비스 | ❌ 빈 파일 | 삭제 권장 (Scheduler가 직접 처리) |
| `DailyReportService.java` | 일일 리포트 서비스 | ❌ 빈 파일 | 구현 필요 또는 삭제 |
| `InactiveUserService.java` | 비활성 사용자 서비스 | ❌ 빈 파일 | 구현 필요 또는 삭제 |
| `TokenCleanupService.java` | 토큰 정리 서비스 | ❌ 빈 파일 | 구현 필요 또는 삭제 |
| `MessageArchivingService.java` | 메시지 아카이빙 서비스 | ❌ 빈 파일 | 삭제 권장 (ArchiveService로 대체됨) |

---

## 🔍 상세 분석

### 1. ChatroomCleanupScheduler ✅

**위치**: `batch/src/main/java/com/dorandoran/batch/job/ChatroomCleanupScheduler.java`

**구현 상태**:
- ✅ `@Component` 어노테이션으로 등록됨
- ✅ `@Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")` 설정됨
- ✅ `JdbcTemplate`을 직접 사용하여 구현됨
- ✅ `BatchJobController`에서 수동 실행 가능

**실행 방식**:
- **자동**: 매일 새벽 3시 (Asia/Seoul)
- **수동**: `POST /api/batch/jobs/chatroom-cleanup`

**기능**:
- Soft-delete된 채팅방을 하드 삭제
- 관련 메시지, 친밀도 진행도도 함께 삭제

**문제점**:
- ⚠️ Service 레이어 없이 Scheduler에서 직접 처리
- ⚠️ `ChatroomCleanupService.java`가 빈 파일로 존재 (혼란 가능)

**권장 사항**:
1. Service 레이어 추가하여 비즈니스 로직 분리
2. 빈 파일 `ChatroomCleanupService.java` 삭제 또는 구현

---

### 2. ArchiveJob ✅

**위치**: `batch/src/main/java/com/dorandoran/batch/job/ArchiveJob.java`

**구현 상태**:
- ✅ `@Component` 어노테이션으로 등록됨
- ✅ `@ConditionalOnProperty(name = "archive.enabled", havingValue = "true")` 설정됨
- ✅ `CommandLineRunner` 구현
- ✅ `ArchiveService` 사용

**실행 방식**:
- **Cron**: `archive-chatrooms.sh` 스크립트로 매일 새벽 4시 실행
- **수동**: `java -jar batch.jar --archive.enabled=true`

**기능**:
- 90일 이상 된 아카이빙/삭제된 채팅방을 Archive 스키마로 이동
- 메시지, Agent 결과도 함께 아카이빙

**매핑 상태**:
- ✅ `ArchiveService` 정상 매핑
- ✅ `ArchiveSqlInitializer` 정상 작동 (SQL 함수 자동 생성)

**문제점**: 없음

---

### 3. 빈 파일들 정리 필요 ⚠️

#### 3.1. 중복/불필요한 파일

**삭제 권장**:
1. `ChatroomCleanupJob.java` - `ChatroomCleanupScheduler`로 대체됨
2. `MessageArchivingJob.java` - `ArchiveJob`으로 대체됨
3. `ChatroomCleanupService.java` - 빈 파일, Scheduler가 직접 처리
4. `MessageArchivingService.java` - 빈 파일, `ArchiveService`로 대체됨

#### 3.2. 미구현 파일 (구현 필요 또는 삭제)

**구현 필요 여부 확인 필요**:
1. `DailyReportJob.java` - 일일 리포트 생성 기능이 필요한가?
2. `InactiveUserJob.java` - 비활성 사용자 처리 기능이 필요한가?
3. `TokenCleanupJob.java` - 토큰 정리 기능이 필요한가?
4. `DailyReportService.java` - 일일 리포트 서비스
5. `InactiveUserService.java` - 비활성 사용자 서비스
6. `TokenCleanupService.java` - 토큰 정리 서비스

---

## 📋 매핑 상태 요약

### ✅ 정상 매핑

```
ChatroomCleanupScheduler
  └─> JdbcTemplate (직접 사용)
  └─> BatchJobController (수동 실행)

ArchiveJob
  └─> ArchiveService
      ├─> ArchChatroomRepository
      ├─> ArchMessageRepository
      ├─> ArchAgentResultRepository
      └─> ArchIngestionStateRepository
  └─> ArchiveSqlInitializer (SQL 함수 생성)
```

### ❌ 매핑되지 않음

```
ChatroomCleanupJob (빈 파일)
ChatroomCleanupService (빈 파일)
DailyReportJob (빈 파일)
DailyReportService (빈 파일)
InactiveUserJob (빈 파일)
InactiveUserService (빈 파일)
TokenCleanupJob (빈 파일)
TokenCleanupService (빈 파일)
MessageArchivingJob (빈 파일)
MessageArchivingService (빈 파일)
```

---

## 🔧 권장 조치 사항

### 즉시 조치 (정리)

1. **빈 파일 삭제**:
   ```bash
   # 중복/불필요한 파일 삭제
   rm batch/src/main/java/com/dorandoran/batch/job/ChatroomCleanupJob.java
   rm batch/src/main/java/com/dorandoran/batch/job/MessageArchivingJob.java
   rm batch/src/main/java/com/dorandoran/batch/service/ChatroomCleanupService.java
   rm batch/src/main/java/com/dorandoran/batch/service/MessageArchivingService.java
   ```

2. **ChatroomCleanupScheduler 리팩토링** (선택):
   - Service 레이어 추가하여 비즈니스 로직 분리
   - 테스트 용이성 향상

### 검토 필요 (비즈니스 결정)

다음 기능들이 실제로 필요한지 확인:

1. **DailyReportJob**: 일일 리포트 생성 기능
2. **InactiveUserJob**: 비활성 사용자 처리 기능
3. **TokenCleanupJob**: 토큰 정리 기능

**결정 후**:
- 필요 없으면 → 파일 삭제
- 필요하면 → 구현 계획 수립

---

## 📝 application.yml 설정 점검

### ✅ 현재 설정

```yaml
# Archive 설정
archive:
  enabled: false  # 기본값: false (Cron에서 --archive.enabled=true로 실행)
  days-old: 90
  limit: 100
  job-name: "default-archive-job"
```

### ⚠️ 추가 설정 필요 (미구현 Job들)

다음 Job들이 구현되면 설정 추가 필요:

```yaml
# 예시 (구현 시 추가)
daily-report:
  enabled: true
  schedule: "0 0 1 * * *"  # 매일 새벽 1시

inactive-user:
  enabled: true
  days-inactive: 90
  schedule: "0 0 2 * * *"  # 매일 새벽 2시

token-cleanup:
  enabled: true
  schedule: "0 0 */6 * * *"  # 6시간마다
```

---

## ✅ 체크리스트

### 구현 완료
- [x] ChatroomCleanupScheduler 구현 및 스케줄링
- [x] ArchiveJob 구현 및 Cron 설정
- [x] ArchiveService 구현
- [x] Archive Repository 구현
- [x] Archive SQL 함수 자동 생성

### 정리 필요
- [ ] 빈 파일 삭제 (ChatroomCleanupJob, MessageArchivingJob 등)
- [ ] ChatroomCleanupService 구현 또는 삭제
- [ ] 미구현 Job/Service 파일 정리

### 검토 필요
- [ ] DailyReportJob 필요 여부 확인
- [ ] InactiveUserJob 필요 여부 확인
- [ ] TokenCleanupJob 필요 여부 확인

---

## 📊 최종 평가

### 현재 상태
- ✅ **구현 완료**: 2개 업무 (ChatroomCleanupScheduler, ArchiveJob)
- ⚠️ **빈 파일**: 10개 (정리 필요)
- ❌ **미구현**: 6개 (검토 필요)

### 권장 우선순위
1. **높음**: 빈 파일 삭제 (코드 정리)
2. **중간**: ChatroomCleanupScheduler 리팩토링 (Service 레이어 추가)
3. **낮음**: 미구현 Job 검토 및 구현 결정

---

## 🎯 결론

**현재 Batch Service는 2개의 주요 업무가 정상 작동 중입니다:**
1. ChatroomCleanupScheduler: 매일 새벽 3시 자동 실행
2. ArchiveJob: 매일 새벽 4시 Cron으로 실행

**하지만 10개의 빈 파일이 존재하여 코드베이스가 혼란스러울 수 있습니다.**
- 즉시 삭제 가능: 4개 (중복/불필요)
- 검토 후 결정: 6개 (미구현)

**권장 사항**: 빈 파일들을 정리하여 코드베이스를 깔끔하게 유지하는 것이 좋습니다.


