-- Archive 스키마 SQL 함수
-- JAR 실행 시 자동으로 생성/업데이트됨

-- =========================================================
-- 1. Archive 스키마 확인 및 생성
-- =========================================================
CREATE SCHEMA IF NOT EXISTS archive_schema;

-- =========================================================
-- 2. Archive 진행 상태 업데이트 함수
-- =========================================================
CREATE OR REPLACE FUNCTION archive_schema.update_ingestion_state(
    p_job_name TEXT,
    p_last_chatroom_id UUID,
    p_last_message_id UUID,
    p_last_message_created_at TIMESTAMP,
    p_status TEXT,
    p_note TEXT
) RETURNS VOID AS $$
BEGIN
    INSERT INTO archive_schema.arch_ingestion_state (
        job_name,
        last_source_chatroom_id,
        last_source_message_id,
        last_source_message_created_at,
        status,
        updated_at,
        note
    ) VALUES (
        p_job_name,
        p_last_chatroom_id,
        p_last_message_id,
        p_last_message_created_at,
        p_status,
        NOW(),
        p_note
    )
    ON CONFLICT (job_name) DO UPDATE SET
        last_source_chatroom_id = EXCLUDED.last_source_chatroom_id,
        last_source_message_id = EXCLUDED.last_source_message_id,
        last_source_message_created_at = EXCLUDED.last_source_message_created_at,
        status = EXCLUDED.status,
        updated_at = NOW(),
        note = EXCLUDED.note;
END;
$$ LANGUAGE plpgsql;

-- =========================================================
-- 3. Archive 진행 상태 조회 함수
-- =========================================================
CREATE OR REPLACE FUNCTION archive_schema.get_ingestion_state(p_job_name TEXT)
RETURNS TABLE (
    last_source_chatroom_id UUID,
    last_source_message_id UUID,
    last_source_message_created_at TIMESTAMP,
    status TEXT,
    updated_at TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        ais.last_source_chatroom_id,
        ais.last_source_message_id,
        ais.last_source_message_created_at,
        ais.status,
        ais.updated_at
    FROM archive_schema.arch_ingestion_state ais
    WHERE ais.job_name = p_job_name;
END;
$$ LANGUAGE plpgsql;


