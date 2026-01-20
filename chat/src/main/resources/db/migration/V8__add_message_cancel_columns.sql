-- 메시지 전송 중지(취소) 컬럼 추가
ALTER TABLE chat_schema.messages
    ADD COLUMN IF NOT EXISTS is_cancelled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP WITHOUT TIME ZONE;
