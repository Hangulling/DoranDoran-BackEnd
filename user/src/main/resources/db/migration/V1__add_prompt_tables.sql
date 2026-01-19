-- 프롬프트 운영 관리 테이블 추가
-- 서버 DB와 동기화를 위해 prompt_versions, prompt_actives 테이블 생성

-- 프롬프트 버전 관리 테이블
CREATE TABLE IF NOT EXISTS user_schema.prompt_versions (
    id BIGSERIAL PRIMARY KEY,
    agent_type VARCHAR(50) NOT NULL,  -- INTIMACY_ANALYSIS, VOCABULARY_EXTRACTION 등
    concept VARCHAR(20) NOT NULL,     -- friend, coworker, boss, senior, honey
    intimacy_level INTEGER NOT NULL,   -- 1 또는 3
    version VARCHAR(20) NOT NULL,      -- v0.1, v0.2 등
    content TEXT NOT NULL,             -- 프롬프트 내용
    file_path VARCHAR(500) NOT NULL,   -- prompts/intimacy/analysis/friend_1.txt
    memo VARCHAR(500),
    parent_version_id BIGINT REFERENCES user_schema.prompt_versions(id),  -- 이전 버전 추적
    created_by UUID NOT NULL REFERENCES user_schema.app_user(id),
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT uk_prompt_agent_concept_level_version UNIQUE (agent_type, concept, intimacy_level, version),
    CONSTRAINT chk_prompt_intimacy_level CHECK (intimacy_level IN (1, 3))
);

COMMENT ON TABLE user_schema.prompt_versions IS '프롬프트 버전 관리';
COMMENT ON COLUMN user_schema.prompt_versions.agent_type IS '에이전트 타입 (INTIMACY_ANALYSIS, INTIMACY_CORRECTION, VOCABULARY_EXTRACTION, VOCABULARY_EXPLANATION, CONVERSATION, GREETING)';
COMMENT ON COLUMN user_schema.prompt_versions.concept IS '컨셉 (friend, coworker, boss, senior, honey)';
COMMENT ON COLUMN user_schema.prompt_versions.intimacy_level IS '친밀도 레벨 (1 또는 3)';
COMMENT ON COLUMN user_schema.prompt_versions.version IS '버전 번호 (v0.1, v0.2 등)';
COMMENT ON COLUMN user_schema.prompt_versions.content IS '프롬프트 내용';
COMMENT ON COLUMN user_schema.prompt_versions.file_path IS '파일 경로 (prompts/intimacy/analysis/friend_1.txt)';
COMMENT ON COLUMN user_schema.prompt_versions.memo IS '메모';
COMMENT ON COLUMN user_schema.prompt_versions.parent_version_id IS '이전 버전 ID (버전 간 관계 추적)';
COMMENT ON COLUMN user_schema.prompt_versions.created_by IS '생성자';
COMMENT ON COLUMN user_schema.prompt_versions.created_at IS '생성 시간';

CREATE INDEX IF NOT EXISTS idx_prompt_agent_concept_level ON user_schema.prompt_versions (agent_type, concept, intimacy_level, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_prompt_parent_version ON user_schema.prompt_versions(parent_version_id);

-- Active 프롬프트 관리 테이블
CREATE TABLE IF NOT EXISTS user_schema.prompt_actives (
    env VARCHAR(20) NOT NULL DEFAULT 'prod',  -- prod, stage
    agent_type VARCHAR(50) NOT NULL,
    concept VARCHAR(20) NOT NULL,
    intimacy_level INTEGER NOT NULL,
    prompt_version_id BIGINT NOT NULL REFERENCES user_schema.prompt_versions(id),
    activated_by UUID NOT NULL REFERENCES user_schema.app_user(id),
    activated_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (env, agent_type, concept, intimacy_level),
    CONSTRAINT chk_prompt_active_intimacy_level CHECK (intimacy_level IN (1, 3))
);

COMMENT ON TABLE user_schema.prompt_actives IS 'Active 프롬프트 관리';
COMMENT ON COLUMN user_schema.prompt_actives.env IS '환경 (prod, stage)';
COMMENT ON COLUMN user_schema.prompt_actives.agent_type IS '에이전트 타입';
COMMENT ON COLUMN user_schema.prompt_actives.concept IS '컨셉';
COMMENT ON COLUMN user_schema.prompt_actives.intimacy_level IS '친밀도 레벨';
COMMENT ON COLUMN user_schema.prompt_actives.prompt_version_id IS '프롬프트 버전 ID';
COMMENT ON COLUMN user_schema.prompt_actives.activated_by IS '활성화한 사용자';
COMMENT ON COLUMN user_schema.prompt_actives.activated_at IS '활성화 시간';

CREATE INDEX IF NOT EXISTS idx_prompt_active_env_agent ON user_schema.prompt_actives (env, agent_type);
