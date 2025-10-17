-- DoranDoran 데이터베이스에 새 사용자 생성 스크립트
-- AWS RDS PostgreSQL 연결 후 실행

-- 1. 새 사용자 생성 (UUID는 자동 생성)
INSERT INTO user_schema.app_user (
    email,
    first_name,
    last_name,
    name,
    password_hash,
    picture,
    info,
    status,
    role,
    coach_check,
    created_at,
    updated_at,
    last_conn_time
) VALUES (
    'newuser@example.com',                    -- 이메일 (변경 필요)
    '홍',                                     -- 이름 (변경 필요)
    '길동',                                   -- 성 (변경 필요)
    '홍길동',                                 -- 전체 이름 (변경 필요)
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi',  -- 비밀번호 해시 (변경 필요)
    'https://example.com/profile.jpg',        -- 프로필 사진 URL (선택사항)
    '새로운 사용자입니다',                     -- 사용자 정보
    'ACTIVE',                                 -- 상태
    'ROLE_USER',                              -- 역할
    false,                                    -- 코치 체크
    NOW(),                                    -- 생성 시간
    NOW(),                                    -- 수정 시간
    NOW()                                     -- 마지막 연결 시간
);

-- 2. 생성된 사용자 확인
SELECT 
    id,
    email,
    name,
    first_name,
    last_name,
    status,
    role,
    created_at
FROM user_schema.app_user 
WHERE email = 'newuser@example.com';

-- 3. 사용자 프로필 테이블에도 기본 레코드 생성 (선택사항)
INSERT INTO user_schema.profiles (
    user_id,
    bio,
    avatar_url,
    settings,
    created_at,
    updated_at
) 
SELECT 
    id,
    '안녕하세요! 새로운 사용자입니다.',
    'https://example.com/avatar.jpg',
    '{}'::jsonb,
    NOW(),
    NOW()
FROM user_schema.app_user 
WHERE email = 'newuser@example.com'
ON CONFLICT (user_id) DO NOTHING;

-- 4. 사용자 설정 테이블에도 기본 레코드 생성 (선택사항)
INSERT INTO user_schema.settings (
    user_id,
    setting_key,
    setting_value,
    created_at,
    updated_at
) VALUES 
(
    (SELECT id FROM user_schema.app_user WHERE email = 'newuser@example.com'),
    'theme',
    'light',
    NOW(),
    NOW()
),
(
    (SELECT id FROM user_schema.app_user WHERE email = 'newuser@example.com'),
    'language',
    'ko',
    NOW(),
    NOW()
)
ON CONFLICT (user_id, setting_key) DO NOTHING;

-- 5. 최종 확인
SELECT 
    u.id,
    u.email,
    u.name,
    u.status,
    u.role,
    p.bio,
    COUNT(s.setting_key) as settings_count
FROM user_schema.app_user u
LEFT JOIN user_schema.profiles p ON u.id = p.user_id
LEFT JOIN user_schema.settings s ON u.id = s.user_id
WHERE u.email = 'newuser@example.com'
GROUP BY u.id, u.email, u.name, u.status, u.role, p.bio;
