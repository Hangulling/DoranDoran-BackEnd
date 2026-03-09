-- travel, food 관심 주제 추가

INSERT INTO user_schema.interest_topics (topic_key, label, is_active)
VALUES 
    ('travel', '여행', true),
    ('food', '음식', true)
ON CONFLICT (topic_key) DO UPDATE
SET label = EXCLUDED.label,
    is_active = EXCLUDED.is_active;
