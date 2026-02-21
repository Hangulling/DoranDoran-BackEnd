# Archive 아카이빙 구현 방안 평가

> **평가일**: 2025-01-04  
> **기준**: 기존 백업 스크립트 패턴 (`scripts/backup/create-auto-backup.sh`)

---

## 기존 백업 스크립트 패턴 분석

### 현재 구조
```bash
# scripts/backup/create-auto-backup.sh
- Docker 컨테이너 접근: docker exec dorandoran-shared-db
- PostgreSQL 명령어 직접 실행: pg_dump, psql
- Cron 자동 실행: 매일 새벽 3시
- 로그 파일 관리: /home/ec2-user/backups/cron.log
- 오래된 파일 자동 삭제: 7일 이상
```

### 장점
- ✅ 서버 부하 최소화 (비피크 시간 실행)
- ✅ 자동화 가능 (Cron)
- ✅ 기존 인프라 활용
- ✅ 로그 관리 용이

---

## 옵션 5: EC2 Cron + 스크립트 평가

### ✅ 장점 (기존 패턴과 일치)

1. **기존 인프라 활용**
   - 백업 스크립트와 동일한 패턴
   - Docker 컨테이너 접근 방식 동일
   - Cron 설정 경험 있음

2. **서버 부하 분산**
   - 비피크 시간 실행 (예: 새벽 4시)
   - 백업(3시) 이후 실행으로 부하 분산
   - 기존 서비스에 영향 최소

3. **자동화**
   - Cron으로 완전 자동화
   - 수동 개입 불필요
   - 로그 자동 관리

4. **유지보수 용이**
   - Shell 스크립트로 간단
   - 기존 백업 스크립트와 유사한 구조
   - 디버깅 쉬움

### ⚠️ 단점 및 해결 방안

#### 1. 복잡한 데이터 변환 로직

**문제**: 아카이빙은 단순 SQL이 아니라 복잡한 변환이 필요
- JSONB 파싱 및 변환
- 여러 테이블 조인
- Agent 결과 분리
- Store, Billing 테이블 조회

**해결 방안**: **하이브리드 접근**
- **SQL 함수/프로시저**: 기본 데이터 복사는 SQL로
- **Java 스크립트**: 복잡한 변환은 Java로 (독립 실행)

#### 2. Java 런타임 필요

**문제**: Shell만으로는 JSONB 파싱/변환이 어려움

**해결 방안**: **독립 실행 가능한 JAR**
```bash
# EC2에 JAR 배치
/home/ec2-user/archive-batch.jar

# Cron에서 실행
0 4 * * * java -jar /home/ec2-user/archive-batch.jar >> /home/ec2-user/backups/archive.log 2>&1
```

#### 3. 리소스 사용

**문제**: EC2 리소스 사용

**해결 방안**: 
- 비피크 시간 실행 (새벽 4시, 백업 이후)
- 메모리 제한 설정: `java -Xmx512m -jar ...`
- 배치 크기 제한 (한 번에 너무 많이 처리하지 않음)

---

## 추천 구현 방안: 하이브리드 접근

### Phase 1: SQL 기반 기본 아카이빙 (즉시 구현 가능)

**목적**: 간단한 데이터 복사는 SQL로 처리

```sql
-- archive_schema에 함수 생성
CREATE OR REPLACE FUNCTION archive_schema.archive_chatroom_simple(
    p_chatroom_id UUID
) RETURNS UUID AS $$
DECLARE
    v_arch_id UUID;
BEGIN
    -- 기본 데이터 복사 (스냅샷 제외)
    INSERT INTO archive_schema.arch_chatrooms (
        source_chatroom_id, user_id, chatbot_id,
        name, description, concept,
        last_message_at, source_last_message_id,
        is_archived, is_deleted,
        source_created_at, source_updated_at,
        archived_at, meta
    )
    SELECT 
        cr.id, cr.user_id, cr.chatbot_id,
        cr.name, cr.description,
        COALESCE(cr.settings->>'concept', 'FRIEND'),
        cr.last_message_at, cr.last_message_id,
        cr.is_archived, cr.is_deleted,
        cr.created_at, cr.updated_at,
        NOW(), '{}'::jsonb
    FROM chat_schema.chatrooms cr
    WHERE cr.id = p_chatroom_id
    RETURNING id INTO v_arch_id;
    
    RETURN v_arch_id;
END;
$$ LANGUAGE plpgsql;
```

**장점**:
- ✅ 즉시 구현 가능
- ✅ 서버 부하 최소
- ✅ 기존 백업 스크립트 패턴과 유사

**단점**:
- ⚠️ JSONB 변환은 제한적
- ⚠️ Agent 결과 분리는 어려움

### Phase 2: Java 기반 완전한 아카이빙 (추가 구현)

**목적**: 복잡한 변환 로직 처리

```bash
#!/bin/bash
# /home/ec2-user/archive-chatrooms.sh

set -e

CONTAINER_NAME="dorandoran-shared-db"
DB_NAME="dorandoran"
DB_USER="doran"
ARCHIVE_JAR="/home/ec2-user/archive-batch.jar"
LOG_FILE="/home/ec2-user/backups/archive.log"

echo "[$(date +'%Y-%m-%d %H:%M:%S')] 아카이빙 시작"

# Java JAR 실행 (EC2에 Java 설치 필요)
java -Xmx512m -jar "$ARCHIVE_JAR" \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/$DB_NAME \
  --spring.datasource.username=$DB_USER \
  --spring.datasource.password=doran \
  >> "$LOG_FILE" 2>&1

if [ $? -eq 0 ]; then
  echo "[$(date +'%Y-%m-%d %H:%M:%S')] ✅ 아카이빙 완료"
else
  echo "[$(date +'%Y-%m-%d %H:%M:%S')] ❌ 아카이빙 실패"
  exit 1
fi
```

**Cron 설정**:
```bash
# 매일 새벽 4시 실행 (백업 이후)
0 4 * * * /home/ec2-user/archive-chatrooms.sh
```

---

## 최종 추천: 옵션 5 (EC2 Cron + 하이브리드)

### 구현 전략

1. **Phase 1: SQL 함수 기반 (즉시 사용)**
   - 기본 데이터 복사는 SQL 함수로
   - Shell 스크립트에서 호출
   - 기존 백업 스크립트 패턴과 동일

2. **Phase 2: Java JAR 추가 (완전한 아카이빙)**
   - 복잡한 변환은 Java로
   - 독립 실행 가능한 JAR
   - Cron에서 실행

### 장점 요약

| 항목 | 평가 |
|------|------|
| **서버 부하** | ✅ 최소 (비피크 시간) |
| **기존 패턴 일치** | ✅ 백업 스크립트와 동일 |
| **자동화** | ✅ Cron으로 완전 자동 |
| **구현 난이도** | ⚠️ 중간 (하이브리드 필요) |
| **유지보수** | ✅ 쉬움 (기존 패턴) |

### 단점 및 해결

| 단점 | 해결 방안 |
|------|----------|
| 복잡한 변환 로직 | Java JAR로 처리 |
| Java 런타임 필요 | EC2에 Java 설치 (한 번만) |
| 리소스 사용 | 메모리 제한, 비피크 시간 실행 |

---

## 결론

**옵션 5 (EC2 Cron + 스크립트)를 추천합니다.**

**이유**:
1. ✅ 기존 백업 스크립트와 동일한 패턴
2. ✅ 서버 부하 최소화 (비피크 시간)
3. ✅ 완전 자동화 가능
4. ✅ 유지보수 용이

**구현 순서**:
1. **즉시**: SQL 함수 기반 기본 아카이빙
2. **추가**: Java JAR 기반 완전한 아카이빙

이 방식으로 진행할까요?


