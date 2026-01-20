-- chat_schema 컬럼 정의 및 인덱스 동기화
-- 서버 DB 기준으로 제약조건 이름 통일 및 인덱스 추가

-- chatbots 테이블 제약조건 이름 통일
DO $$
BEGIN
    -- 기존 제약조건이 있으면 삭제
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chatbots_intimacy_level_check') THEN
        ALTER TABLE chat_schema.chatbots DROP CONSTRAINT chatbots_intimacy_level_check;
    END IF;
END $$;

-- 서버 기준 제약조건 추가 (서버에는 chk_chatbots_type만 있음)
-- intimacy_level_check는 서버에 없으므로 유지하지 않음

-- 서버에만 있는 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_messages_room_turn ON chat_schema.messages USING btree (chatroom_id, turn_number);
CREATE INDEX IF NOT EXISTS idx_messages_created_at ON chat_schema.messages USING btree (created_at);
CREATE INDEX IF NOT EXISTS idx_messages_chatroom ON chat_schema.messages USING btree (chatroom_id, sequence_number);
CREATE INDEX IF NOT EXISTS idx_messages_sender ON chat_schema.messages USING btree (sender_id);
CREATE INDEX IF NOT EXISTS idx_ucli_user_last_ts ON chat_schema.user_chatbot_last_interaction USING btree (user_id, last_interaction_at DESC);
