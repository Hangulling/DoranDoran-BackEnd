SELECT topic_key, label, is_active 
FROM user_schema.interest_topics 
WHERE topic_key IN ('entertainment', 'daily', 'f&b', 'sports') 
ORDER BY topic_key;
