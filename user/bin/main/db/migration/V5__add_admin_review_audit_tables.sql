-- Admin 리뷰/감사 로그 테이블 추가

-- 관리 필요 내역
CREATE TABLE IF NOT EXISTS user_schema.review_tickets (
    id BIGSERIAL PRIMARY KEY,
    conversation_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    agent_type VARCHAR(50),
    note TEXT,
    created_by UUID,
    assignee UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    done_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_review_tickets_status ON user_schema.review_tickets(status);
CREATE INDEX IF NOT EXISTS idx_review_tickets_agent ON user_schema.review_tickets(agent_type);
CREATE INDEX IF NOT EXISTS idx_review_tickets_conversation ON user_schema.review_tickets(conversation_id);

-- 관리 필요 내역 항목
CREATE TABLE IF NOT EXISTS user_schema.review_ticket_items (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    message_id UUID,
    agent_type VARCHAR(50) NOT NULL,
    snapshot_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_review_ticket_items_ticket
        FOREIGN KEY (ticket_id)
        REFERENCES user_schema.review_tickets(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_review_ticket_items_ticket ON user_schema.review_ticket_items(ticket_id);
CREATE INDEX IF NOT EXISTS idx_review_ticket_items_message ON user_schema.review_ticket_items(message_id);
CREATE INDEX IF NOT EXISTS idx_review_ticket_items_agent ON user_schema.review_ticket_items(agent_type);

-- 관리자 감사 로그
CREATE TABLE IF NOT EXISTS user_schema.admin_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    admin_user_id UUID NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    target_type VARCHAR(50),
    target_id BIGINT,
    summary VARCHAR(500),
    before_json JSONB,
    after_json JSONB,
    ip VARCHAR(50),
    user_agent VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_admin ON user_schema.admin_audit_logs(admin_user_id);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_action ON user_schema.admin_audit_logs(action_type);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_created ON user_schema.admin_audit_logs(created_at DESC);
