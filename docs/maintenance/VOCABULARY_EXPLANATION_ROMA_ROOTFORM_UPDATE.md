# VOCABULARY_EXPLANATION 프롬프트 업데이트: roma를 rootForm 기준으로

## 변경 내용
- **목적**: `context.roma` 로마자 표기가 동사원형(rootForm) 기준으로 생성되도록 명시
- **기존**: "정확한 로마자 표기"
- **변경**: "동사원형(rootForm)을 기준으로 로마자 표기. 원본 표현(originalExpression)이 아닌 rootForm의 발음을 표기할 것"

## 1. 파일 기반 프롬프트 (이미 반영됨)
`chat/src/main/resources/prompts/vocabulary/explanation/` 아래 10개 파일이 업데이트되었습니다.
- DB에 해당 프롬프트가 없으면 **파일 fallback**으로 이 내용이 사용됩니다.

## 2. DB 프롬프트 업데이트

DB에서 VOCABULARY_EXPLANATION을 사용 중이면 아래 SQL로 업데이트해야 합니다.

### 적용할 SQL

```sql
-- VOCABULARY_EXPLANATION 프롬프트의 roma 지침 업데이트
UPDATE user_schema.prompt_versions
SET content = REPLACE(
    content,
    '정확한 로마자 표기',
    '동사원형(rootForm)을 기준으로 로마자 표기. 원본 표현(originalExpression)이 아닌 rootForm의 발음을 표기할 것'
)
WHERE agent_type = 'VOCABULARY_EXPLANATION'
  AND content LIKE '%정확한 로마자 표기%';
```

### DB에 VOCABULARY_EXPLANATION이 있는지 확인

```sql
SELECT id, agent_type, concept, intimacy_level, version,
       LEFT(content, 200) as content_preview
FROM user_schema.prompt_versions
WHERE agent_type = 'VOCABULARY_EXPLANATION'
ORDER BY concept, intimacy_level;
```

### SSH 접속 후 실행 예시

```powershell
# SSH 접속
ssh -i "$env:USERPROFILE\Downloads\dorandoran-key.pem" ec2-user@3.21.177.186

# Docker 사용 시 (실제 컨테이너명/이미지는 환경에 맞게 수정)
docker exec -i <postgres_container_name> psql -U doran -d dorandoran -c "
UPDATE user_schema.prompt_versions
SET content = REPLACE(
    content,
    '정확한 로마자 표기',
    '동사원형(rootForm)을 기준으로 로마자 표기. 원본 표현(originalExpression)이 아닌 rootForm의 발음을 표기할 것'
)
WHERE agent_type = 'VOCABULARY_EXPLANATION'
  AND content LIKE '%정확한 로마자 표기%';
"

# 또는 psql 직접 연결 시
# psql -h <db_host> -U doran -d dorandoran -f update_roma_rootform.sql
```

### 참고: 프롬프트 로딩 순서
1. **DB 우선**: `prompt_actives` → `prompt_versions` 조회
2. **파일 fallback**: DB에 없으면 `prompts/vocabulary/explanation/{concept}_{level}.txt` 로드

DB에 active로 등록된 VOCABULARY_EXPLANATION이 있으면 DB 내용이 사용되므로, 해당하는 경우 위 SQL 업데이트가 필요합니다.
