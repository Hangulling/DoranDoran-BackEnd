-- 관심 주제 데이터 직접 삽입
-- 서버 DB에 직접 실행하세요

INSERT INTO user_schema.interest_topics (topic_key, label, is_active)
VALUES 
    ('entertainment', '엔터테인먼트', true),
    ('daily', '일상', true),
    ('f&b', '패션·뷰티', true),
    ('sports', '스포츠', true)
ON CONFLICT (topic_key) DO UPDATE
SET label = EXCLUDED.label,
    is_active = EXCLUDED.is_active;
