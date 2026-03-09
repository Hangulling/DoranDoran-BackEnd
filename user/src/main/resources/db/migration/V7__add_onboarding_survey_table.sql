-- 온보딩 설문 저장 테이블 (유입 경로, 한국어 수준, 학습 목적)
-- 통합 온보딩 API(PATCH /api/users/{userId}/onboard + body)에서 사용

CREATE TABLE IF NOT EXISTS user_schema.onboarding_survey (
    user_id UUID NOT NULL PRIMARY KEY,
    referral_source VARCHAR(50) NULL,
    referral_other VARCHAR(80) NULL,
    korean_level SMALLINT NULL,
    purpose_key VARCHAR(50) NULL,
    purpose_other VARCHAR(80) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_onboarding_survey_user
        FOREIGN KEY (user_id)
        REFERENCES user_schema.app_user(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_onboarding_survey_user_id ON user_schema.onboarding_survey(user_id);

COMMENT ON TABLE user_schema.onboarding_survey IS '온보딩 설문 답변 (유입 경로, 한국어 수준, 학습 목적) - 사용자당 1행';
