# 서버-로컬 DB 동기화 마이그레이션 요약

**생성일**: 2026-01-14  
**목적**: 서버 DB(운영 환경)를 기준으로 로컬 DB를 동기화

## 생성된 마이그레이션 파일

### 1. User 서비스
- `user/src/main/resources/db/migration/V1__add_prompt_tables.sql`
  - `prompt_versions` 테이블 생성
  - `prompt_actives` 테이블 생성
  - 관련 인덱스 및 제약조건 추가

- `user/src/main/resources/db/migration/V2__sync_user_schema_columns.sql`
  - `app_user` 테이블 컬럼 동기화 (timestamp 타입, DEFAULT 값, NOT NULL 제약)
  - `profiles` 테이블 컬럼 동기화
  - `settings` 테이블 컬럼 동기화
  - 제약조건 이름 통일 (`chk_app_user_status`)
  - 서버 인덱스 추가 (`app_user_email_idx`)

### 2. Auth 서비스
- `auth/src/main/resources/db/migration/V1__sync_auth_schema.sql`
  - 모든 auth_schema 테이블의 timestamp 타입을 `timestamp(6)`으로 변경
  - DEFAULT 값 제거 (서버 기준)
  - 영향받는 테이블:
    - `auth_events`
    - `email_verifications`
    - `login_attempts`
    - `password_reset_tokens`
    - `refresh_tokens`
    - `token_blacklist`

### 3. Chat 서비스
- `chat/src/main/resources/db/migration/V7__sync_chat_schema_columns_and_indexes.sql`
  - 제약조건 이름 통일
  - 서버에만 있는 인덱스 추가:
    - `idx_messages_room_turn`
    - `idx_messages_created_at`
    - `idx_messages_chatroom`
    - `idx_messages_sender`
    - `idx_ucli_user_last_ts`

### 4. Batch 서비스
- `batch/src/main/resources/db/migration/V1__create_archive_schema.sql`
  - `archive_schema` 스키마 생성
  - `arch_chatrooms` 테이블 생성
  - `arch_messages` 테이블 생성
  - `arch_agent_results` 테이블 생성
  - `arch_ingestion_state` 테이블 생성
  - 모든 인덱스 및 제약조건 생성
  - 권한 부여

## Flyway 설정 필요사항

다음 서비스들에 Flyway 설정이 필요합니다:

### User 서비스
`user/src/main/resources/application.yml`에 추가:
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    schemas: user_schema
```

### Auth 서비스
`auth/src/main/resources/application.yml`에 추가:
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    schemas: auth_schema
```

### Batch 서비스
`batch/src/main/resources/application.yml`에 추가:
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    schemas: archive_schema, batch_schema
```

## 마이그레이션 실행 순서

1. **User 서비스**: V1, V2 순서로 실행
2. **Auth 서비스**: V1 실행
3. **Chat 서비스**: V7 실행 (기존 V1-V6 이후)
4. **Batch 서비스**: V1 실행

## 주의사항

1. **데이터 보존**: 모든 ALTER TABLE 작업은 기존 데이터를 보존합니다.
2. **timestamp 타입 변경**: `timestamp` → `timestamp(6)` 변경은 데이터 손실 없이 수행됩니다.
3. **DEFAULT 값 제거**: 기존 데이터는 유지되지만, 새로 삽입되는 데이터는 DEFAULT 값이 없습니다.
4. **제약조건 변경**: 기존 제약조건을 삭제하고 새로 생성하므로, 데이터 무결성을 확인해야 합니다.

## 검증 방법

마이그레이션 실행 후:
1. DDL 추출 스크립트 재실행
2. 비교 리포트 재생성
3. 서버와 로컬 DDL이 일치하는지 확인

## 롤백 방법

필요시 각 마이그레이션 파일의 반대 작업을 수행하는 롤백 스크립트를 작성할 수 있습니다.
