-- 채팅방 제약/인덱스
CREATE UNIQUE INDEX IF NOT EXISTS uq_chatrooms_user_bot_active
  ON chat_schema.chatrooms (user_id, chatbot_id)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_chatrooms_user_updated
  ON chat_schema.chatrooms (user_id, updated_at DESC);

