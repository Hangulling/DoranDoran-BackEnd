-- 관심 주제 마스터 초기 데이터 (travel, food, entertainment, daily, f&b, sports)
-- f&b = Fashion and Beauty (패션·뷰티)

INSERT INTO user_schema.interest_topics (topic_key, label, is_active)
VALUES
    ('travel', '여행', true),
    ('food', '음식', true),
    ('entertainment', '엔터테인먼트', true),
    ('daily', '일상', true),
    ('f&b', '패션·뷰티', true),
    ('sports', '스포츠', true)
ON CONFLICT (topic_key) DO UPDATE
SET label = EXCLUDED.label,
    is_active = EXCLUDED.is_active;
