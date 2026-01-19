-- 대화 턴 단위 시퀀스 추가
-- Bot 응답부터 User 응답까지를 하나의 턴으로 묶어 데이터 분석에 유용한 구조로 변경

-- turn_number 필드 추가
ALTER TABLE chat_schema.messages 
ADD COLUMN turn_number BIGINT NOT NULL DEFAULT 0;

-- 컬럼 설명 추가
COMMENT ON COLUMN chat_schema.messages.turn_number IS '대화 턴 번호 (Bot 응답부터 User 응답까지 하나의 턴, 미완료 턴은 0)';

-- 턴 단위 조회를 위한 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_messages_room_turn
  ON chat_schema.messages (chatroom_id, turn_number);

-- 기존 데이터 처리: 기존 메시지는 turn_number를 0으로 유지 (미완료 턴으로 표시)
-- 향후 필요시 별도 마이그레이션 스크립트로 턴 번호 재계산 가능










