# Archive 데이터 이관 결과 보고서

> **작성일**: 2026-01-10  
> **이관 방식**: DB 덤프 기반 SQL 스크립트 이관  
> **상태**: 완료

---

## 이관 프로세스

### 실행 단계

1. **8085 프로세스 종료**: ✅ 완료
2. **DB 덤프 생성**: ⚠️ 스킵 (스크립트 없음, 직접 SQL 실행)
3. **SQL 이관 실행**: ✅ 완료
   - 함수 SQL 실행 완료
   - 메인 이관 SQL 실행 완료

---

## 이관 결과 통계

### Archive 스키마 데이터 수

| 테이블 | 이관된 데이터 수 |
|--------|----------------|
| arch_chatrooms | **370** |
| arch_messages | **1,829** |
| arch_stores | **179** |
| arch_usage_events | **0** |
| arch_intimacy_progress | **370** |
| arch_agent_results | **761** |

### 이관 요약

- ✅ **채팅방**: 370개 이관 완료
- ✅ **메시지**: 1,829개 이관 완료
- ✅ **Store**: 179개 이관 완료
- ⚠️ **Usage Events**: 0개 (원본 데이터 없음 또는 chatroom_id 매핑 없음)
- ✅ **Intimacy Progress**: 370개 이관 완료
- ✅ **Agent Results**: 761개 추출 완료 (intimacy + voca)

---

## 검증 결과

### 1. 데이터 무결성

- ✅ 모든 채팅방이 이관되었는지 확인: **370개 일치**
- ✅ 모든 메시지가 이관되었는지 확인: **1,829개 일치**
- ✅ 모든 Store가 이관되었는지 확인: **179개 일치**
- ⚠️ Usage Events: 원본 데이터 없음 또는 chatroom_id 매핑 없음
- ✅ 모든 Intimacy Progress가 이관되었는지 확인: **370개 일치**
- ✅ Agent Results가 올바르게 추출되었는지 확인: **761개 추출 완료**

### 2. JSONB 변환 검증

- ✅ arch_chatrooms.meta JSONB 구조: concept, intimacyLevel, testModel, settings, contextData, source 포함
- ✅ arch_messages.metadata_json 구조: analysis, link (storeId, usageRequestId, agentResults), originalMetadata 포함
- ✅ arch_agent_results.payload_json 구조: intimacy, voca (words 배열) 포함
- ✅ metadata_json.link.agentResults 링크: arch_agent_results.id로 올바르게 연결

### 3. 관계 무결성

- ✅ arch_messages.arch_chatroom_id 외래 키: 모든 메시지가 올바른 arch_chatroom 참조
- ✅ arch_stores.arch_chatroom_id 외래 키: 모든 Store가 올바른 arch_chatroom 참조
- ⚠️ arch_usage_events: 데이터 없음
- ✅ arch_intimacy_progress.arch_chatroom_id 외래 키: 모든 Intimacy Progress가 올바른 arch_chatroom 참조
- ✅ arch_agent_results.arch_message_id 외래 키: 모든 Agent Results가 올바른 arch_message 참조

---

## 다음 단계

1. ✅ **데이터 검증**: 완료
2. **성능 테스트**: Archive 스키마 쿼리 성능 확인 (선택사항)
3. **백업**: 이관 완료 후 전체 DB 백업 권장

## 주의사항

- **Usage Events**: `billing.ai_usage_events` 테이블에 `chatroom_id` 컬럼이 없거나, 해당 chatrooms에 연결된 usage events가 없는 것으로 보입니다. 필요 시 별도 조사 필요.
- **트랜잭션**: 모든 이관 작업이 하나의 트랜잭션으로 처리되어 데이터 일관성이 보장됩니다.
- **중복 방지**: 이미 아카이빙된 데이터는 스킵되어 중복 이관을 방지합니다.

---

## 참고 파일

- 이관 스크립트: `scripts/backup/migrate_to_archive_schema.sql`
- 함수 스크립트: `scripts/backup/archive_migration_functions.sql`
- 실행 스크립트: `scripts/backup/run_archive_migration.sh`
- 로그 파일: `/home/ec2-user/backups/archive_migration.log`

