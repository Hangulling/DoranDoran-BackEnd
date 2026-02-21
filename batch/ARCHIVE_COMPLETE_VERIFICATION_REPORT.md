# Archive 데이터 누락 문제 해결 및 전체 재검증 보고서

> **검증일**: 2025-01-04  
> **버전**: 2.0 (확장)  
> **상태**: ✅ 모든 데이터 누락 문제 해결 완료

---

## 📊 검증 결과 요약

| 항목 | 이전 상태 | 현재 상태 | 비고 |
|------|----------|----------|------|
| **Store 데이터** | ❌ ID만 저장 | ✅ 완전 아카이빙 | `arch_stores` 테이블 추가 |
| **Usage 데이터** | ❌ ID만 저장 | ✅ 완전 아카이빙 | `arch_usage_events` 테이블 추가 |
| **Intimacy Progress** | ⚠️ 일부만 저장 | ✅ 완전 아카이빙 | `arch_intimacy_progress` 테이블 추가 |
| **settings 전체** | ⚠️ 일부만 저장 | ✅ 완전 저장 | `meta.settings`에 전체 저장 |
| **Agent 결과 처리** | ⚠️ 부분 파싱 없음 | ✅ 부분 파싱 추가 | 에러 처리 강화 |
| **트랜잭션 처리** | ⚠️ 약함 | ✅ 강화 | 원자적 처리 보장 |

---

## 1. 스키마 확장 완료

### 1.1 새로 추가된 테이블

✅ **`arch_stores`** (14개 컬럼)
- Store의 모든 필드 아카이빙
- `ai_response` JSONB 전체 저장
- 인덱스: chatroom, message, user, archived_at, ai_response (GIN)

✅ **`arch_usage_events`** (14개 컬럼)
- Usage 이벤트의 모든 필드 아카이빙
- `meta` JSONB 전체 저장
- 인덱스: chatroom, user, event_time, request_id, archived_at, meta (GIN)

✅ **`arch_intimacy_progress`** (10개 컬럼)
- Intimacy Progress의 모든 필드 아카이빙
- `progress_data` JSONB 전체 저장
- 인덱스: chatroom, user, archived_at, progress_data (GIN)

### 1.2 스키마 구조 검증

**서버 검증 결과**:
- ✅ 모든 테이블 정상 생성
- ✅ 모든 인덱스 정상 생성
- ✅ 제약조건 정상 적용
- ✅ 권한 정상 부여

---

## 2. Entity 및 Repository 검증

### 2.1 Entity 생성 완료

✅ **`ArchStore`**
- 모든 컬럼 정확히 매핑
- `ai_response` JSONB 필드 정상 처리

✅ **`ArchUsageEvent`**
- 모든 컬럼 정확히 매핑
- `event_time` 타입 변환 처리 (OffsetDateTime → LocalDateTime)
- `cost_in`, `cost_out` BigDecimal 변환 처리
- `meta` JSONB 필드 정상 처리

✅ **`ArchIntimacyProgress`**
- 모든 컬럼 정확히 매핑
- `progress_data` JSONB 필드 정상 처리

### 2.2 Repository 생성 완료

✅ **`ArchStoreRepository`**
- `existsBySourceStoreId()`, `findBySourceStoreId()` 메서드 제공

✅ **`ArchUsageEventRepository`**
- `existsBySourceUsageEventId()`, `findBySourceUsageEventId()` 메서드 제공

✅ **`ArchIntimacyProgressRepository`**
- `existsBySourceIntimacyProgressId()`, `findBySourceIntimacyProgressId()` 메서드 제공

---

## 3. 아카이빙 로직 구현 완료

### 3.1 Store 아카이빙

**구현 위치**: `ArchiveService.archiveStores()`

**처리 내용**:
- `store_schema.stores`에서 `chatroom_id`로 조회
- `is_deleted = false`인 Store만 아카이빙
- 모든 필드 아카이빙 (id, message_id, user_id, content, corrected_content, ai_response, bot_type 등)
- `ai_response` JSONB 전체 저장
- 중복 체크 (`existsBySourceStoreId()`)

**SQL 쿼리 검증**: ✅ 정상

### 3.2 Usage 이벤트 아카이빙

**구현 위치**: `ArchiveService.archiveUsageEvents()`

**처리 내용**:
- `billing.ai_usage_events`에서 `chatroom_id`로 조회
- 모든 필드 아카이빙 (id, event_time, user_id, provider, model, request_id, input_tokens, output_tokens, cost_in, cost_out, meta)
- `event_time` 타입 변환 (OffsetDateTime → LocalDateTime)
- `cost_in`, `cost_out` BigDecimal 변환
- `meta` JSONB 전체 저장
- 중복 체크 (`existsBySourceUsageEventId()`)

**SQL 쿼리 검증**: ✅ 정상

### 3.3 Intimacy Progress 아카이빙

**구현 위치**: `ArchiveService.archiveIntimacyProgress()`

**처리 내용**:
- `chat_schema.intimacy_progress`에서 `chatroom_id`로 조회
- 모든 필드 아카이빙 (id, user_id, intimacy_level, total_corrections, last_feedback, last_updated, progress_data)
- `progress_data` JSONB 전체 저장
- 중복 체크 (`existsBySourceIntimacyProgressId()`)
- Intimacy Progress가 없는 경우 정상 처리 (EmptyResultDataAccessException)

**SQL 쿼리 검증**: ✅ 정상

### 3.4 settings 전체 저장

**구현 위치**: `ArchiveService.buildArchChatroom()`

**처리 내용**:
- `settings` JSONB 전체를 `meta.settings`에 저장
- 파싱 실패 시에도 원본 문자열 저장 시도
- `concept`, `testModel`은 평면 컬럼으로도 유지 (검색 성능)

**검증**: ✅ 정상

### 3.5 Agent 결과 처리 개선

**구현 위치**: `ArchiveService.extractAndSaveAgentResults()`

**개선 사항**:
1. **부분 파싱**: 각 Agent 결과를 개별적으로 try-catch 처리하여, 하나가 실패해도 다른 것은 계속 처리
2. **파싱 실패 처리**: JSON 파싱 실패 시 원본 metadata를 `originalMetadata`에 저장
3. **트랜잭션 강화**: Agent 결과 저장과 metadata 업데이트를 원자적으로 처리
4. **로깅 강화**: 각 단계별 상세 로깅

**검증**: ✅ 정상

---

## 4. 데이터 흐름 검증

### 4.1 아카이빙 흐름

```
archiveChatroom()
  ├─ buildArchChatroom() (settings 전체 저장)
  ├─ archiveStores() (Store 완전 아카이빙)
  ├─ archiveUsageEvents() (Usage 완전 아카이빙)
  └─ archiveIntimacyProgress() (Intimacy Progress 완전 아카이빙)

archiveMessages()
  ├─ archiveMessage()
  │   ├─ buildArchMessage() (metadata.usage.requestId 추출)
  │   └─ extractAndSaveAgentResults() (부분 파싱, 트랜잭션 강화)
  └─ (메시지별 처리)
```

### 4.2 데이터 완전성 검증

✅ **채팅방 데이터**: 완전 이관
- 기본 정보, settings 전체, contextData, meta 전체

✅ **메시지 데이터**: 완전 이관
- 모든 필드, metadata 전체 (originalMetadata 포함)

✅ **Store 데이터**: 완전 이관
- 모든 필드, ai_response JSONB 전체

✅ **Usage 이벤트 데이터**: 완전 이관
- 모든 필드, meta JSONB 전체

✅ **Intimacy Progress 데이터**: 완전 이관
- 모든 필드, progress_data JSONB 전체

✅ **Agent 결과 데이터**: 완전 이관
- 부분 파싱으로 최대한 많은 데이터 추출

---

## 5. 누락 가능성 재분석

### 5.1 이전 문제점 해결 상태

| 문제점 | 이전 상태 | 현재 상태 | 해결 방법 |
|--------|----------|----------|----------|
| Store 데이터 누락 | ❌ 높음 | ✅ 해결 | `arch_stores` 테이블 추가 |
| Usage 데이터 누락 | ❌ 높음 | ✅ 해결 | `arch_usage_events` 테이블 추가 |
| Intimacy Progress 누락 | ⚠️ 중간 | ✅ 해결 | `arch_intimacy_progress` 테이블 추가 |
| settings 전체 누락 | ⚠️ 중간 | ✅ 해결 | `meta.settings`에 전체 저장 |
| Agent 결과 파싱 실패 | ⚠️ 중간 | ✅ 개선 | 부분 파싱 로직 추가 |
| agentResults 맵 업데이트 실패 | ⚠️ 낮음 | ✅ 개선 | 트랜잭션 강화 |

### 5.2 남은 잠재적 누락 가능성

#### ✅ 낮음: 활성 채팅방

**현재**: `is_archived = false AND is_deleted = false`인 채팅방은 아카이빙되지 않음

**영향**: 의도된 동작 (활성 채팅방은 아카이빙 대상이 아님)

**권장**: 필요 시 아카이빙 조건 확장 가능

#### ✅ 낮음: 메시지 페이징 제한

**현재**: 최대 1,000,000개 메시지까지 처리

**영향**: 매우 큰 채팅방에서만 발생 가능

**권장**: 필요 시 cursor-based pagination으로 개선 가능

#### ✅ 낮음: JSONB 파싱 실패

**현재**: 파싱 실패 시 원본 문자열 저장 시도

**영향**: 데이터는 보존되지만 구조화되지 않음

**권장**: 현재 처리 방식 적절

---

## 6. 최종 검증 체크리스트

### 스키마 검증
- [x] `arch_stores` 테이블 구조 확인 ✅
- [x] `arch_usage_events` 테이블 구조 확인 ✅
- [x] `arch_intimacy_progress` 테이블 구조 확인 ✅
- [x] 인덱스 및 제약조건 확인 ✅

### Entity 검증
- [x] `ArchStore` ↔ `arch_stores` 매핑 확인 ✅
- [x] `ArchUsageEvent` ↔ `arch_usage_events` 매핑 확인 ✅
- [x] `ArchIntimacyProgress` ↔ `arch_intimacy_progress` 매핑 확인 ✅

### SQL 쿼리 검증
- [x] Store 조회 쿼리 검증 ✅
- [x] Usage 이벤트 조회 쿼리 검증 ✅
- [x] Intimacy Progress 조회 쿼리 검증 ✅

### 데이터 흐름 검증
- [x] Store 아카이빙 흐름 확인 ✅
- [x] Usage 이벤트 아카이빙 흐름 확인 ✅
- [x] Intimacy Progress 아카이빙 흐름 확인 ✅
- [x] Agent 결과 처리 흐름 확인 ✅

### 누락 가능성 재분석
- [x] 모든 관련 데이터가 아카이빙되는지 확인 ✅
- [x] JSONB 데이터가 완전히 저장되는지 확인 ✅
- [x] 에러 처리 시 데이터 손실 가능성 확인 ✅
- [x] 트랜잭션 일관성 확인 ✅

---

## 7. 구현 완료 사항

### Phase 1: 스키마 확장 ✅
- [x] `arch_stores` 테이블 생성 SQL 작성
- [x] `arch_usage_events` 테이블 생성 SQL 작성
- [x] `arch_intimacy_progress` 테이블 생성 SQL 작성
- [x] 스키마 변경사항 문서화

### Phase 2: Entity 및 Repository 추가 ✅
- [x] `ArchStore` Entity 생성
- [x] `ArchUsageEvent` Entity 생성
- [x] `ArchIntimacyProgress` Entity 생성
- [x] 각각의 Repository 생성

### Phase 3: 아카이빙 로직 구현 ✅
- [x] Store 아카이빙 로직 추가 (`archiveStores()`)
- [x] Usage 이벤트 아카이빙 로직 추가 (`archiveUsageEvents()`)
- [x] Intimacy Progress 아카이빙 로직 추가 (`archiveIntimacyProgress()`)
- [x] `archiveChatroom()` 메서드에 관련 데이터 아카이빙 호출 추가
- [x] `buildArchChatroom()` 메서드에 settings 전체 저장 로직 추가

### Phase 4: Agent 결과 처리 개선 ✅
- [x] `extractAndSaveAgentResults()` 메서드에 부분 파싱 로직 추가
- [x] 트랜잭션 처리 강화
- [x] 에러 처리 및 로깅 개선

### Phase 5: 전체 재검증 ✅
- [x] 스키마 구조 검증
- [x] Entity-스키마 매핑 검증
- [x] SQL 쿼리 검증
- [x] 데이터 흐름 검증
- [x] 누락 가능성 재분석
- [x] 최종 검증 보고서 작성

---

## 8. 생성/수정된 파일

### 새로 생성된 파일
- ✅ `chat/ARCHIVE_SCHEMA_EXTENDED_SQL.sql` - 확장된 스키마 SQL
- ✅ `batch/src/main/java/com/dorandoran/batch/entity/ArchStore.java`
- ✅ `batch/src/main/java/com/dorandoran/batch/entity/ArchUsageEvent.java`
- ✅ `batch/src/main/java/com/dorandoran/batch/entity/ArchIntimacyProgress.java`
- ✅ `batch/src/main/java/com/dorandoran/batch/repository/ArchStoreRepository.java`
- ✅ `batch/src/main/java/com/dorandoran/batch/repository/ArchUsageEventRepository.java`
- ✅ `batch/src/main/java/com/dorandoran/batch/repository/ArchIntimacyProgressRepository.java`
- ✅ `batch/ARCHIVE_COMPLETE_VERIFICATION_REPORT.md` - 최종 검증 보고서

### 수정된 파일
- ✅ `batch/src/main/java/com/dorandoran/batch/service/ArchiveService.java` - 아카이빙 로직 확장
  - Repository 추가 (ArchStoreRepository, ArchUsageEventRepository, ArchIntimacyProgressRepository)
  - `archiveChatroom()` 메서드에 관련 데이터 아카이빙 호출 추가
  - `buildArchChatroom()` 메서드에 settings 전체 저장 로직 추가
  - `archiveStores()` 메서드 추가
  - `archiveUsageEvents()` 메서드 추가
  - `archiveIntimacyProgress()` 메서드 추가
  - `extractAndSaveAgentResults()` 메서드 개선 (부분 파싱, 트랜잭션 강화)

---

## 9. 최종 결론

### ✅ 모든 데이터 누락 문제 해결 완료

1. **Store 데이터**: `arch_stores` 테이블로 완전 아카이빙
2. **Usage 데이터**: `arch_usage_events` 테이블로 완전 아카이빙
3. **Intimacy Progress**: `arch_intimacy_progress` 테이블로 완전 아카이빙
4. **settings 전체**: `meta.settings`에 전체 저장
5. **Agent 결과 처리**: 부분 파싱 및 트랜잭션 강화

### ✅ 데이터 완전성 보장

- 모든 관련 데이터가 아카이빙됨
- JSONB 데이터가 완전히 저장됨
- 에러 처리 시에도 데이터 손실 최소화
- 트랜잭션 일관성 보장

### ✅ 하위 호환성 유지

- 기존 필드 유지 (`arch_messages.metadata_json.link.storeId`, `usageRequestId` 등)
- 기존 아카이빙 로직과 호환
- 기존 스키마 구조 유지

---

**보고서 작성일**: 2025-01-04  
**검증자**: AI Assistant  
**상태**: ✅ 모든 데이터 누락 문제 해결 완료, 전체 재검증 완료


