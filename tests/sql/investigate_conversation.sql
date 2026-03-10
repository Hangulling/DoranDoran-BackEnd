-- Conversation ID: 5ef9144c-eb30-44a6-82d6-2a643312fc07
\echo '=== chat_schema.chatrooms ==='
SELECT id, user_id, chatbot_id, is_deleted, is_archived, last_message_at, created_at
FROM chat_schema.chatrooms
WHERE id = '5ef9144c-eb30-44a6-82d6-2a643312fc07';

\echo '=== archive_schema.arch_chatrooms (by id or source_chatroom_id) ==='
SELECT id, source_chatroom_id, user_id, user_email_snapshot, concept, is_deleted, last_message_at
FROM archive_schema.arch_chatrooms
WHERE id = '5ef9144c-eb30-44a6-82d6-2a643312fc07'
   OR source_chatroom_id = '5ef9144c-eb30-44a6-82d6-2a643312fc07';

\echo '=== chat_schema.messages count ==='
SELECT COUNT(*) AS msg_count, MIN(created_at) AS first_msg, MAX(created_at) AS last_msg
FROM chat_schema.messages
WHERE chatroom_id = '5ef9144c-eb30-44a6-82d6-2a643312fc07';

\echo '=== chatroom only (user_schema may be in different DB) ==='
SELECT cr.id, cr.user_id, cr.is_deleted, cr.last_message_at, cr.created_at
FROM chat_schema.chatrooms cr
WHERE cr.id = '5ef9144c-eb30-44a6-82d6-2a643312fc07';
