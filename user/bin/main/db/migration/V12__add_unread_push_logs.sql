-- 안읽음 푸시 추적 테이블 (키톡 스타일)
-- 푸시 알림을 지우면 프론트에서 스토리지 접근 불가 → 백엔드에서 안읽음 추적

CREATE TABLE IF NOT EXISTS user_schema.unread_push_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    push_type VARCHAR(20) NOT NULL,
    chatbot_id UUID,
    chatroom_id UUID,
    message_id UUID,
    concept VARCHAR(20),
    topic VARCHAR(255),
    start_message TEXT,
    title VARCHAR(255),
    body TEXT,
    sent_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    read_at TIMESTAMPTZ,
    CONSTRAINT chk_unread_push_type CHECK (push_type IN ('CHATROOM_CREATE', 'NEW_MESSAGE'))
);

CREATE INDEX IF NOT EXISTS idx_unread_push_user_read ON user_schema.unread_push_logs(user_id, read_at);
CREATE INDEX IF NOT EXISTS idx_unread_push_user_sent ON user_schema.unread_push_logs(user_id, sent_at DESC);

COMMENT ON TABLE user_schema.unread_push_logs IS '안읽음 푸시 로그 - 푸시 발송 시 insert, 읽음 처리 시 read_at 업데이트';
COMMENT ON COLUMN user_schema.unread_push_logs.push_type IS 'CHATROOM_CREATE: 주제 기반 채팅방 생성 푸시, NEW_MESSAGE: 기존 채팅방 새 메시지 푸시';
COMMENT ON COLUMN user_schema.unread_push_logs.read_at IS 'NULL이면 안읽음, 값이 있으면 읽음';
