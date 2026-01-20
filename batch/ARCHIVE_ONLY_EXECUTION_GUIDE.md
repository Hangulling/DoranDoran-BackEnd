# Archive 작업만 실행하기 위한 가이드

> **작성일**: 2025-01-04  
> **목적**: 아카이빙 작업만 실행하고, 다른 작업들(ChatroomCleanupJob, InactiveUserJob)은 실행되지 않도록 보장

---

## 현재 구조

### Job 활성화 방식

모든 Job은 `@ConditionalOnProperty`로 제어되며, 기본값은 모두 `false`입니다:

```yaml
# application.yml
archive:
  enabled: false  # 기본값: false

chatroom-cleanup:
  enabled: false  # 기본값: false

inactive-user:
  enabled: false  # 기본값: false
```

### Job 실행 조건

각 Job은 다음 조건에서만 실행됩니다:

- **ArchiveJob**: `--archive.enabled=true` 필요
- **ChatroomCleanupJob**: `--chatroom-cleanup.enabled=true` 필요
- **InactiveUserJob**: `--inactive-user.enabled=true` 필요

---

## Archive 작업만 실행하는 방법

### 방법 1: archive-chatrooms.sh 스크립트 사용 (권장)

**이미 구현되어 있음**: `scripts/backup/archive-chatrooms.sh`

이 스크립트는 ArchiveJob만 실행하고, 다른 job은 명시적으로 `false`로 설정합니다:

```bash
java -Xmx512m -jar "$BATCH_JAR" \
  --archive.enabled=true \
  --archive.days-old=$DAYS_OLD \
  --archive.limit=$LIMIT \
  --archive.job-name="archive-job" \
  --chatroom-cleanup.enabled=false \
  --inactive-user.enabled=false
```

**사용법**:
```bash
bash scripts/backup/archive-chatrooms.sh
```

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

**중요**: 다른 job을 명시적으로 `false`로 설정해야 합니다.

### 방법 3: 환경 변수 사용

```bash
export ARCHIVE_ENABLED=true
export CHATROOM_CLEANUP_ENABLED=false
export INACTIVE_USER_ENABLED=false

java -jar batch.jar \
  --archive.enabled=${ARCHIVE_ENABLED} \
  --chatroom-cleanup.enabled=${CHATROOM_CLEANUP_ENABLED} \
  --inactive-user.enabled=${INACTIVE_USER_ENABLED}
```

---

## 안전장치

### 1. application.yml 기본값

모든 job의 기본값이 `false`이므로, 명시적으로 `true`로 설정하지 않으면 실행되지 않습니다.

### 2. @ConditionalOnProperty

각 Job은 `matchIfMissing = false`로 설정되어 있어, 프로퍼티가 없으면 실행되지 않습니다.

### 3. 명시적 false 설정

ArchiveJob 실행 시 다른 job을 명시적으로 `false`로 설정하여 이중 보호합니다.

---

## 주의사항

### ❌ 하지 말아야 할 것

1. **모든 job을 true로 설정하지 말 것**:
   ```bash
   # ❌ 잘못된 예
   java -jar batch.jar \
     --archive.enabled=true \
     --chatroom-cleanup.enabled=true \  # 이렇게 하면 ChatroomCleanupJob도 실행됨!
     --inactive-user.enabled=true
   ```

2. **application.yml에서 기본값을 true로 변경하지 말 것**

3. **통합 스크립트 사용 시 주의**:
   - `run-all-batch-jobs.sh`는 모든 job을 순차적으로 실행합니다
   - Archive만 실행하려면 `archive-chatrooms.sh`를 사용하세요

### ✅ 올바른 방법

1. **archive-chatrooms.sh 사용** (가장 안전)
2. **JAR 실행 시 다른 job을 명시적으로 false로 설정**
3. **환경 변수로 제어**

---

## 검증 방법

### 실행 전 확인

```bash
# 실행할 명령어 확인
cat scripts/backup/archive-chatrooms.sh | grep "enabled"
```

출력 예시:
```
--archive.enabled=true
--chatroom-cleanup.enabled=false
--inactive-user.enabled=false
```

### 실행 후 확인

로그에서 다음을 확인:
- ✅ "Archive Job 시작" 메시지
- ❌ "ChatroomCleanup Job 시작" 메시지가 없어야 함
- ❌ "InactiveUser Job 시작" 메시지가 없어야 함

---

## 추가 안전장치 (선택사항)

### Option 1: application.yml에 프로파일 추가

```yaml
# application-archive-only.yml
archive:
  enabled: true
chatroom-cleanup:
  enabled: false
inactive-user:
  enabled: false
```

실행:
```bash
java -jar batch.jar --spring.profiles.active=archive-only
```

### Option 2: BatchJobController 비활성화

REST API를 통한 수동 실행을 방지하려면:

```java
@ConditionalOnProperty(name = "batch.controller.enabled", havingValue = "true", matchIfMissing = false)
@RestController
public class BatchJobController {
    // ...
}
```

---

## 요약

✅ **가장 안전한 방법**: `scripts/backup/archive-chatrooms.sh` 사용

✅ **직접 실행 시**: 다른 job을 명시적으로 `false`로 설정

✅ **기본값**: 모든 job이 `false`이므로, 명시적으로 `true`로 설정하지 않으면 실행되지 않음

---

**상태**: 현재 구조로도 Archive만 실행 가능하며, 추가 안전장치는 선택사항입니다.


