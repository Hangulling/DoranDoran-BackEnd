# DoranDoran API 명세서 V3 (추가 기능 기준)

이 문서는 이번에 추가 구현된 기능들의 API 명세와, **현재 서버의 실제 스키마 요약**을 포함합니다.  
스키마 덤프 원본은 `docs/schema_dump_server.sql`에 보관되어 있습니다.

## 1. 개요
- **Gateway**: `http://localhost:8080`
- **Auth**: `http://localhost:8081`
- **User**: `http://localhost:8082`
- **Chat**: `http://localhost:8083`

## 2. 인증/헤더
Gateway는 JWT에서 아래 헤더를 주입합니다.
- `X-User-Id`
- `X-User-Email`
- `X-User-Name`
- `X-Auth-Ts`, `X-Auth-Sign` (HMAC)

## 3. 공통 응답 형식
```json
{
  "success": true,
  "data": {},
  "message": "성공",
  "errorCode": null,
  "timestamp": "2026-01-19T12:00:00"
}
```

## 4. 공통 에러 코드
- `U001` 사용자 없음
- `U002` 이메일 중복
- `U003` 비밀번호 오류
- `U004` 비밀번호 포맷 오류
- `U005` 이미 비활성
- `U006` 이미 정지
- `U007` 비활성 계정
- `A001` 토큰 만료
- `A002` 토큰 유효하지 않음
- `A003` 접근 권한 없음
- `A004` 소셜 로그인 비밀번호 재설정 불가
- `A005` 인증 코드 오류
- `A006` 인증 코드 만료
- `C001` 채팅방 없음
- `C002` 메시지 없음
- `S001` 스토어 항목 없음
- `E001` 내부 오류
- `E002` 잘못된 요청
- `E003` 검증 실패

---

## 5. 추가 기능 API

### 5.1 메인홈 게시글

#### 5.1.1 게시글 6개 조회
```
GET /api/home/posts
```
**응답**
```json
[
  {
    "externalId": "17890000000000000",
    "title": "게시글 제목",
    "imageUrl": "https://...",
    "description": "캡션 전체",
    "permalink": "https://www.instagram.com/p/...",
    "publishedAt": "2026-01-19T00:00:00"
  }
]
```

#### 5.1.2 게시글 상세 조회
```
GET /api/home/posts/{externalId}
```
**응답**
```json
{
  "externalId": "17890000000000000",
  "title": "게시글 제목",
  "imageUrl": "https://...",
  "description": "캡션 전체",
  "permalink": "https://www.instagram.com/p/...",
  "publishedAt": "2026-01-19T00:00:00"
}
```

#### 5.1.3 visit instagram 이동
클라이언트가 `permalink`로 이동.

---

### 5.2 문의/신고

#### 5.2.1 문의/신고 생성
```
POST /api/support
```
**헤더**
- `X-User-Id` (필수)
- `X-User-Email` (선택, 없으면 DB에서 조회)
- `X-User-Name` (선택, 없으면 DB에서 조회)

**요청**
```json
{
  "type": "INQUIRY",
  "category": "일반문의",
  "content": "문의 내용",
  "replyRequested": true,
  "replyEmail": "reply@example.com",
  "chatroomId": "uuid",
  "messageId": "uuid",
  "messageContent": "신고 대상 메시지",
  "aiResponseSnapshot": "{}"
}
```
**규칙**
- `type=REPORT`일 때 `category`, `messageId`, `messageContent` 필수

**응답**
```json
{
  "success": true,
  "data": {
    "id": 100,
    "createdAt": "2026-01-19T12:00:00"
  },
  "message": "문의가 접수되었습니다."
}
```

#### 5.2.2 문의 시 이메일 조회
```
GET /api/users/{userId}/email
```
**응답**
```json
{
  "success": true,
  "data": "user@example.com",
  "message": "사용자 이메일 조회 성공"
}
```

---

### 5.3 마이페이지

#### 5.3.1 관심 주제 조회
```
GET /api/users/{userId}/interests
```
**응답**
```json
{
  "topics": [
    { "key": "travel", "label": "여행" }
  ]
}
```

#### 5.3.2 관심 주제 저장
```
PUT /api/users/{userId}/interests
```
**요청**
```json
{ "topicKeys": ["travel", "food"] }
```
**응답**
```json
{
  "topics": [
    { "key": "travel", "label": "여행" },
    { "key": "food", "label": "음식" }
  ]
}
```

#### 5.3.3 알림 설정 조회
```
GET /api/users/{userId}/notifications
```
**응답**
```json
{ "pushEnabled": true }
```

#### 5.3.4 알림 설정 변경
```
PUT /api/users/{userId}/notifications
```
**요청**
```json
{ "pushEnabled": true }
```
**응답**
```json
{ "pushEnabled": true }
```

---

### 5.4 알림 (푸시)

#### 5.4.1 FCM 토큰 등록
```
POST /api/notifications/register
```
**요청**
```json
{ "token": "fcm_token", "platform": "android" }
```

#### 5.4.2 푸시 발송 (내부)
```
POST /api/notifications/send
```
**요청**
```json
{
  "userId": "uuid",
  "title": "채팅방",
  "body": "메시지",
  "chatroomId": "uuid",
  "messageId": "uuid"
}
```

#### 5.4.3 푸시 발송 로그 조회
```
GET /api/notifications/logs?userId={uuid}&page=0&size=20
```

#### 5.4.4 푸시 payload (FCM data)
```json
{
  "deeplink": "dorandoran://chat?roomId=...",
  "universalLink": "https://www.doran-chat.com/chat?roomId=...",
  "chatroomId": "uuid",
  "messageId": "uuid",
  "startMessage": "푸시 body 그대로",
  "sentAt": "2026-01-19T12:00:00+09:00"
}
```
**클라이언트 동작**
- 푸시 클릭 → `chatroomId`로 이동
- `startMessage`를 챗봇에게 전송하여 대화 시작

---

### 5.5 메인홈 통계

#### 5.5.1 연속 접속/퍼펙트 조회
```
GET /api/users/{userId}/stats
```
**응답**
```json
{
  "streakCount": 5,
  "perfectCount": 12,
  "lastActiveDate": "2026-01-19"
}
```

#### 5.5.2 퍼펙트 증가
```
POST /api/users/{userId}/stats/perfect
```

---

### 5.6 채팅

#### 5.6.1 메시지 전송 취소
```
POST /api/chat/messages/{messageId}/cancel
POST /api/chat/chatrooms/{chatroomId}/messages/{messageId}/cancel
```

#### 5.6.2 메시지 단건 조회
```
GET /api/chat/messages/{messageId}
```

---

## 6. DB 스키마/테이블 목록 (서버 덤프 기준)
덤프 파일: `docs/schema_dump_server.sql`

### 6.1 스키마
- `archive_schema`
- `auth_schema`
- `batch_schema`
- `billing`
- `chat_schema`
- `store_schema`
- `user_schema`

### 6.4 스키마별 DDL (CREATE TABLE)

아래는 서버 덤프(`docs/schema_dump_server.sql`) 기준의 각 테이블 CREATE TABLE DDL입니다.

#### 6.4.1 `archive_schema`

**arch_agent_results**
```sql
CREATE TABLE archive_schema.arch_agent_results (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    arch_message_id uuid NOT NULL,
    agent_type character varying(20) NOT NULL,
    payload_json jsonb NOT NULL,
    request_id text,
    provider text,
    model text,
    latency_ms integer,
    input_tokens integer,
    output_tokens integer,
    source_created_at timestamp without time zone,
    archived_at timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT arch_agent_type_check CHECK (((agent_type)::text = ANY ((ARRAY['intimacy'::character varying, 'conver'::character varying, 'voca'::character varying])::text[])))
);
```

**arch_chatrooms**
```sql
CREATE TABLE archive_schema.arch_chatrooms (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    source_chatroom_id uuid NOT NULL,
    user_id uuid,
    user_email_snapshot character varying(320),
    chatbot_id uuid,
    chatbot_name_snapshot character varying(100),
    chatbot_type_snapshot character varying(20),
    chatbot_intimacy_level_snapshot integer,
    name character varying(100) NOT NULL,
    description text,
    concept character varying(50),
    last_message_at timestamp without time zone,
    source_last_message_id uuid,
    is_archived boolean DEFAULT false NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    source_created_at timestamp without time zone,
    source_updated_at timestamp without time zone,
    archived_at timestamp without time zone DEFAULT now() NOT NULL,
    source_deleted_at timestamp without time zone,
    meta jsonb DEFAULT '{}'::jsonb NOT NULL
);
```

**arch_ingestion_state**
```sql
CREATE TABLE archive_schema.arch_ingestion_state (
    id bigint NOT NULL,
    job_name text NOT NULL,
    last_source_chatroom_id uuid,
    last_source_message_id uuid,
    last_source_message_created_at timestamp without time zone,
    status text DEFAULT 'RUNNING'::text NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    note text
);
```

**arch_intimacy_progress**
```sql
CREATE TABLE archive_schema.arch_intimacy_progress (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    arch_chatroom_id uuid NOT NULL,
    source_intimacy_progress_id uuid NOT NULL,
    user_id uuid NOT NULL,
    intimacy_level integer DEFAULT 1 NOT NULL,
    total_corrections integer DEFAULT 0,
    last_feedback text,
    last_updated timestamp without time zone,
    progress_data jsonb,
    archived_at timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT arch_intimacy_level_check CHECK ((intimacy_level = ANY (ARRAY[1, 2, 3])))
);
```

**arch_messages**
```sql
CREATE TABLE archive_schema.arch_messages (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    arch_chatroom_id uuid NOT NULL,
    source_message_id uuid NOT NULL,
    source_parent_message_id uuid,
    sender_type character varying(20) NOT NULL,
    sender_id uuid,
    content text NOT NULL,
    content_type character varying(20) DEFAULT 'text'::character varying NOT NULL,
    sequence_number bigint NOT NULL,
    turn_number bigint DEFAULT 0 NOT NULL,
    token_count integer,
    processing_time_ms integer,
    is_edited boolean DEFAULT false NOT NULL,
    edited_at timestamp without time zone,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone,
    source_created_at timestamp without time zone,
    source_updated_at timestamp without time zone,
    archived_at timestamp without time zone DEFAULT now() NOT NULL,
    metadata_json jsonb DEFAULT '{}'::jsonb NOT NULL,
    CONSTRAINT arch_messages_content_type_check CHECK (((content_type)::text = ANY ((ARRAY['text'::character varying, 'code'::character varying, 'system'::character varying, 'json'::character varying])::text[]))),
    CONSTRAINT arch_messages_sender_type_check CHECK (((sender_type)::text = ANY ((ARRAY['user'::character varying, 'bot'::character varying, 'system'::character varying])::text[])))
);
```

**arch_stores**
```sql
CREATE TABLE archive_schema.arch_stores (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    arch_chatroom_id uuid NOT NULL,
    source_store_id uuid NOT NULL,
    source_message_id uuid NOT NULL,
    user_id uuid NOT NULL,
    content text NOT NULL,
    corrected_content text,
    ai_response jsonb NOT NULL,
    bot_type character varying(20) NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone,
    source_created_at timestamp without time zone,
    source_updated_at timestamp without time zone,
    archived_at timestamp without time zone DEFAULT now() NOT NULL
);
```

**arch_usage_events**
```sql
CREATE TABLE archive_schema.arch_usage_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    arch_chatroom_id uuid NOT NULL,
    source_usage_event_id uuid NOT NULL,
    user_id uuid NOT NULL,
    event_time timestamp without time zone NOT NULL,
    provider text NOT NULL,
    model text NOT NULL,
    request_id text,
    input_tokens integer DEFAULT 0 NOT NULL,
    output_tokens integer DEFAULT 0 NOT NULL,
    cost_in numeric(18,6) DEFAULT 0 NOT NULL,
    cost_out numeric(18,6) DEFAULT 0 NOT NULL,
    meta jsonb,
    archived_at timestamp without time zone DEFAULT now() NOT NULL
);
```

**management_queue**
```sql
CREATE TABLE archive_schema.management_queue (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    queue_type character varying(50) NOT NULL,
    status character varying(20) NOT NULL,
    request_data jsonb NOT NULL,
    result_data jsonb,
    admin_name character varying(100),
    admin_ip character varying(45),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone,
    completed_at timestamp without time zone,
    error_message text
);
```

#### 6.4.2 `auth_schema`

**auth_events**
```sql
CREATE TABLE auth_schema.auth_events (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    event_type character varying(50) NOT NULL,
    metadata text,
    user_id uuid
);
```

**email_verifications**
```sql
CREATE TABLE auth_schema.email_verifications (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    token_hash character varying(128) NOT NULL,
    verified boolean NOT NULL,
    user_id uuid NOT NULL
);
```

**login_attempts**
```sql
CREATE TABLE auth_schema.login_attempts (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    email character varying(320),
    ip_address character varying(255),
    succeeded boolean NOT NULL,
    user_agent character varying(500),
    user_id uuid
);
```

**password_reset_tokens**
```sql
CREATE TABLE auth_schema.password_reset_tokens (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    token_hash character varying(128) NOT NULL,
    used boolean NOT NULL,
    user_id uuid NOT NULL
);
```

**refresh_tokens**
```sql
CREATE TABLE auth_schema.refresh_tokens (
    id bigint NOT NULL,
    device_id character varying(200),
    expires_at timestamp(6) without time zone NOT NULL,
    ip_address character varying(255),
    issued_at timestamp(6) without time zone NOT NULL,
    revoked boolean NOT NULL,
    rotated_from_id bigint,
    token text NOT NULL,
    user_agent character varying(500),
    user_id uuid NOT NULL
);
```

**token_blacklist**
```sql
CREATE TABLE auth_schema.token_blacklist (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    reason character varying(200),
    token_hash character varying(128) NOT NULL,
    token_type character varying(20) NOT NULL
);
```

#### 6.4.3 `billing`

**ai_usage_events**
```sql
CREATE TABLE billing.ai_usage_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    event_time timestamp without time zone DEFAULT now() NOT NULL,
    user_id uuid NOT NULL,
    chatroom_id uuid NOT NULL,
    provider text NOT NULL,
    model text NOT NULL,
    request_id text,
    input_tokens integer DEFAULT 0 NOT NULL,
    output_tokens integer DEFAULT 0 NOT NULL,
    cost_in numeric(18,6) DEFAULT 0 NOT NULL,
    cost_out numeric(18,6) DEFAULT 0 NOT NULL,
    meta jsonb
);
```

**monthly_user_costs**
```sql
CREATE TABLE billing.monthly_user_costs (
    billing_month date NOT NULL,
    user_id uuid NOT NULL,
    input_tokens bigint DEFAULT 0 NOT NULL,
    output_tokens bigint DEFAULT 0 NOT NULL,
    cost_in numeric(18,6) DEFAULT 0 NOT NULL,
    cost_out numeric(18,6) DEFAULT 0 NOT NULL,
    total_cost numeric(18,6) DEFAULT 0 NOT NULL,
    last_aggregated_at timestamp without time zone DEFAULT now() NOT NULL
);
```

#### 6.4.4 `chat_schema`

**chatbots**
```sql
CREATE TABLE chat_schema.chatbots (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    display_name character varying(100) NOT NULL,
    description text,
    bot_type character varying(50) NOT NULL,
    model_name character varying(100),
    personality jsonb,
    system_prompt text,
    capabilities jsonb,
    settings jsonb,
    intimacy_level integer DEFAULT 1,
    avatar_url character varying(500),
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    created_by uuid,
    intimacy_system_prompt text,
    intimacy_user_prompt text,
    vocabulary_system_prompt text,
    vocabulary_user_prompt text,
    translation_system_prompt text,
    translation_user_prompt text,
    CONSTRAINT chatbots_intimacy_level_check CHECK (((intimacy_level >= 1) AND (intimacy_level <= 3))),
    CONSTRAINT chk_chatbots_type CHECK (((bot_type)::text = ANY (ARRAY[('gpt'::character varying)::text, ('claude'::character varying)::text, ('custom'::character varying)::text])))
);
```

**chatrooms**
```sql
CREATE TABLE chat_schema.chatrooms (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    chatbot_id uuid NOT NULL,
    user_id uuid NOT NULL,
    settings jsonb DEFAULT '{}'::jsonb,
    context_data jsonb,
    last_message_at timestamp without time zone,
    last_message_id uuid,
    is_archived boolean DEFAULT false,
    is_deleted boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now()
);
```

**flyway_schema_history**
```sql
CREATE TABLE chat_schema.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);
```

**intimacy_progress**
```sql
CREATE TABLE chat_schema.intimacy_progress (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    chatroom_id uuid NOT NULL,
    user_id uuid NOT NULL,
    intimacy_level integer DEFAULT 1 NOT NULL,
    total_corrections integer DEFAULT 0,
    last_feedback text,
    last_updated timestamp without time zone DEFAULT now(),
    progress_data jsonb,
    CONSTRAINT intimacy_progress_intimacy_level_check CHECK ((intimacy_level = ANY (ARRAY[1, 2, 3])))
);
```

**messages**
```sql
CREATE TABLE chat_schema.messages (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    chatroom_id uuid NOT NULL,
    sender_type character varying(20) NOT NULL,
    sender_id uuid,
    content text NOT NULL,
    content_type character varying(20) DEFAULT 'text'::character varying,
    metadata jsonb,
    sequence_number bigint NOT NULL,
    token_count integer,
    processing_time_ms integer,
    is_edited boolean DEFAULT false,
    edited_at timestamp without time zone,
    is_deleted boolean DEFAULT false,
    deleted_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    parent_message_id uuid,
    turn_number bigint DEFAULT 0 NOT NULL,
    is_cancelled boolean DEFAULT false NOT NULL,
    cancelled_at timestamp without time zone,
    CONSTRAINT messages_content_type_check CHECK (((content_type)::text = ANY (ARRAY[('text'::character varying)::text, ('code'::character varying)::text, ('system'::character varying)::text, ('json'::character varying)::text]))),
    CONSTRAINT messages_sender_type_check CHECK (((sender_type)::text = ANY (ARRAY[('user'::character varying)::text, ('bot'::character varying)::text, ('system'::character varying)::text])))
);
```

**user_chatbot_last_interaction**
```sql
CREATE TABLE chat_schema.user_chatbot_last_interaction (
    user_id uuid NOT NULL,
    chatbot_id uuid NOT NULL,
    last_interaction_at timestamp with time zone,
    last_room_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);
```

#### 6.4.5 `store_schema`

**stores**
```sql
CREATE TABLE store_schema.stores (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    message_id uuid NOT NULL,
    chatroom_id uuid NOT NULL,
    content text NOT NULL,
    corrected_content text,
    ai_response jsonb NOT NULL,
    bot_type character varying(20),
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);
```

#### 6.4.6 `user_schema`

**admin_audit_logs**
```sql
CREATE TABLE user_schema.admin_audit_logs (
    id bigint NOT NULL,
    admin_user_id uuid NOT NULL,
    action_type character varying(50) NOT NULL,
    target_type character varying(50),
    target_id bigint,
    summary character varying(500),
    before_json jsonb,
    after_json jsonb,
    ip character varying(50),
    user_agent character varying(500),
    created_at timestamp without time zone DEFAULT now() NOT NULL
);
```

**admin_roles**
```sql
CREATE TABLE user_schema.admin_roles (
    id bigint NOT NULL,
    role_name character varying(50) NOT NULL,
    description character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL
);
```

**admin_user_roles**
```sql
CREATE TABLE user_schema.admin_user_roles (
    admin_user_id bigint NOT NULL,
    admin_role_id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);
```

**admin_users**
```sql
CREATE TABLE user_schema.admin_users (
    id bigint NOT NULL,
    username character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);
```

**app_user**
```sql
CREATE TABLE user_schema.app_user (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    email character varying(320) NOT NULL,
    first_name character varying(50) NOT NULL,
    last_name character varying(50) NOT NULL,
    name character varying(50) NOT NULL,
    password_hash character varying(100),
    picture character varying(255),
    info character varying(100) DEFAULT ''::character varying NOT NULL,
    last_conn_time timestamp without time zone DEFAULT now() NOT NULL,
    status character varying(255) DEFAULT 'ACTIVE'::character varying NOT NULL,
    role character varying(20) DEFAULT 'ROLE_USER'::character varying NOT NULL,
    coach_check boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    exit_modal_do_not_show_again boolean DEFAULT false NOT NULL,
    oauth_id character varying(255),
    oauth_provider character varying(20),
    is_onboard boolean DEFAULT false NOT NULL,
    birth_date date DEFAULT '1900-01-01'::date NOT NULL,
    signup_question character varying(255) DEFAULT '질문이 설정되지 않았습니다.'::character varying NOT NULL,
    signup_answer character varying(30) DEFAULT '답변이 설정되지 않았습니다.'::character varying NOT NULL,
    CONSTRAINT app_user_oauth_provider_check CHECK (((oauth_provider)::text = ANY ((ARRAY['GOOGLE'::character varying, 'FACEBOOK'::character varying, 'KAKAO'::character varying, 'NAVER'::character varying])::text[]))),
    CONSTRAINT chk_app_user_role CHECK (((role)::text = ANY (ARRAY[('ROLE_USER'::character varying)::text, ('ROLE_ADMIN'::character varying)::text]))),
    CONSTRAINT chk_app_user_status CHECK (((status)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('SUSPENDED'::character varying)::text])))
);
```

**fcm_tokens**
```sql
CREATE TABLE user_schema.fcm_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    token text NOT NULL,
    platform character varying(20) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);
```

**interest_topics**
```sql
CREATE TABLE user_schema.interest_topics (
    topic_key character varying(50) NOT NULL,
    label character varying(100) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);
```

**posts_cache**
```sql
CREATE TABLE user_schema.posts_cache (
    external_id character varying(100) NOT NULL,
    title text,
    image_url text,
    description text,
    permalink text,
    published_at timestamp without time zone,
    fetched_at timestamp without time zone DEFAULT now() NOT NULL
);
```

**profiles**
```sql
CREATE TABLE user_schema.profiles (
    id bigint NOT NULL,
    avatar_url character varying(500),
    bio text,
    created_at timestamp(6) without time zone NOT NULL,
    settings jsonb,
    updated_at timestamp(6) without time zone NOT NULL,
    user_id uuid NOT NULL
);
```

**prompt_actives**
```sql
CREATE TABLE user_schema.prompt_actives (
    env character varying(20) DEFAULT 'prod'::character varying NOT NULL,
    agent_type character varying(50) NOT NULL,
    concept character varying(20) NOT NULL,
    intimacy_level integer NOT NULL,
    prompt_version_id bigint NOT NULL,
    activated_by uuid NOT NULL,
    activated_at timestamp without time zone DEFAULT now(),
    CONSTRAINT chk_prompt_active_intimacy_level CHECK ((intimacy_level = ANY (ARRAY[1, 3])))
);
```

**prompt_versions**
```sql
CREATE TABLE user_schema.prompt_versions (
    id bigint NOT NULL,
    agent_type character varying(50) NOT NULL,
    concept character varying(20) NOT NULL,
    intimacy_level integer NOT NULL,
    version character varying(20) NOT NULL,
    content text NOT NULL,
    file_path character varying(500) NOT NULL,
    memo character varying(500),
    parent_version_id bigint,
    created_by uuid NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    CONSTRAINT chk_prompt_intimacy_level CHECK ((intimacy_level = ANY (ARRAY[1, 3])))
);
```

**push_delivery_logs**
```sql
CREATE TABLE user_schema.push_delivery_logs (
    id bigint NOT NULL,
    user_id uuid NOT NULL,
    chatroom_id uuid NOT NULL,
    sent_date date NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);
```

**review_ticket_items**
```sql
CREATE TABLE user_schema.review_ticket_items (
    id bigint NOT NULL,
    ticket_id bigint NOT NULL,
    message_id uuid,
    agent_type character varying(50) NOT NULL,
    snapshot_json jsonb,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);
```

**review_tickets**
```sql
CREATE TABLE user_schema.review_tickets (
    id bigint NOT NULL,
    conversation_id uuid,
    status character varying(20) DEFAULT 'OPEN'::character varying NOT NULL,
    agent_type character varying(50),
    note text,
    created_by uuid,
    assignee uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    done_at timestamp without time zone
);
```

**settings**
```sql
CREATE TABLE user_schema.settings (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    setting_key character varying(100) NOT NULL,
    setting_value text,
    updated_at timestamp(6) without time zone NOT NULL,
    user_id uuid NOT NULL
);
```

**support_requests**
```sql
CREATE TABLE user_schema.support_requests (
    id bigint NOT NULL,
    user_id uuid NOT NULL,
    requester_email character varying(320) NOT NULL,
    type character varying(20) NOT NULL,
    category character varying(100),
    content text NOT NULL,
    reply_requested boolean DEFAULT false NOT NULL,
    reply_email character varying(320),
    chatroom_id uuid,
    message_id uuid,
    message_content text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    ai_response_snapshot jsonb,
    requester_name character varying(100),
    CONSTRAINT chk_support_type CHECK (((type)::text = ANY ((ARRAY['INQUIRY'::character varying, 'REPORT'::character varying])::text[])))
);
```

**user_interest_topics**
```sql
CREATE TABLE user_schema.user_interest_topics (
    user_id uuid NOT NULL,
    topic_key character varying(50) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);
```

**user_notification_settings**
```sql
CREATE TABLE user_schema.user_notification_settings (
    user_id uuid NOT NULL,
    push_enabled boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);
```

**user_stats**
```sql
CREATE TABLE user_schema.user_stats (
    user_id uuid NOT NULL,
    streak_count integer DEFAULT 0 NOT NULL,
    last_active_date date,
    perfect_count integer DEFAULT 0 NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);
```
