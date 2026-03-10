-- VOCABULARY_EXPLANATION: roma를 rootForm 기준으로 생성하도록 지침 추가
-- 실행: psql -h <host> -U <user> -d dorandoran -f update_vocabulary_explanation_roma_rootform.sql

UPDATE user_schema.prompt_versions
SET content = REPLACE(
    content,
    '정확한 로마자 표기',
    '동사원형(rootForm)을 기준으로 로마자 표기. 원본 표현(originalExpression)이 아닌 rootForm의 발음을 표기할 것'
)
WHERE agent_type = 'VOCABULARY_EXPLANATION'
  AND content LIKE '%정확한 로마자 표기%';
