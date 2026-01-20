# Archive Only 모드 설정 완료

> **작성일**: 2025-01-04  
> **목적**: Archive 작업만 실행하고, 다른 작업들(하드 삭제 등)은 실행되지 않도록 보장

---

## ✅ 현재 안전장치

### 1. application.yml 기본값

모든 job의 기본값이 `false`로 설정되어 있어, 명시적으로 `true`로 설정하지 않으면 실행되지 않습니다:

```yaml
archive:
  enabled: false  # 기본값: false

chatroom-cleanup:
  enabled: false  # 기본값: false

inactive-user:
  enabled: false  # 기본값: false
```

### 2. @ConditionalOnProperty

각 Job은 `matchIfMissing = false`로 설정되어 있어, 프로퍼티가 없으면 실행되지 않습니다:

```java
@ConditionalOnProperty(name = "archive.enabled", havingValue = "true", matchIfMissing = false)
```

### 3. archive-chatrooms.sh 스크립트

이미 다른 job을 명시적으로 `false`로 설정하고 있습니다:

```bash
--archive.enabled=true
--chatroom-cleanup.enabled=false  # 명시적으로 false
--inactive-user.enabled=false     # 명시적으로 false
--batch.controller.enabled=false  # REST API 비활성화
```

---

## 📋 Archive만 실행하는 방법

### 방법 1: archive-chatrooms.sh 사용 (가장 권장) ✅

```bash
bash scripts/backup/archive-chatrooms.sh
```

**장점**:
- ✅ 다른 job을 명시적으로 `false`로 설정
- ✅ REST API도 비활성화
- ✅ 로그 파일 자동 관리
- ✅ 에러 처리 포함

### 방법 2: 프로파일 사용

```bash
java -jar batch.jar --spring.profiles.active=archive-only
```

**프로파일 파일**: `application-archive-only.yml`
- Archive만 활성화
- 다른 작업들은 명시적으로 `false`
- REST API 비활성화

### 방법 3: JAR 직접 실행

```bash
java -jar batch.jar \
  --archive.enabled=true \
  --archive.days-old=90 \
  --archive.limit=100 \
  --archive.job-name="archive-job" \
  --chatroom-cleanup.enabled=false \
  --inactive-user.enabled=false \
  --batch.controller.enabled=false
```

**중요**: 다른 job과 controller를 반드시 `false`로 명시해야 합니다.

---

## 🔒 추가 안전장치

### 1. BatchJobController 조건부 활성화

REST API를 통한 수동 실행을 방지하기 위해 `@ConditionalOnProperty` 추가:

```java
@ConditionalOnProperty(
    name = "batch.controller.enabled", 
    havingValue = "true", 
    matchIfMissing = true  // 기본값: true (하위 호환성)
)
```

**사용법**:
- Archive만 실행: `--batch.controller.enabled=false`
- REST API 사용: `--batch.controller.enabled=true` (기본값)

### 2. application-archive-only.yml 프로파일

Archive만 실행하는 전용 프로파일 생성:

```yaml
archive:
  enabled: true
chatroom-cleanup:
  enabled: false
inactive-user:
  enabled: false
batch:
  controller:
    enabled: false
```

---

## ✅ 검증 방법

### 실행 전 확인

```bash
# 스크립트 내용 확인
cat scripts/backup/archive-chatrooms.sh | grep "enabled"
```

예상 출력:
```
--archive.enabled=true
--chatroom-cleanup.enabled=false
--inactive-user.enabled=false
--batch.controller.enabled=false
```

### 실행 후 로그 확인

로그에서 다음을 확인:
- ✅ `"=== Archive Job 시작 ==="` 메시지 있음
- ❌ `"[ChatroomCleanup]"` 메시지 없음
- ❌ `"=== InactiveUser Job 시작 ==="` 메시지 없음
- ❌ `"[BatchJobController]"` 메시지 없음

---

## 📝 요약

### ✅ 현재 구조로도 안전

1. **기본값이 모두 false**: 명시적으로 `true`로 설정하지 않으면 실행되지 않음
2. **@ConditionalOnProperty**: 프로퍼티가 없으면 실행되지 않음
3. **archive-chatrooms.sh**: 다른 job을 명시적으로 `false`로 설정

### ✅ 추가 안전장치 (선택사항)

1. **BatchJobController 조건부 활성화**: REST API 비활성화 가능
2. **application-archive-only.yml 프로파일**: Archive 전용 프로파일

### ✅ 권장 방법

**`scripts/backup/archive-chatrooms.sh` 사용**

이 스크립트는:
- ✅ ArchiveJob만 실행
- ✅ 다른 job을 명시적으로 `false`로 설정
- ✅ REST API도 비활성화
- ✅ 로그 파일 자동 관리

---

**상태**: ✅ Archive만 실행 가능, 다른 작업은 실행되지 않음


