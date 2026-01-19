# Batch 서비스 배포 보고서

> **배포일**: 2025-01-09  
> **버전**: 0.1.0-SNAPSHOT  
> **상태**: ✅ 배포 완료

---

## 배포 요약

### 빌드 정보
- **빌드 시간**: 2025-01-09
- **빌드 상태**: ✅ 성공
- **빌드 명령어**: `.\gradlew.bat :batch:clean :batch:build -x test`
- **빌드 결과**: BUILD SUCCESSFUL in 25s

### JAR 파일 정보
- **로컬 경로**: `D:\new_dev\dorandoran-project\batch\build\libs\batch-0.1.0-SNAPSHOT.jar`
- **서버 경로**: `/home/ec2-user/batch.jar`
- **파일 크기**: 56MB
- **MD5 해시**: `acf09efb936a5d133d47ced827eefbbb`

---

## 배포된 기능

### ✅ Archive 작업
- **ArchiveJob**: 채팅방 및 메시지 아카이빙
- **ArchiveService**: 
  - Store 데이터 아카이빙 (`arch_stores`)
  - Usage 이벤트 아카이빙 (`arch_usage_events`)
  - Intimacy Progress 아카이빙 (`arch_intimacy_progress`)
  - settings 전체 저장
  - Agent 결과 부분 파싱 및 트랜잭션 강화

### ✅ Archive Only 모드
- **archive-chatrooms.sh**: ArchiveJob만 실행하는 스크립트
- **BatchJobController**: 조건부 활성화 (REST API 비활성화 가능)
- **application-archive-only.yml**: Archive 전용 프로파일

### ✅ 안전장치
- 모든 job 기본값: `enabled: false`
- `@ConditionalOnProperty`: 명시적 활성화 필요
- archive-chatrooms.sh: 다른 job을 명시적으로 `false`로 설정

---

## 서버 상태 확인

### JAR 파일
```bash
-rw-rw-r--. 1 ec2-user ec2-user 56M Jan  9 07:24 /home/ec2-user/batch.jar
```

### 실행 스크립트
- **위치**: `/home/ec2-user/backups/archive-chatrooms.sh` (확인 필요)
- **용도**: ArchiveJob만 단독 실행

---

## 실행 방법

### Archive만 실행 (권장)

```bash
bash /home/ec2-user/backups/archive-chatrooms.sh
```

또는:

```bash
java -jar /home/ec2-user/batch.jar \
  --archive.enabled=true \
  --archive.days-old=90 \
  --archive.limit=100 \
  --archive.job-name="archive-job" \
  --chatroom-cleanup.enabled=false \
  --inactive-user.enabled=false \
  --batch.controller.enabled=false
```

### 프로파일 사용

```bash
java -jar /home/ec2-user/batch.jar \
  --spring.profiles.active=archive-only
```

---

## 다음 단계

1. ✅ **빌드 완료**
2. ✅ **서버 배포 완료**
3. ⏳ **스크립트 배포 확인** (필요 시)
4. ⏳ **테스트 실행** (ArchiveJob 실행하여 데이터 이관 확인)

---

## 주의사항

### ✅ Archive만 실행 보장

- `archive-chatrooms.sh` 사용 시 다른 job은 실행되지 않음
- `--chatroom-cleanup.enabled=false` 명시
- `--inactive-user.enabled=false` 명시
- `--batch.controller.enabled=false` 명시

### ⚠️ 하드 삭제 작업

- `ChatroomCleanupJob`은 ArchiveJob 완료 후 별도로 실행해야 함
- Archive만 실행하려면 `archive-chatrooms.sh` 사용

---

**배포 상태**: ✅ 완료  
**다음 작업**: ArchiveJob 테스트 실행


