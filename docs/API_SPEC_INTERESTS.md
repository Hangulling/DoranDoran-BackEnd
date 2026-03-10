# 관심주제 API 명세 (igu 기준)

## 1. 개요
- **서비스**: User Service
- **Gateway Base URL**: `http://localhost:8080`
- **Direct Base URL**: `http://localhost:8082`
- **요청 Content-Type**: `application/json`

관심주제는 DB 마스터 테이블 `user_schema.interest_topics(topic_key, label, is_active)`에 저장된 값만 유효합니다.  
사용자가 보내는 `topicKeys` 중 DB에 존재하지 않는 키가 있으면 오류가 발생합니다.

---

## 2. 엔드포인트

### 2.1 사용자 관심주제 조회
`GET /api/users/{userId}/interests`

#### Path Param
- **userId**: UUID 문자열

#### 성공 응답
`200 OK`

```json
{
  "topics": [
    { "topicKey": "travel", "label": "여행" },
    { "topicKey": "food", "label": "음식" }
  ]
}
```

#### 비고
- 사용자가 관심주제를 아직 저장하지 않았다면 `topics`는 빈 배열로 반환됩니다.
- 저장된 키가 있더라도 해당 마스터 토픽이 `is_active=false`이면 응답에서 필터링될 수 있습니다.

---

### 2.2 사용자 관심주제 저장/수정
`PUT /api/users/{userId}/interests`

#### Path Param
- **userId**: UUID 문자열

#### 요청 바디
```json
{
  "topicKeys": ["travel", "food"]
}
```

#### 처리 규칙(서버 로직)
- `topicKeys`가 `null`이면 전체 관심주제를 비우는 것으로 처리됩니다.
- 각 key는 공백/빈 문자열은 무시되고, 중복은 제거됩니다.
- `interest_topics`에 존재하지 않는 key가 있으면 실패합니다.
  - 오류 메시지 예: `"존재하지 않는 관심 주제입니다: travel"`
- 저장 시 기존 매핑을 삭제 후, 요청값으로 다시 저장합니다(전체 교체 방식).

#### 성공 응답
`200 OK`

```json
{
  "topics": [
    { "topicKey": "travel", "label": "여행" },
    { "topicKey": "food", "label": "음식" }
  ]
}
```

#### 실패 응답(대표 케이스)
`400 Bad Request`

- 존재하지 않는 관심주제 key
  - `errorCode`: `INVALID_REQUEST`
  - `message`: `"존재하지 않는 관심 주제입니다: {key}"`

---

## 3. (참고) 관심주제 key “전체 목록” 조회
`igu` 기준 코드에는 “관심주제 마스터 전체 목록”을 반환하는 전용 API가 별도로 존재하지 않습니다.

필요 시 DB에서 아래 쿼리로 확인할 수 있습니다.

```sql
SELECT topic_key, label
FROM user_schema.interest_topics
WHERE is_active = true
ORDER BY topic_key;
```

