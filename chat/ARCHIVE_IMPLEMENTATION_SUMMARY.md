# Archive 아카이빙 구현 완료 요약

> **구현일**: 2025-01-04  
> **방식**: EC2 Cron + JAR 파일 (기존 백업 스크립트 패턴과 동일)

---

## 구현 완료 항목

### ✅ 1. Archive 엔티티 생성
- `ArchChatroom`: Archive 채팅방 엔티티
- `ArchMessage`: Archive 메시지 엔티티
- `ArchAgentResult`: Archive Agent 결과 엔티티
- `ArchIngestionState`: Archive 적재 상태 엔티티

### ✅ 2. Archive Repository 생성
- `ArchChatroomRepository`
- `ArchMessageRepository`
- `ArchAgentResultRepository`
- `ArchIngestionStateRepository`

### ✅ 3. SQL 함수 스크립트 (JAR 내부 포함)
- `batch/src/main/resources/db/archive/archive_functions.sql`
- JAR 실행 시 자동 생성/업데이트

### ✅ 4. SQL 초기화 로직
- `ArchiveSqlInitializer`: ApplicationRunner로 시작 시 SQL 함수 자동 생성

### ✅ 5. ArchiveService 구현
- `archiveChatroom()`: 채팅방 아카이빙
- `archiveMessages()`: 메시지 아카이빙 (페이징 처리)
- `extractAndSaveAgentResults()`: Agent 결과 분리 및 저장
- `findChatroomsToArchive()`: 아카이빙 대상 조회
- `updateIngestionState()`: 진행 상태 업데이트

### ✅ 6. ArchiveJob 구현
- `CommandLineRunner`로 JAR 실행 시 아카이빙 수행
- `@ConditionalOnProperty`로 활성화 제어
- 설정값: `archive.enabled`, `archive.days-old`, `archive.limit`

### ✅ 7. Shell 스크립트
- `scripts/backup/archive-chatrooms.sh`: JAR 실행 스크립트
- 기존 백업 스크립트와 동일한 패턴

### ✅ 8. Cron 설정 가이드
- `scripts/backup/setup-archive-cron.sh`: Cron 설정 스크립트
- `scripts/backup/ARCHIVE_README.md`: 사용 가이드

---

## 아키텍처

```
EC2 Cron (매일 새벽 4시)
  ↓
archive-chatrooms.sh
  ↓
java -jar archive-batch.jar --archive.enabled=true
  ↓
ArchiveSqlInitializer (SQL 함수 자동 생성)
  ↓
ArchiveJob (CommandLineRunner)
  ↓
ArchiveService
  ↓
Archive Repository → PostgreSQL (archive_schema)
```

---

## 주요 특징

### 1. JAR 내부 SQL 스크립트 포함
- SQL 함수가 JAR 내부 `resources/db/archive/`에 포함
- JAR 실행 시 자동으로 SQL 함수 생성/업데이트
- 별도 SQL 파일 관리 불필요

### 2. 기존 백업 스크립트 패턴과 일치
- 동일한 디렉토리 구조 (`/home/ec2-user/backups/`)
- 동일한 로그 파일 형식
- 동일한 Cron 설정 방식

### 3. 자동화
- Cron으로 완전 자동 실행
- SQL 함수 자동 생성
- 진행 상태 자동 추적

### 4. 안전한 처리
- 페이징 처리로 메모리 사용량 제한
- 트랜잭션 관리
- 에러 처리 및 로깅

---

## 배포 방법

### 1. JAR 빌드

```bash
./gradlew :batch:build
```

### 2. EC2에 배포

```bash
# JAR 파일 업로드
scp -i ~/.ssh/dorandoran-key.pem \
  batch/build/libs/batch-*.jar \
  ec2-user@3.21.177.186:/home/ec2-user/archive-batch.jar

# 스크립트 업로드
scp -i ~/.ssh/dorandoran-key.pem \
  scripts/backup/archive-chatrooms.sh \
  ec2-user@3.21.177.186:/home/ec2-user/archive-chatrooms.sh

# 실행 권한 부여
ssh -i ~/.ssh/dorandoran-key.pem ec2-user@3.21.177.186 \
  "chmod +x /home/ec2-user/archive-chatrooms.sh"
```

### 3. Cron 설정

```bash
ssh -i ~/.ssh/dorandoran-key.pem ec2-user@3.21.177.186
bash scripts/backup/setup-archive-cron.sh
```

또는 수동:

```bash
(crontab -l 2>/dev/null; echo "0 4 * * * /home/ec2-user/archive-chatrooms.sh >> /home/ec2-user/backups/archive.log 2>&1") | crontab -
```

---

## 실행 방법

### 자동 실행 (Cron)
- 매일 새벽 4시 자동 실행

### 수동 실행

```bash
ssh -i ~/.ssh/dorandoran-key.pem ec2-user@3.21.177.186
/home/ec2-user/archive-chatrooms.sh
```

### 로그 확인

```bash
ssh -i ~/.ssh/dorandoran-key.pem ec2-user@3.21.177.186
tail -f /home/ec2-user/backups/archive.log
```

---

## 설정 변경

### application.yml

```yaml
archive:
  enabled: false  # 기본값: false (Cron에서 --archive.enabled=true로 실행)
  days-old: 90    # 90일 이상 된 채팅방 아카이빙
  limit: 100      # 한 번에 처리할 최대 채팅방 수
  job-name: "default-archive-job"
```

### 스크립트에서 변경

```bash
# archive-chatrooms.sh 수정
DAYS_OLD=90
LIMIT=100
```

---

## 다음 단계

1. **테스트**: 로컬에서 JAR 실행 테스트
2. **배포**: EC2에 배포 및 Cron 설정
3. **모니터링**: 로그 확인 및 아카이빙 상태 모니터링
4. **튜닝**: 필요시 `days-old`, `limit` 조정

---

## 참고 문서

- `scripts/backup/ARCHIVE_README.md`: 상세 사용 가이드
- `chat/ARCHIVE_SCHEMA_FINAL.md`: Archive 스키마 상세 설명
- `chat/ARCHIVE_SCHEMA_FINAL_SQL.sql`: Archive 스키마 SQL


