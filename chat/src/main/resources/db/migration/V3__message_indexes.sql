-- 메시지 인덱스 최적화
CREATE INDEX IF NOT EXISTS idx_messages_room_sequence
  ON chat_schema.messages (chatroom_id, sequence_number);

CREATE INDEX IF NOT EXISTS idx_messages_room_created
  ON chat_schema.messages (chatroom_id, created_at DESC);

