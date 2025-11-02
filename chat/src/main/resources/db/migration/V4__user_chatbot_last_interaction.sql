-- user-chatbot 마지막 상호작용 시간 영속 테이블
CREATE TABLE IF NOT EXISTS chat_schema.user_chatbot_last_interaction (
    user_id UUID NOT NULL,
    chatbot_id UUID NOT NULL,
    last_interaction_at TIMESTAMPTZ NULL,
    last_room_id UUID NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT user_chatbot_last_interaction_pk PRIMARY KEY (user_id, chatbot_id)
);

-- 조회 최적화 인덱스
CREATE INDEX IF NOT EXISTS idx_ucli_user_last_ts
    ON chat_schema.user_chatbot_last_interaction (user_id, last_interaction_at DESC);

-- updated_at 트리거 (선택: 없으면 애플리케이션에서 갱신)
CREATE OR REPLACE FUNCTION chat_schema.fn_touch_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_touch_updated_at_ucli ON chat_schema.user_chatbot_last_interaction;
CREATE TRIGGER trg_touch_updated_at_ucli
BEFORE UPDATE ON chat_schema.user_chatbot_last_interaction
FOR EACH ROW EXECUTE FUNCTION chat_schema.fn_touch_updated_at();




