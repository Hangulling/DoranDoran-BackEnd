# 대화 ID 5ef9144c-eb30-44a6-82d6-2a643312fc07 미노출 조사 가이드

## 0. 서버 직접 조사 결과 (결론)

**원인: 해당 채팅방이 삭제 처리(`is_deleted = true`)되어 있어 목록에서 제외됨.**

| 항목 | 결과 |
|------|------|
| **chat_schema.chatrooms** | 1건 존재. `is_deleted = true`, `last_message_at = NULL`, `created_at = 2026-02-18 07:00:13` |
| **user_id** | `3fa11b2f-0e4d-4332-aa76-308407ec68ca` |
| **chatbot_id** | `22222222-2222-2222-2222-222222222223` |
| **archive_schema.arch_chatrooms** | 0건 (아카이브되지 않음) |
| **chat_schema.messages** | 0건 (메시지 없음) |

관리자 목록 API는 `is_deleted = false`인 채팅방만 조회하므로, 이 대화는 '사용자 채팅 내역'에 나타나지 않는 것이 정상 동작입니다.

---

## 1. koach-admin '사용자 채팅 내역' 동작 요약

- **화면**: 사용자 채팅 내역 (ChatLogsUserHistoryPage)
- **API**: User 서비스 `GET /api/admin/conversations` → Chat 서비스 `GET /api/admin/conversations`
- **데이터 소스**:
  - **chat**: `chat_schema.chatrooms` (실시간), **목록에 나오는 conversationId = chatrooms.id**
  - **archive**: `archive_schema.arch_chatrooms` (아카이브), **목록에 나오는 conversationId = arch_chatrooms.id** (원본 채팅방 ID가 아님)

### 목록에 안 나올 수 있는 이유

| 원인 | chat 데이터소스 | archive 데이터소스 |
|------|-----------------|---------------------|
| 삭제됨 | `chatrooms.is_deleted = true` | `arch_chatrooms.is_deleted = true` |
| 레코드 없음 | 해당 id가 chatrooms에 없음 | 해당 id가 arch_chatrooms에 없음 또는 아직 아카이브 미적용 |
| ID 혼동 | - | **아카이브 목록의 conversationId는 arch_chatrooms.id(새 UUID)**. 원본 채팅방 ID는 `source_chatroom_id`에만 있음. 따라서 "원본 대화 ID"로 검색하면 archive 목록에서는 다른 UUID로 보일 수 있음 |
| 필터 | userEmail → userId 변환 후 해당 유저만 조회, from/to/roomKey/intimacyLevel | 동일 필터 적용 |
| 페이징 | 20개씩 페이지; 다른 페이지에 있을 수 있음 | 동일 |

---

## 2. 서버에서 확인할 SQL (EC2 SSH 후 Chat DB에 접속)

대화 ID: `5ef9144c-eb30-44a6-82d6-2a643312fc07`

### 2-1. chat_schema (실시간 채팅방)

```sql
-- 해당 ID가 채팅방에 있는지, 삭제 여부
SELECT id, user_id, chatbot_id, is_deleted, is_archived, last_message_at, created_at, updated_at
FROM chat_schema.chatrooms
WHERE id = '5ef9144c-eb30-44a6-82d6-2a643312fc07';
```

- **결과 0건**: 이 ID는 실시간 채팅방이 아니거나, 이미 삭제되어 테이블에서 제거된 경우.
- **결과 1건**:
  - `is_deleted = true` → **chat 데이터소스 목록에 안 나옴** (코드에서 `is_deleted = false`만 조회).
  - `is_deleted = false`이면 목록에 나와야 함. 이때는 **필터(이메일/기간/roomKey/intimacy)** 또는 **페이징** 때문에 화면에서 안 보일 수 있음.

### 2-2. archive_schema (아카이브)

```sql
-- (A) conversationId가 arch_chatrooms.id인 경우 (아카이브 목록에서 쓰는 ID)
SELECT id, source_chatroom_id, user_id, user_email_snapshot, concept, last_message_at, is_deleted, source_created_at
FROM archive_schema.arch_chatrooms
WHERE id = '5ef9144c-eb30-44a6-82d6-2a643312fc07';

-- (B) conversationId가 원본 채팅방 ID(source_chatroom_id)인 경우
SELECT id, source_chatroom_id, user_id, user_email_snapshot, concept, last_message_at, is_deleted, source_created_at
FROM archive_schema.arch_chatrooms
WHERE source_chatroom_id = '5ef9144c-eb30-44a6-82d6-2a643312fc07';
```

- **(A) 0건, (B) 1건**: 사용 중인 ID가 **원본 채팅방 ID**라면, 아카이브 목록에는 **다른 UUID(arch_chatrooms.id)** 로 표시되므로, "사용자 채팅 내역"에서 **그 원본 ID로는 검색/표시되지 않음**. 대신 (B) 결과의 `id`가 목록에 나오는 conversationId.
- **(A) 1건이고 is_deleted = true**: archive 데이터소스 목록에 안 나옴.
- **(B) 0건**: 해당 채팅방은 아직 아카이브되지 않았을 수 있음.

### 2-3. 메시지 존재 여부 (실시간)

```sql
SELECT COUNT(*), MIN(created_at), MAX(created_at)
FROM chat_schema.messages
WHERE chatroom_id = '5ef9144c-eb30-44a6-82d6-2a643312fc07';
```

- 0건이면 메시지가 없거나 채팅방이 아예 다른 시스템에서 온 ID일 수 있음.

### 2-4. 해당 채팅방의 user_id로 이메일 확인 (필터 원인 확인)

```sql
SELECT cr.id, cr.user_id, cr.is_deleted, cr.last_message_at,
       u.email
FROM chat_schema.chatrooms cr
LEFT JOIN user_schema.users u ON u.id = cr.user_id
WHERE cr.id = '5ef9144c-eb30-44a6-82d6-2a643312fc07';
```

- 관리자 화면에서 **이메일로 검색**했다면, 이 조회 결과의 `email`과 일치해야 "chat" 목록에 노출됨.

---

## 3. SSH 접속 및 실행 예시

```powershell
# 1) 이미지 전송 (사용자가 제시한 명령)
scp -i "$env:USERPROFILE\Downloads\dorandoran-key.pem" dist/dorandoran-auth-latest.tar ec2-user@3.21.177.186:/home/ec2-user/

# 2) SSH 접속
ssh -i "$env:USERPROFILE\Downloads\dorandoran-key.pem" ec2-user@3.21.177.186

# 3) 서버에서 Chat DB가 PostgreSQL이라면 (호스트/DB명/유저는 환경에 맞게 변경)
psql -h <CHAT_DB_HOST> -U <DB_USER> -d <CHAT_DB_NAME> -c "
SELECT id, user_id, is_deleted, last_message_at
FROM chat_schema.chatrooms
WHERE id = '5ef9144c-eb30-44a6-82d6-2a643312fc07';
"

# 4) 아카이브 확인
psql -h <CHAT_DB_HOST> -U <DB_USER> -d <CHAT_DB_NAME> -c "
SELECT id, source_chatroom_id, is_deleted
FROM archive_schema.arch_chatrooms
WHERE id = '5ef9144c-eb30-44a6-82d6-2a643312fc07'
   OR source_chatroom_id = '5ef9144c-eb30-44a6-82d6-2a643312fc07';
"
```

---

## 4. 결론 체크리스트

- [ ] `chat_schema.chatrooms`에 해당 id가 있는가? → 없으면 chat 목록에 없음.
- [ ] 있으면 `is_deleted`가 true인가? → true면 chat 목록에서 제외됨.
- [ ] 사용 중인 ID가 **원본 채팅방 ID**인가? → archive 목록에서는 **arch_chatrooms.id**만 conversationId로 쓰이므로, 원본 ID로는 archive 목록에 안 나올 수 있음.
- [ ] `archive_schema.arch_chatrooms`에 `source_chatroom_id = 해당 id` 행이 있는가? → 없으면 아직 아카이브 안 됨.
- [ ] 관리자가 이메일/기간 등 필터를 걸었는가? → 해당 대화가 조건에 맞아야 목록에 보임.
- [ ] 20개씩 페이징이므로, 다른 페이지에 있어서 안 보일 수 있음.

이 문서와 위 SQL로 서버에서 한 번씩 실행해 보시면, "왜 조회가 되지 않는지" 원인을 좁힐 수 있습니다.
