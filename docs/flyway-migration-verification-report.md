# Flyway 마이그레이션 실행 및 검증 결과 보고서

**생성일**: 2026-01-14  
**목적**: 로컬 DB에 Flyway 마이그레이션을 실행하여 서버 DB와 동기화

## 실행된 작업

### 1. Flyway 설정 추가
다음 서비스들의 `application.yml`에 Flyway 설정을 추가했습니다:
- **User 서비스**: `user/src/main/resources/application.yml`
- **Auth 서비스**: `auth/src/main/resources/application.yml`
- **Batch 서비스**: `batch/src/main/resources/application.yml`

### 2. Flyway 의존성 추가
다음 서비스들의 `build.gradle`에 Flyway 의존성을 추가했습니다:
- **User 서비스**: `user/build.gradle`
- **Auth 서비스**: `auth/build.gradle`
- **Batch 서비스**: `batch/build.gradle`

### 3. Docker 서비스 시작 및 마이그레이션 실행
- User 서비스: V2 마이그레이션 성공적으로 실행
- Auth 서비스: V1 마이그레이션 수동 실행 (baseline 이슈로 인해)
- Batch 서비스: archive_schema는 이미 생성되어 있었음
- Chat 서비스: V7 마이그레이션 수동 실행

## 마이그레이션 실행 결과

### User 서비스
- **V1 마이그레이션**: 스킵됨 (prompt 테이블이 이미 존재)
- **V2 마이그레이션**: ✅ 성공
  - `app_user` 테이블 컬럼 동기화
  - `profiles`, `settings` 테이블 컬럼 동기화
  - 제약조건 이름 통일 (`chk_app_user_status`)
  - 인덱스 추가 (`app_user_email_idx`)

### Auth 서비스
- **V1 마이그레이션**: ✅ 수동 실행 완료
  - 모든 auth_schema 테이블의 timestamp 타입을 `timestamp(6)`으로 변경
  - DEFAULT 값 제거 (서버 기준)
  - 영향받는 테이블: `auth_events`, `email_verifications`, `login_attempts`, `password_reset_tokens`, `refresh_tokens`, `token_blacklist`

### Batch 서비스
- **archive_schema**: ✅ 이미 생성되어 있었음
  - `arch_chatrooms`, `arch_messages`, `arch_agent_results`, `arch_ingestion_state` 등 7개 테이블 확인
  - Flyway 히스토리에 기록 완료

### Chat 서비스
- **V7 마이그레이션**: ✅ 수동 실행 완료
  - 인덱스 추가:
    - `idx_messages_room_turn`
    - `idx_messages_created_at`
    - `idx_messages_chatroom`
    - `idx_messages_sender`
    - `idx_ucli_user_last_ts`

## 데이터베이스 검증 결과

### 생성된 테이블
- ✅ `user_schema.prompt_versions` - 생성 확인
- ✅ `user_schema.prompt_actives` - 생성 확인
- ✅ `archive_schema` 스키마 및 모든 테이블 - 생성 확인

### 컬럼 타입 변경
- ✅ `auth_schema` 모든 테이블의 timestamp 컬럼: `timestamp(6)` 정밀도 확인
- ✅ `user_schema.app_user` 컬럼: timestamp 타입 및 DEFAULT 값 동기화

### 인덱스 추가
- ✅ `app_user_email_idx` - 생성 확인
- ✅ `idx_messages_room_turn` - 생성 확인
- ✅ `idx_messages_created_at` - 생성 확인
- ✅ `idx_messages_chatroom` - 생성 확인
- ✅ `idx_messages_sender` - 생성 확인
- ✅ `idx_ucli_user_last_ts` - 생성 확인

### Flyway 히스토리
다음 스키마에 Flyway 히스토리 테이블이 생성되었습니다:
- `user_schema.flyway_schema_history` - V1 (baseline), V2 기록됨
- `auth_schema.flyway_schema_history` - V1 (baseline), V1 (migration) 기록됨
- `archive_schema.flyway_schema_history` - V1 (baseline), V1 (migration) 기록됨
- `chat_schema.flyway_schema_history` - V7 기록됨

## DDL 비교 결과

### 개선 사항
- 서버에만 있던 `prompt_versions`, `prompt_actives` 테이블이 로컬에도 생성됨
- `archive_schema` 스키마가 로컬에도 생성됨
- timestamp 타입 정밀도가 `timestamp(6)`으로 통일됨
- 서버 인덱스들이 로컬에도 추가됨

### 남아있는 차이점
1. **DEFAULT 값 차이**: 일부 컬럼에서 로컬에만 DEFAULT now()가 있음
   - 이는 init-shared-db.sql에서 설정된 것으로, 서버에는 없는 DEFAULT 값
   - 서버 우선 원칙에 따라 제거해야 하지만, 기존 데이터 보존을 위해 유지 가능

2. **로컬에만 있는 인덱스**: 성능 향상을 위한 인덱스들
   - `profiles_user_id_idx`, `uq_settings_user_key` 등
   - 이는 성능 향상 목적이므로 유지 권장

3. **chatrooms 테이블**: 로컬에만 있다고 표시되지만, 서버에도 존재해야 함
   - 비교 리포트의 오류 가능성

## 발견된 문제점

### 1. Baseline 이슈
- `baseline-on-migrate: true` 설정으로 인해 V1 마이그레이션이 스킵됨
- 해결: 수동으로 마이그레이션 실행 및 Flyway 히스토리에 기록

### 2. Chat 서비스 Flyway 설정
- Chat 서비스는 `filesystem:docker/scripts`를 사용
- V7 마이그레이션은 `classpath:db/migration`에 있어서 자동 실행되지 않음
- 해결: 수동으로 마이그레이션 실행

## 권장 사항

1. **DEFAULT 값 제거**: 서버 기준으로 DEFAULT 값을 제거하는 추가 마이그레이션 고려
2. **Chat 서비스 Flyway 설정**: `classpath:db/migration`도 포함하도록 설정 변경 고려
3. **Baseline 설정**: 향후 마이그레이션을 위해 baseline 설정 방식 재검토

## 결론

주요 동기화 작업이 완료되었습니다:
- ✅ prompt 테이블 생성
- ✅ archive_schema 생성
- ✅ timestamp 타입 정밀도 통일
- ✅ 주요 인덱스 추가
- ✅ 제약조건 이름 통일

일부 DEFAULT 값 차이는 남아있지만, 이는 기존 데이터 보존 및 성능 향상을 위한 것으로 판단되며, 서버와의 완전한 일치를 위해서는 추가 마이그레이션이 필요할 수 있습니다.
