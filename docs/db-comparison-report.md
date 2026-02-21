# 데이터베이스 DDL 비교 리포트

생성 일시: 2026-01-14 23:28:04

## 요약

- 서버 DB 테이블 수: 18
- 로컬 DB 테이블 수: 21
- 공통 테이블 수: 17
- 차이가 있는 테이블 수: 10
- 서버에만 있는 테이블 수: 4
- 로컬에만 있는 테이블 수: 1

## 서버에만 있는 테이블

### user_schema

- `user_schema.flyway_schema_history`
- `user_schema.prompt_versions`
- `user_schema.prompt_actives`

### auth_schema

- `auth_schema.flyway_schema_history`

## 로컬에만 있는 테이블

### chat_schema

- `chat_schema.chatrooms`

## 테이블 차이점

### chat_schema

#### chat_schema.chatbots

**로컬에 없는 컬럼:**
- `CONSTRAINT`

### user_schema

#### user_schema.app_user

**컬럼 정의 차이:**
- `updated_at`:
  - 서버: `timestamp without time zone DEFAULT now() NOT NULL`
  - 로컬: `timestamp without time zone NOT NULL`
- `last_conn_time`:
  - 서버: `timestamp without time zone DEFAULT now() NOT NULL`
  - 로컬: `timestamp without time zone NOT NULL`
- `signup_question`:
  - 서버: `character varying(255) DEFAULT '吏덈Ц???ㅼ젙?섏? ?딆븯?듬땲??'::character varying NOT NULL`
  - 로컬: `character varying(255) DEFAULT '??????????? ????????'::character varying NOT NULL`
- `signup_answer`:
  - 서버: `character varying(30) DEFAULT '?듬????ㅼ젙?섏? ?딆븯?듬땲??'::character varying NOT NULL`
  - 로컬: `character varying(30) DEFAULT '??????????? ????????'::character varying NOT NULL`
- `created_at`:
  - 서버: `timestamp without time zone DEFAULT now() NOT NULL`
  - 로컬: `timestamp without time zone NOT NULL`

#### user_schema.settings

**인덱스 차이:**
- 로컬에만 있는 인덱스:
  - `uq_settings_user_key`
  - `settings_user_id_idx`

#### user_schema.profiles

**인덱스 차이:**
- 로컬에만 있는 인덱스:
  - `profiles_user_id_idx`

### auth_schema

#### auth_schema.refresh_tokens

**컬럼 정의 차이:**
- `issued_at`:
  - 서버: `timestamp(6) without time zone NOT NULL`
  - 로컬: `timestamp(6) without time zone DEFAULT now() NOT NULL`

**인덱스 차이:**
- 로컬에만 있는 인덱스:
  - `idx_refresh_expires`
  - `idx_refresh_revoked`
  - `idx_refresh_user`

#### auth_schema.token_blacklist

**컬럼 정의 차이:**
- `created_at`:
  - 서버: `timestamp(6) without time zone NOT NULL`
  - 로컬: `timestamp(6) without time zone DEFAULT now() NOT NULL`

**인덱스 차이:**
- 로컬에만 있는 인덱스:
  - `idx_blacklist_expires`
  - `uq_blacklist_token_hash`

#### auth_schema.auth_events

**컬럼 정의 차이:**
- `created_at`:
  - 서버: `timestamp(6) without time zone NOT NULL`
  - 로컬: `timestamp(6) without time zone DEFAULT now() NOT NULL`

**인덱스 차이:**
- 로컬에만 있는 인덱스:
  - `idx_auth_events_type`
  - `idx_auth_events_created`
  - `idx_auth_events_user`

#### auth_schema.password_reset_tokens

**컬럼 정의 차이:**
- `created_at`:
  - 서버: `timestamp(6) without time zone NOT NULL`
  - 로컬: `timestamp(6) without time zone DEFAULT now() NOT NULL`

**인덱스 차이:**
- 로컬에만 있는 인덱스:
  - `idx_pwreset_expires`
  - `idx_pwreset_user`
  - `idx_pwreset_used`

#### auth_schema.email_verifications

**컬럼 정의 차이:**
- `verified`:
  - 서버: `boolean NOT NULL`
  - 로컬: `boolean DEFAULT false NOT NULL`
- `created_at`:
  - 서버: `timestamp(6) without time zone NOT NULL`
  - 로컬: `timestamp(6) without time zone DEFAULT now() NOT NULL`

**인덱스 차이:**
- 로컬에만 있는 인덱스:
  - `idx_emailver_user`
  - `uq_emailver_token_hash`

#### auth_schema.login_attempts

**컬럼 정의 차이:**
- `created_at`:
  - 서버: `timestamp(6) without time zone NOT NULL`
  - 로컬: `timestamp(6) without time zone DEFAULT now() NOT NULL`

**인덱스 차이:**
- 로컬에만 있는 인덱스:
  - `idx_login_attempts_email`
  - `idx_login_attempts_user`
  - `idx_login_attempts_created`

## 결론

⚠️ **서버 DB와 로컬 DB의 DDL에 차이가 있습니다.**
위의 차이점을 확인하고 필요시 동기화하세요.
