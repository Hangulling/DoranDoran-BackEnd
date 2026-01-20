# Archive 스키마 스냅샷 컬럼 분석

> **분석일**: 2025-01-04  
> **제안**: 스냅샷 컬럼 제거, arch_messages에 직접 저장  
> **목적**: 중복 제거 및 단순화

---

## 📊 현재 상황

### 현재 스냅샷 컬럼 구조

```sql
-- arch_chatrooms
user_id                 uuid NULL,
user_email_snapshot     varchar(320) NULL,
chatbot_id              uuid NULL,
chatbot_name_snapshot   varchar(100) NULL,
chatbot_type_snapshot   varchar(20) NULL,
chatbot_intimacy_level_snapshot integer NULL,
```

### 제안된 구조

```sql
-- arch_chatrooms (스냅샷 제거)
user_id                 uuid NULL,
chatbot_id              uuid NULL,

-- arch_messages에 직접 저장
sender_id               uuid NULL,
sender_email            varchar(320) NULL,  -- user인 경우
sender_name             varchar(100) NULL,  -- bot인 경우
```

---

## ✅ 제안의 장점

### 1. 중복 제거
- **데이터 중복 감소**
  - arch_chatrooms에 스냅샷이 있으면 모든 메시지가 같은 정보를 참조
  - arch_messages에 직접 저장하면 중복 제거

### 2. 단순화
- **스키마 단순화**
  - arch_chatrooms의 스냅샷 컬럼 제거
  - 구조가 더 단순해짐

### 3. 메시지 레벨 정보
- **메시지별 정보 보존**
  - 각 메시지가 생성된 시점의 정보를 보존
  - 사용자 이메일 변경 시에도 각 메시지의 원본 정보 유지

---

## ⚠️ 제안의 단점 및 반박

### 1. 데이터 중복 증가 (실제로는 더 큰 문제)

**문제**:
- arch_messages에 스냅샷을 저장하면:
  - 같은 채팅방의 모든 메시지가 동일한 user_email, chatbot_name을 중복 저장
  - 예: 100개 메시지가 있으면 user_email이 100번 중복

**현재 구조 (arch_chatrooms에 스냅샷)**:
- user_email_snapshot: 1번만 저장 (채팅방당)
- arch_messages는 arch_chatroom_id로 참조만

**비교**:
```
현재 구조: 1개 채팅방 + 100개 메시지
- user_email_snapshot: 1개 (arch_chatrooms)
- 총 저장: 1개

제안 구조: 1개 채팅방 + 100개 메시지
- sender_email: 100개 (arch_messages, user 메시지만)
- 총 저장: 100개 (중복 증가)
```

**결론**: 제안은 오히려 중복을 증가시킴

### 2. 채팅방 레벨 정보 손실

**문제**:
- 채팅방은 사용자와 챗봇의 관계를 나타냄
- 채팅방 레벨에서 사용자/챗봇 정보가 필요한 경우:
  - "이 사용자의 모든 채팅방 조회"
  - "이 챗봇의 모든 채팅방 조회"
  - 채팅방 목록 조회 시 사용자/챗봇 정보 표시

**현재 구조**:
```sql
-- 채팅방 목록 조회 (JOIN 불필요)
SELECT 
  id, name, 
  user_email_snapshot, 
  chatbot_name_snapshot
FROM arch_chatrooms
WHERE user_id = ?
```

**제안 구조**:
```sql
-- 채팅방 목록 조회 (복잡한 서브쿼리 필요)
SELECT 
  ac.id, ac.name,
  (SELECT sender_email FROM arch_messages 
   WHERE arch_chatroom_id = ac.id AND sender_type = 'user' 
   LIMIT 1) as user_email,
  (SELECT sender_name FROM arch_messages 
   WHERE arch_chatroom_id = ac.id AND sender_type = 'bot' 
   LIMIT 1) as chatbot_name
FROM arch_chatrooms ac
WHERE user_id = ?
```

**결론**: 채팅방 레벨 조회 성능 저하

### 3. Archive 시점의 데이터 보존

**문제**:
- Archive는 "특정 시점의 데이터 스냅샷"을 보존하는 것이 목적
- 채팅방이 archive된 시점의 사용자/챗봇 정보를 보존해야 함
- 메시지별로 다른 정보를 저장하면 archive 시점의 일관성 손실

**예시**:
```
채팅방 생성: 2024-01-01
- user_email: "user@example.com"
- chatbot_name: "가아라"

메시지 1: 2024-01-01 (user_email: "user@example.com")
메시지 2: 2024-01-02 (user_email: "user@example.com")
메시지 3: 2024-01-03 (user_email: "user@example.com")

사용자 이메일 변경: 2024-01-04
- user_email: "newuser@example.com"

Archive: 2024-01-05
```

**현재 구조**:
- arch_chatrooms.user_email_snapshot: "user@example.com" (archive 시점)
- 모든 메시지는 동일한 archive 시점 정보 참조

**제안 구조**:
- 각 메시지마다 다른 이메일 저장 가능
- archive 시점의 일관성 손실

**결론**: Archive 목적에 부적합

### 4. 조회 성능

**현재 구조**:
```sql
-- 채팅방 정보 조회 (인덱스 활용)
SELECT * FROM arch_chatrooms 
WHERE user_id = ? 
ORDER BY source_created_at DESC;
-- user_email_snapshot, chatbot_name_snapshot 바로 사용 가능
```

**제안 구조**:
```sql
-- 채팅방 정보 조회 (서브쿼리 또는 JOIN 필요)
SELECT 
  ac.*,
  (SELECT sender_email FROM arch_messages ...) as user_email,
  (SELECT sender_name FROM arch_messages ...) as chatbot_name
FROM arch_chatrooms ac
WHERE user_id = ?
ORDER BY source_created_at DESC;
-- 서브쿼리로 인한 성능 저하
```

**결론**: 조회 성능 저하

### 5. 데이터 일관성

**문제**:
- 같은 채팅방의 메시지들이 서로 다른 사용자/챗봇 정보를 가질 수 있음
- 데이터 일관성 보장 어려움

**예시**:
```
메시지 1: sender_email = "user@example.com"
메시지 2: sender_email = "user@example.com"  
메시지 3: sender_email = "newuser@example.com"  -- 사용자 이메일 변경 후
```

**현재 구조**:
- arch_chatrooms.user_email_snapshot: archive 시점의 일관된 정보

**결론**: 데이터 일관성 보장 어려움

---

## 💡 최종 권장안

### ✅ **스냅샷 컬럼 유지 권장**

**이유**:
1. **데이터 중복 감소**: 채팅방당 1번만 저장 (메시지당 N번 저장보다 효율적)
2. **조회 성능**: 채팅방 레벨 조회 시 JOIN/서브쿼리 불필요
3. **Archive 목적**: 특정 시점의 데이터 스냅샷 보존
4. **데이터 일관성**: archive 시점의 일관된 정보 보장

### 📋 현재 구조의 장점

1. **정규화**: 채팅방 레벨 정보는 arch_chatrooms에, 메시지 레벨 정보는 arch_messages에
2. **성능**: 채팅방 목록 조회 시 스냅샷 컬럼 바로 사용
3. **일관성**: archive 시점의 일관된 정보 보존
4. **저장 공간**: 중복 최소화

### ⚠️ 제안 구조의 문제점

1. **중복 증가**: 메시지마다 동일한 정보 중복 저장
2. **성능 저하**: 채팅방 레벨 조회 시 서브쿼리 필요
3. **일관성 손실**: archive 시점의 일관된 정보 보장 어려움
4. **복잡도 증가**: 조회 쿼리 복잡도 증가

---

## 📊 비교표

| 항목 | 현재 구조 (스냅샷 유지) | 제안 구조 (스냅샷 제거) |
|------|----------------------|----------------------|
| **데이터 중복** | ✅ 최소 (채팅방당 1번) | ❌ 증가 (메시지당 N번) |
| **조회 성능** | ✅ 인덱스 활용 가능 | ⚠️ 서브쿼리 필요 |
| **Archive 목적** | ✅ 시점별 일관성 보장 | ❌ 일관성 손실 가능 |
| **스키마 단순성** | ⚠️ 스냅샷 컬럼 존재 | ✅ 단순 |
| **데이터 일관성** | ✅ archive 시점 일관 | ❌ 메시지별 다를 수 있음 |
| **저장 공간** | ✅ 효율적 | ❌ 비효율적 |

---

## 🎯 결론

**스냅샷 컬럼은 유지하는 것이 권장됩니다.**

- ✅ 데이터 중복 최소화
- ✅ 조회 성능 최적화
- ✅ Archive 목적 달성 (시점별 일관성)
- ✅ 데이터 일관성 보장

**제안 구조는**:
- ❌ 오히려 중복을 증가시킴
- ❌ 조회 성능 저하
- ❌ Archive 목적에 부적합

**다만**, 메시지 레벨에서 추가 정보가 필요한 경우 (예: 메시지 생성 시점의 사용자 정보가 변경되었을 때), 선택적으로 arch_messages에 추가 정보를 저장할 수 있습니다. 하지만 기본적인 사용자/챗봇 정보는 arch_chatrooms의 스냅샷 컬럼에 유지하는 것이 효율적입니다.


