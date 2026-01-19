-- Admin 사용자/역할 테이블 추가

CREATE TABLE IF NOT EXISTS user_schema.admin_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_admin_users_is_active ON user_schema.admin_users(is_active);

CREATE TABLE IF NOT EXISTS user_schema.admin_roles (
    id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO user_schema.admin_roles (role_name, description)
VALUES
    ('VIEWER', '조회만 가능한 역할'),
    ('REVIEWER', '관리 필요 내역 처리 역할'),
    ('PROMPT_MANAGER', '프롬프트 편집/적용/배포 역할'),
    ('SUPER_ADMIN', '모든 권한을 가진 관리자 역할')
ON CONFLICT (role_name) DO NOTHING;

CREATE TABLE IF NOT EXISTS user_schema.admin_user_roles (
    admin_user_id BIGINT NOT NULL,
    admin_role_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (admin_user_id, admin_role_id),
    CONSTRAINT fk_admin_user_roles_user
        FOREIGN KEY (admin_user_id)
        REFERENCES user_schema.admin_users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_admin_user_roles_role
        FOREIGN KEY (admin_role_id)
        REFERENCES user_schema.admin_roles(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_admin_user_roles_user ON user_schema.admin_user_roles(admin_user_id);
CREATE INDEX IF NOT EXISTS idx_admin_user_roles_role ON user_schema.admin_user_roles(admin_role_id);
