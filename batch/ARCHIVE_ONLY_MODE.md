# Archive Only 모드 설정 가이드

> **작성일**: 2025-01-04  
> **목적**: Archive 작업만 실행하고, 다른 작업들(하드 삭제 등)은 실행되지 않도록 보장

---

## 현재 상태

### ✅ 이미 구현된 안전장치

1. **application.yml 기본값**: 모든 job이 `enabled: false`
2. **@ConditionalOnProperty**: `matchIfMissing = false`로 설정되어 프로퍼티가 없으면 실행되지 않음
3. **archive-chatrooms.sh**: 다른 job을 명시적으로 `false`로 설정

---

## Archive만 실행하는 방법

### 방법 1: archive-chatrooms.sh 사용 (가장 권장)

```bash
bash scripts/backup/archive-chatrooms.sh
```

이 스크립트는 다음을 보장합니다:
- ✅ ArchiveJob만 실행
- ❌ ChatroomCleanupJob 실행 안 됨
- ❌ InactiveUserJob 실행 안 됨

### 방법 2: JAR 직접 실행

```bash
java -jar batch.jar \
  --archive.enabled=true \
  --archive.days-old=90 \
  --archive.limit=100 \
  --archive.job-name="archive-job" \
  --chatroom-cleanup.enabled=false \
  --inactive-user.enabled=false
```

**중요**: 다른 job을 반드시 `false`로 명시해야 합니다.

---

## 추가 안전장치 (선택사항)

### Option 1: application-archive-only.yml 프로파일 생성

새로운 프로파일을 만들어 Archive만 활성화:

```yaml
# batch/src/main/resources/application-archive-only.yml
archive:
  enabled: true
  days-old: 90
  limit: 100
  job-name: "archive-only-job"

chatroom-cleanup:
  enabled: false  # 명시적으로 false

inactive-user:
  enabled: false  # 명시적으로 false
```

실행:
```bash
java -jar batch.jar --spring.profiles.active=archive-only
```

### Option 2: 환경 변수로 제어

```bash
# 환경 변수 설정
export ARCHIVE_ONLY_MODE=true

# 스크립트에서 사용
if [ "$ARCHIVE_ONLY_MODE" = "true" ]; then
  java -jar batch.jar \
    --archive.enabled=true \
    --chatroom-cleanup.enabled=false \
    --inactive-user.enabled=false
fi
```

### Option 3: BatchJobController 비활성화

REST API를 통한 수동 실행을 방지하려면:

```java
@ConditionalOnProperty(
    name = "batch.controller.enabled", 
    havingValue = "true", 
    matchIfMissing = false
)
@RestController
@RequestMapping("/api/batch/jobs")
public class BatchJobController {
    // ...
}
```

그리고 `application.yml`에:
```yaml
batch:
  controller:
    enabled: false  # Archive만 실행할 때는 false
```

---

## 검증 체크리스트

### 실행 전
- [ ] `archive-chatrooms.sh` 스크립트 확인 (다른 job이 false로 설정되어 있는지)
- [ ] JAR 실행 명령어 확인 (다른 job이 false로 설정되어 있는지)

### 실행 후
- [ ] 로그에서 "Archive Job 시작" 메시지 확인
- [ ] 로그에서 "ChatroomCleanup Job 시작" 메시지가 없는지 확인
- [ ] 로그에서 "InactiveUser Job 시작" 메시지가 없는지 확인
- [ ] 아카이빙된 데이터 확인

---

## 권장 사항

### ✅ 권장: archive-chatrooms.sh 사용

가장 안전하고 간단한 방법입니다. 이미 구현되어 있으며, 다른 job을 명시적으로 `false`로 설정합니다.

### ⚠️ 주의: run-all-batch-jobs.sh 사용 금지

`run-all-batch-jobs.sh`는 모든 job을 순차적으로 실행하므로, Archive만 실행하려면 사용하지 마세요.

---

## 요약

**현재 구조로도 Archive만 실행 가능합니다:**

1. ✅ `application.yml`에서 모든 job 기본값이 `false`
2. ✅ `@ConditionalOnProperty`로 명시적 활성화 필요
3. ✅ `archive-chatrooms.sh`가 다른 job을 명시적으로 `false`로 설정

**추가 안전장치는 선택사항이며, 현재 구조로도 충분히 안전합니다.**

---

**상태**: ✅ Archive만 실행 가능, 다른 작업은 실행되지 않음


