# Archive 스키마 최종 보고서

> **생성일**: 2025-02-02  
> **상태**: ✅ 호환성 점검 완료, 최종 스키마 확정  
> **버전**: 1.0

---

## 📋 요약

현재 프로젝트의 스키마, 엔티티, DTO, 서비스 로직과의 호환성을 점검한 결과, **archive 스키마는 완벽하게 호환 가능**합니다.

### ✅ 호환성 평가 결과

| 항목 | 평가 | 상태 |
|------|------|------|
| **엔티티 매핑** | ✅ 완벽 | 모든 필드 매핑 가능 |
| **JSONB 구조** | ✅ 완벽 | 운영 구조와 일치 |
| **데이터 타입** | ✅ 완벽 | 모든 타입 일치 |
| **외래키 관계** | ✅ 완벽 | SET NULL 정책 적절 |
| **인덱스 전략** | ✅ 완벽 | 조회 패턴 최적화 |
| **제약조건** | ✅ 수정 완료 | UNIQUE 제약 수정 반영 |
| **데이터 변환** | ⚠️ 구현 필요 | 가이드 제공됨 |

**전체 평가**: ✅ **호환 가능** (구현 준비 완료)

---

## 📁 생성된 문서

### 1. 최종 스키마 정의서
- **파일**: `ARCHIVE_SCHEMA_FINAL.md`
- **내용**: 
  - 스키마 정의
  - JSONB 구조 상세
  - 데이터 변환 가이드
  - 주의사항 및 제약조건

### 2. 호환성 점검 보고서
- **파일**: `ARCHIVE_SCHEMA_COMPATIBILITY_CHECK.md`
- **내용**:
  - 엔티티 매핑 점검 결과
  - JSONB 구조 점검 결과
  - 서비스 로직 호환성
  - 제약조건 점검

### 3. 최종 SQL 스크립트
- **파일**: `ARCHIVE_SCHEMA_FINAL_SQL.sql`
- **내용**: 실행 가능한 최종 스키마 SQL

### 4. 스키마 제안서 (업데이트)
- **파일**: `DATASET_STRUCTURE_PROPOSAL.md`
- **변경사항**:
  - UNIQUE 제약 조건 수정 반영
  - voca 여러 단어 처리 방법 명확화

---

## 🔍 주요 점검 결과

### 1. 엔티티 매핑

#### ChatRoom → arch_chatrooms
- ✅ 모든 필드 매핑 가능
- ✅ 스냅샷 컬럼으로 삭제 대비
- ✅ concept 추출 로직 확인 (`extractConceptFromSettings()`)

#### Message → arch_messages
- ✅ 모든 필드 매핑 가능
- ✅ `sequence_number` 운영과 동일 (그대로 복사)
- ⚠️ `metadata` 구조 변환 필요

### 2. JSONB 구조

#### arch_chatrooms.meta
```json
{
  "concept": "FRIEND",              // ✅ 추출 가능
  "intimacyLevel": 1,               // ✅ 추출 가능
  "testModel": "...",                // ✅ 추출 가능
  "contextData": { ... }            // ✅ 복사 가능
}
```

#### arch_messages.metadata_json
- ✅ 운영 metadata → Archive 구조 변환 가능
- ✅ agent_results 분리 가능
- ✅ 원본 metadata 백업 가능

#### arch_agent_results.payload_json
- ✅ 운영 구조와 완벽 일치
- ✅ intimacy, voca, conver 모두 지원

### 3. 제약조건 수정

#### 확인 완료
- ✅ `uq_arch_messages_room_seq` UNIQUE 제약 유지 (운영과 동일)
- ✅ voca 여러 단어 처리 방법 명확화 (words 배열)

---

## ⚠️ 구현 주의사항

### 1. sequence_number 사용

**중요**: 운영의 `sequence_number`와 archive의 `sequence_number`는 **동일한 의미**

- **운영**: 메시지 순서 번호 (1, 2, 3, 4, ...)
- **Archive**: 운영과 동일 (메시지 순서 번호)

**해결**: 운영의 `sequence_number`를 그대로 복사 (재계산 불필요)
```java
// 운영의 sequence_number를 그대로 복사
archMsg.setSequenceNumber(message.getSequenceNumber());
```

### 2. metadata 변환

**운영 구조**:
```json
{
  "userMessageAnalysis": { "intimacy": {...} },
  "botResponseAnalysis": { "vocabulary": {...} },
  "usage": {...}
}
```

**Archive 구조**:
```json
{
  "link": {
    "agentResults": { "intimacy": "uuid", "voca": "uuid" }
  },
  "originalMetadata": {...}
}
```

**해결**: 변환 로직 구현 필요 (가이드 제공됨)

### 3. 외부 테이블 연결

**storeId 조회**:
```sql
SELECT id FROM store_schema.stores 
WHERE message_id = ? AND is_deleted = false;
```

**usageRequestId 조회**:
```sql
SELECT request_id FROM billing.ai_usage_events 
WHERE chatroom_id = ? AND event_time BETWEEN ? AND ?;
```

---

## 📝 다음 단계

### 1. 스키마 생성
```bash
# SQL 스크립트 실행
psql -U doran -d dorandoran -f chat/ARCHIVE_SCHEMA_FINAL_SQL.sql
```

### 2. 데이터 변환 로직 구현
- `archiveChatroom()` 메서드 구현
- `archiveMessage()` 메서드 구현
- `createAgentResult()` 메서드 구현
- 외부 테이블 연결 로직 구현

### 3. Archive 서비스 개발
- Archive 배치 작업 구현
- Archive API 구현 (필요시)
- 모니터링 및 로깅

### 4. 테스트 및 검증
- 단위 테스트
- 통합 테스트
- 데이터 무결성 검증

---

## 📚 참고 문서

1. **ARCHIVE_SCHEMA_FINAL.md**: 최종 스키마 정의서 및 데이터 변환 가이드
2. **ARCHIVE_SCHEMA_COMPATIBILITY_CHECK.md**: 호환성 점검 상세 보고서
3. **ARCHIVE_SCHEMA_FINAL_SQL.sql**: 실행 가능한 최종 SQL 스크립트
4. **DATASET_STRUCTURE_PROPOSAL.md**: 스키마 제안서 (업데이트됨)
5. **ARCHIVE_SCHEMA_IMPROVEMENTS.md**: 개선 사항 및 조율 결과

---

## ✅ 체크리스트

### 스키마 정의
- [x] arch_chatrooms 테이블 정의
- [x] arch_messages 테이블 정의
- [x] arch_agent_results 테이블 정의
- [x] arch_ingestion_state 테이블 정의
- [x] 인덱스 정의
- [x] 외래키 제약조건 정의
- [x] CHECK 제약조건 정의

### JSONB 구조
- [x] arch_chatrooms.meta 구조 정의
- [x] arch_messages.metadata_json 구조 정의
- [x] arch_agent_results.payload_json 구조 정의

### 호환성 점검
- [x] 엔티티 매핑 점검
- [x] JSONB 구조 점검
- [x] 서비스 로직 호환성 점검
- [x] 제약조건 점검
- [x] 인덱스 전략 점검

### 문서화
- [x] 최종 스키마 정의서 작성
- [x] 호환성 점검 보고서 작성
- [x] 최종 SQL 스크립트 작성
- [x] 데이터 변환 가이드 작성

### 구현 준비
- [ ] 스키마 생성 (SQL 실행)
- [ ] 데이터 변환 로직 구현
- [ ] Archive 서비스 개발
- [ ] 테스트 및 검증

---

## 🎯 결론

현재 프로젝트 구조와 archive 스키마는 **완벽하게 호환**됩니다.

- ✅ 모든 엔티티 필드가 매핑 가능
- ✅ JSONB 구조가 운영과 일치
- ✅ 데이터 변환 로직이 명확히 정의됨
- ✅ 제약조건 수정사항이 반영됨
- ✅ 구현 가이드가 제공됨

**다음 단계**: SQL 스크립트 실행 및 데이터 변환 로직 구현

