# 아카이브 로직 검증 (탈퇴 시 회원 데이터 이관)

**대상**: igu worktree, `UserWithdrawalArchiveService.archiveUserData(userId)`  
**기준**: `docs/schema_dump_server.sql` 상의 chat_schema, store_schema, billing, archive_schema

---

## 1. 아카이브 대상 매핑

| 소스 (운영) | 아카이브 테이블 | 구현 메서드 | 비고 |
|-------------|-----------------|-------------|------|
| chat_schema.chatrooms | arch_chatrooms | archiveChatroom() | 채팅방 1건 + 스냅샷(이메일, 챗봇명 등) |
| chat_schema.messages | arch_messages | archiveMessages() | sequence_number 순 정렬 후 이관 |
| chat_schema.intimacy_progress | arch_intimacy_progress | archiveIntimacyProgress() | 채팅방당 1건 |
| store_schema.stores | arch_stores | archiveStores() | **is_deleted = false** 만 이관 |
| billing.ai_usage_events | arch_usage_events | archiveUsageEvents() | chatroom_id 기준 |
| message.metadata → Agent 결과 | arch_agent_results | archiveAgentResults() | intimacy, voca 타입만 추출 |

**플로우**: `userId` → 해당 유저의 모든 `chatrooms` 조회 → 채팅방별로  
`arch_chatrooms` INSERT → `archiveStores` → `archiveUsageEvents` → `archiveIntimacyProgress` → `archiveMessages`(→ 메시지별 `archiveAgentResults`).

---

## 2. 소스 스키마 대비 정합성

### 2.1 채팅·채팅방 관련 (chat_schema)

| 테이블 | user/chatroom 연관 | 아카이브 여부 | 비고 |
|--------|--------------------|---------------|------|
| chatrooms | user_id (CASCADE 삭제) | ✅ arch_chatrooms | |
| messages | chatroom_id (CASCADE) | ✅ arch_messages | |
| intimacy_progress | chatroom_id, user_id (CASCADE) | ✅ arch_intimacy_progress | |
| user_chatbot_last_interaction | user_id | ❌ 아카이브 안 함 | **삭제만** (hardDeleteUser에서 deleteUserChatbotLastInteraction). 보존 불필요. |
| chatbots | 마스터 데이터 | 아카이브 안 함 | 스냅샷만 arch_chatrooms에 저장 |
| flyway_schema_history | 무관 | 아카이브 안 함 | |

### 2.2 보관함 (store_schema)

| 테이블 | 아카이브 여부 | 비고 |
|--------|---------------|------|
| stores | ✅ arch_stores | **WHERE chatroom_id = ? AND is_deleted = false** → 삭제 처리된 보관건은 이관 안 함 (선택 사항). |

### 2.3 빌링 (billing)

| 테이블 | user/chatroom 연관 | 아카이브 여부 | 비고 |
|--------|--------------------|---------------|------|
| ai_usage_events | chatroom_id, user_id (SET NULL) | ✅ arch_usage_events | chatroom_id 기준 조회 후 이관 |
| monthly_user_costs | user_id (PK 일부) | ❌ 아카이브 안 함, **삭제 처리** | `deleteMonthlyUserCosts(userId)` 로 탈퇴 시 DELETE. 고아 행 방지. |

---

## 3. 놓친 부분·갭 요약

### 3.1 데이터 정합성 (반영됨)

| 항목 | 내용 | 조치 |
|------|------|------|
| **billing.monthly_user_costs** | 탈퇴 시 해당 user_id 행이 남을 수 있음 (FK 없음). | ✅ `UserWithdrawalArchiveService.deleteMonthlyUserCosts(userId)` 추가됨. `hardDeleteUser` 3단계에서 호출. |

### 3.2 선택적 보완 (스냅샷 완전성)

| 항목 | 내용 | 비고 |
|------|------|------|
| **chatrooms.context_data** | `queryRoom()`에서 조회하지 않음. `arch_chatrooms.meta`는 `'{}'`로만 저장. | 대화 컨텍스트까지 보존하려면 settings + context_data를 meta jsonb에 넣어서 INSERT하도록 확장 가능. |
| **store_schema.stores (is_deleted = true)** | 현재는 미삭제 보관만 이관. | 삭제된 보관건도 이관할지 정책 결정 후 필요 시 archiveStores 조건 완화. |

### 3.3 구현 상 일치 여부

| 항목 | 상태 |
|------|------|
| arch_chatrooms 컬럼 매핑 | source_*, 스냅샷, concept(settings에서 추출), meta='{}' → 스키마와 일치. |
| arch_messages 컬럼 | source_message_id, source_parent_message_id, metadata_json 등 매핑 일치. |
| arch_agent_results | agent_type 'intimacy', 'voca'만 사용. DB CHECK는 'conver'도 허용 → 문제 없음. latency_ms 미사용(선택). |
| 중복 이관 방지 | arch_chatrooms( source_chatroom_id ), arch_messages( source_message_id ), arch_stores( source_store_id ), arch_usage_events( source_usage_event_id ), arch_intimacy_progress( source_intimacy_progress_id ) 기준으로 이미 있으면 스킵. |

---

## 4. 실행 순서·예외 동작

- **순서**: 채팅방 목록 조회 → 채팅방별로 arch_chatrooms → arch_stores → arch_usage_events → arch_intimacy_progress → arch_messages(+ arch_agent_results).
- **예외**: 각 단계(채팅방 전체, stores, usage_events, intimacy, messages)에서 예외 시 log.warn 후 해당 채팅방/하위만 스킵, **탈퇴는 계속 진행**.
- **트랜잭션**: `archiveUserData`는 `@Transactional`. 한 채팅방 이관 중 실패 시 해당 트랜잭션 롤백 가능. 상위 `hardDeleteUser`와 같은 트랜잭션인지 확인 필요(같다면 아카이브 실패 시 삭제도 롤백).

---

## 5. 요약

| 구분 | 결과 |
|------|------|
| 채팅방·메시지·친밀도·보관·사용량 | ✅ 이관 구현됨, 소스 스키마와 대응 관계 정합 |
| user_chatbot_last_interaction | ✅ 아카이브 제외·삭제만 (설계상 적절) |
| **billing.monthly_user_costs** | ✅ 탈퇴 시 삭제 처리 (deleteMonthlyUserCosts) |
| context_data / meta | 선택적 보강 가능 (스냅샷 완전성) |
| is_deleted=true stores | 정책에 따라 이관 여부 결정 |

이 문서는 igu worktree의 `UserWithdrawalArchiveService` 및 `schema_dump_server.sql` 기준으로 검증한 내용입니다.
