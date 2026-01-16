-- 회원가입 필드에 기본값 설정
-- birth_date, signup_question, signup_answer에 NOT NULL 및 DEFAULT 값 적용

-- 1. 기존 NULL 값들을 기본값으로 업데이트
UPDATE user_schema.app_user 
SET birth_date = COALESCE(birth_date, '1900-01-01'::DATE)
WHERE birth_date IS NULL;

UPDATE user_schema.app_user 
SET signup_question = COALESCE(signup_question, '질문이 설정되지 않았습니다.')
WHERE signup_question IS NULL;

UPDATE user_schema.app_user 
SET signup_answer = COALESCE(signup_answer, '답변이 설정되지 않았습니다.')
WHERE signup_answer IS NULL;

-- 2. 컬럼에 NOT NULL 제약조건 및 DEFAULT 값 추가
ALTER TABLE user_schema.app_user 
ALTER COLUMN birth_date SET DEFAULT '1900-01-01'::DATE,
ALTER COLUMN birth_date SET NOT NULL;

ALTER TABLE user_schema.app_user 
ALTER COLUMN signup_question SET DEFAULT '질문이 설정되지 않았습니다.',
ALTER COLUMN signup_question SET NOT NULL;

ALTER TABLE user_schema.app_user 
ALTER COLUMN signup_answer SET DEFAULT '답변이 설정되지 않았습니다.',
ALTER COLUMN signup_answer SET NOT NULL;

