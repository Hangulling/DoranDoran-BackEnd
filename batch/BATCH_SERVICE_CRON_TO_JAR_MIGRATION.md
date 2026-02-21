# Batch Service Cron → JAR 실행 방식 변경

> **작성일**: 2025-01-04  
> **변경 내용**: @Scheduled 제거, CommandLineRunner로 통일

---

## 변경 사항

### 1. ChatroomCleanupScheduler → ChatroomCleanupJob

**변경 전**:
- `ChatroomCleanupScheduler`: `@Scheduled`로 매일 새벽 3시 자동 실행
- `@Component`로 항상 활성화

**변경 후**:
- `ChatroomCleanupJob`: `CommandLineRunner`로 JAR 실행 시 실행
- `@ConditionalOnProperty(name = "chatroom-cleanup.enabled", havingValue = "true")`로 제어

**삭제된 파일**:
- `batch/src/main/java/com/dorandoran/batch/job/ChatroomCleanupScheduler.java`
- `batch/src/main/java/com/dorandoran/batch/config/SchedulingConfig.java`

**새로 생성된 파일**:
- `batch/src/main/java/com/dorandoran/batch/job/ChatroomCleanupJob.java`

### 2. BatchApplication 변경

**변경 전**:
```java
@EnableScheduling
public class BatchApplication {
    // ...
}
```

**변경 후**:
```java
// @EnableScheduling 제거
public class BatchApplication {
    // ...
}
```

### 3. application.yml 설정 추가

```yaml
# Chatroom Cleanup 설정
chatroom-cleanup:
  enabled: false  # 기본값: false (JAR 실행 시 --chatroom-cleanup.enabled=true로 실행)
```

---

## 실행 방식

### 변경 전 (Cron 방식)

```java
@Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
public void purgeSoftDeletedChatrooms() {
    // 자동 실행
}
```

### 변경 후 (JAR 실행 방식)

```bash
# 단일 Job 실행
java -jar batch.jar --chatroom-cleanup.enabled=true

# Archive Job 실행
java -jar batch.jar --archive.enabled=true

# 모든 Job 일괄 실행
java -jar batch.jar \
  --chatroom-cleanup.enabled=true \
  --archive.enabled=true
```

---

## Shell 스크립트

### 1. chatroom-cleanup.sh

**위치**: `scripts/backup/chatroom-cleanup.sh`

**용도**: 채팅방 정리 Job만 실행

```bash
#!/bin/bash
java -Xmx512m -jar /home/ec2-user/batch.jar \
  --chatroom-cleanup.enabled=true \
  >> /home/ec2-user/backups/chatroom-cleanup.log 2>&1
```

### 2. run-all-batch-jobs.sh

**위치**: `scripts/backup/run-all-batch-jobs.sh`

**용도**: 모든 Batch Job을 순차적으로 실행

```bash
#!/bin/bash
# 1. 채팅방 정리
java -jar batch.jar --chatroom-cleanup.enabled=true

# 2. Archive 아카이빙
java -jar batch.jar --archive.enabled=true
```

### 3. setup-batch-cron.sh

**위치**: `scripts/backup/setup-batch-cron.sh`

**용도**: Cron 설정 (모든 Job 일괄 실행)

```bash
# 매일 새벽 3시 실행
0 3 * * * /home/ec2-user/run-all-batch-jobs.sh
```

---

## 현재 Job 목록

### ✅ CommandLineRunner로 구현된 Job

1. **ChatroomCleanupJob**
   - 설정: `--chatroom-cleanup.enabled=true`
   - 기능: Soft-delete된 채팅방 하드 삭제

2. **ArchiveJob**
   - 설정: `--archive.enabled=true`
   - 기능: Archive 스키마로 데이터 아카이빙

---

## 사용 방법

### 로컬에서 실행

```bash
# 채팅방 정리만
java -jar batch.jar --chatroom-cleanup.enabled=true

# Archive 아카이빙만
java -jar batch.jar --archive.enabled=true

# 모든 Job 일괄 실행
java -jar batch.jar \
  --chatroom-cleanup.enabled=true \
  --archive.enabled=true
```

### EC2에서 Cron 설정

```bash
# 1. 스크립트 배포
scp scripts/backup/run-all-batch-jobs.sh ec2-user@3.21.177.186:/home/ec2-user/
scp scripts/backup/setup-batch-cron.sh ec2-user@3.21.177.186:/home/ec2-user/

# 2. 실행 권한 부여
chmod +x /home/ec2-user/run-all-batch-jobs.sh
chmod +x /home/ec2-user/setup-batch-cron.sh

# 3. Cron 설정
bash /home/ec2-user/setup-batch-cron.sh
```

---

## 장점

1. **일관성**: 모든 Job이 동일한 방식으로 실행
2. **제어**: 각 Job을 개별적으로 활성화/비활성화 가능
3. **유연성**: 필요에 따라 특정 Job만 실행 가능
4. **테스트 용이**: 로컬에서 쉽게 테스트 가능

---

## 주의사항

1. **JAR 파일 경로**: EC2에서 JAR 파일 경로 확인 필요
2. **메모리 설정**: `-Xmx512m` 등 메모리 제한 설정 권장
3. **로그 파일**: 각 Job의 로그를 별도 파일로 관리 권장

---

## 다음 단계

1. EC2에 JAR 파일 배포
2. Shell 스크립트 배포
3. Cron 설정
4. 테스트 실행


