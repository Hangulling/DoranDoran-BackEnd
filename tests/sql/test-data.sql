-- 테스트 데이터 삽입
INSERT INTO user_schema.app_user (id, username, email, password_hash, nickname, picture, status, created_at, updated_at) 
VALUES 
    ('3fa85f64-5717-4562-b3fc-2c963f66afa6', 'testuser', 'test@example.com', 'hashedpassword', '테스트유저', 'https://example.com/pic.jpg', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO chat_schema.chatbots (id, name, description, system_prompt, intimacy_level, created_at, updated_at) 
VALUES 
    ('3fa85f64-5717-4562-b3fc-2c963f66afa6', '테스트봇', '테스트용 챗봇', '안녕하세요! 테스트 봇입니다.', 1, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;
