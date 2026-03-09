SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'chat_schema' AND tablename = 'chatrooms'
ORDER BY indexname;
