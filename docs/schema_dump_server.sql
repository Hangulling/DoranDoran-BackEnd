--
-- PostgreSQL database dump
--

\restrict fuzQfIMtI2oiVWC0PR7riVsd5Hp2fA71uO8FPQeFMtrO23jVvRcVqyArq2r0Oad

-- Dumped from database version 17.6
-- Dumped by pg_dump version 17.6

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: archive_schema; Type: SCHEMA; Schema: -; Owner: doran
--

CREATE SCHEMA archive_schema;


ALTER SCHEMA archive_schema OWNER TO doran;

--
-- Name: auth_schema; Type: SCHEMA; Schema: -; Owner: doran
--

CREATE SCHEMA auth_schema;


ALTER SCHEMA auth_schema OWNER TO doran;

--
-- Name: batch_schema; Type: SCHEMA; Schema: -; Owner: doran
--

CREATE SCHEMA batch_schema;


ALTER SCHEMA batch_schema OWNER TO doran;

--
-- Name: billing; Type: SCHEMA; Schema: -; Owner: doran
--

CREATE SCHEMA billing;


ALTER SCHEMA billing OWNER TO doran;

--
-- Name: chat_schema; Type: SCHEMA; Schema: -; Owner: doran
--

CREATE SCHEMA chat_schema;


ALTER SCHEMA chat_schema OWNER TO doran;

--
-- Name: store_schema; Type: SCHEMA; Schema: -; Owner: doran
--

CREATE SCHEMA store_schema;


ALTER SCHEMA store_schema OWNER TO doran;

--
-- Name: user_schema; Type: SCHEMA; Schema: -; Owner: doran
--

CREATE SCHEMA user_schema;


ALTER SCHEMA user_schema OWNER TO doran;

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- Name: build_arch_chatroom_meta(jsonb, jsonb, integer, text); Type: FUNCTION; Schema: archive_schema; Owner: doran
--

CREATE FUNCTION archive_schema.build_arch_chatroom_meta(settings_jsonb jsonb, context_data_jsonb jsonb, intimacy_level_value integer, test_model_value text) RETURNS jsonb
    LANGUAGE plpgsql IMMUTABLE
    AS $$
DECLARE
    meta jsonb;
    concept_value text;
BEGIN
    -- concept 추출
    concept_value := archive_schema.extract_concept_from_settings(settings_jsonb);
    
    -- meta 기본 구조 생성
    meta := jsonb_build_object(
        'concept', concept_value,
        'intimacyLevel', intimacy_level_value,
        'source', jsonb_build_object(
            'env', 'prod',
            'service', 'chat'
        )
    );
    
    -- testModel 추가 (있으면)
    IF test_model_value IS NOT NULL THEN
        meta := jsonb_set(meta, '{testModel}', to_jsonb(test_model_value));
    END IF;
    
    -- settings 전체 추가
    IF settings_jsonb IS NOT NULL THEN
        meta := jsonb_set(meta, '{settings}', settings_jsonb);
    END IF;
    
    -- contextData 추가 (있으면)
    IF context_data_jsonb IS NOT NULL THEN
        meta := jsonb_set(meta, '{contextData}', context_data_jsonb);
    END IF;
    
    RETURN meta;
END;
$$;


ALTER FUNCTION archive_schema.build_arch_chatroom_meta(settings_jsonb jsonb, context_data_jsonb jsonb, intimacy_level_value integer, test_model_value text) OWNER TO doran;

--
-- Name: build_arch_message_metadata_json(jsonb, uuid, text); Type: FUNCTION; Schema: archive_schema; Owner: doran
--

CREATE FUNCTION archive_schema.build_arch_message_metadata_json(original_metadata jsonb, store_id_value uuid, usage_request_id_value text) RETURNS jsonb
    LANGUAGE plpgsql IMMUTABLE
    AS $$
DECLARE
    metadata_json jsonb;
    link_obj jsonb;
BEGIN
    -- analysis 기본 구조
    metadata_json := jsonb_build_object(
        'analysis', jsonb_build_object(
            'language', 'ko',
            'safety', jsonb_build_object(
                'flag', false,
                'reason', NULL::jsonb
            )
        )
    );
    
    -- link 객체 생성
    link_obj := jsonb_build_object();
    
    -- storeId 추가 (있으면)
    IF store_id_value IS NOT NULL THEN
        link_obj := jsonb_set(link_obj, '{storeId}', to_jsonb(store_id_value::text));
    END IF;
    
    -- usageRequestId 추가 (있으면)
    IF usage_request_id_value IS NOT NULL THEN
        link_obj := jsonb_set(link_obj, '{usageRequestId}', to_jsonb(usage_request_id_value));
    END IF;
    
    -- link 추가
    IF link_obj != '{}'::jsonb THEN
        metadata_json := jsonb_set(metadata_json, '{link}', link_obj);
    END IF;
    
    -- originalMetadata 추가 (있으면)
    IF original_metadata IS NOT NULL THEN
        metadata_json := jsonb_set(metadata_json, '{originalMetadata}', original_metadata);
    END IF;
    
    RETURN metadata_json;
END;
$$;


ALTER FUNCTION archive_schema.build_arch_message_metadata_json(original_metadata jsonb, store_id_value uuid, usage_request_id_value text) OWNER TO doran;

--
-- Name: extract_concept_from_settings(jsonb); Type: FUNCTION; Schema: archive_schema; Owner: doran
--

CREATE FUNCTION archive_schema.extract_concept_from_settings(settings_jsonb jsonb) RETURNS text
    LANGUAGE plpgsql IMMUTABLE
    AS $$
DECLARE
    concept_value text;
BEGIN
    IF settings_jsonb IS NULL THEN
        RETURN 'FRIEND';
    END IF;
    
    IF settings_jsonb ? 'concept' THEN
        concept_value := UPPER(settings_jsonb->>'concept');
        -- 실제 서비스에서 사용하는 concept 값: FRIEND, HONEY, COWORKER, SENIOR, BOSS
        IF concept_value IN ('FRIEND', 'HONEY', 'COWORKER', 'SENIOR', 'BOSS') THEN
            RETURN concept_value;
        END IF;
    END IF;
    
    RETURN 'FRIEND';
END;
$$;


ALTER FUNCTION archive_schema.extract_concept_from_settings(settings_jsonb jsonb) OWNER TO doran;

--
-- Name: extract_intimacy_agent_result(jsonb); Type: FUNCTION; Schema: archive_schema; Owner: doran
--

CREATE FUNCTION archive_schema.extract_intimacy_agent_result(metadata_jsonb jsonb) RETURNS jsonb
    LANGUAGE plpgsql IMMUTABLE
    AS $$
BEGIN
    IF metadata_jsonb IS NULL THEN
        RETURN NULL;
    END IF;
    
    IF metadata_jsonb ? 'userMessageAnalysis' AND 
       metadata_jsonb->'userMessageAnalysis' ? 'intimacy' THEN
        RETURN metadata_jsonb->'userMessageAnalysis'->'intimacy';
    END IF;
    
    RETURN NULL;
END;
$$;


ALTER FUNCTION archive_schema.extract_intimacy_agent_result(metadata_jsonb jsonb) OWNER TO doran;

--
-- Name: extract_usage_info(jsonb); Type: FUNCTION; Schema: archive_schema; Owner: doran
--

CREATE FUNCTION archive_schema.extract_usage_info(metadata_jsonb jsonb) RETURNS jsonb
    LANGUAGE plpgsql IMMUTABLE
    AS $$
BEGIN
    IF metadata_jsonb IS NULL OR NOT (metadata_jsonb ? 'usage') THEN
        RETURN NULL;
    END IF;
    
    RETURN metadata_jsonb->'usage';
END;
$$;


ALTER FUNCTION archive_schema.extract_usage_info(metadata_jsonb jsonb) OWNER TO doran;

--
-- Name: extract_usage_request_id(jsonb); Type: FUNCTION; Schema: archive_schema; Owner: doran
--

CREATE FUNCTION archive_schema.extract_usage_request_id(metadata_jsonb jsonb) RETURNS text
    LANGUAGE plpgsql IMMUTABLE
    AS $$
BEGIN
    IF metadata_jsonb IS NULL THEN
        RETURN NULL;
    END IF;
    
    IF metadata_jsonb ? 'usage' AND metadata_jsonb->'usage' ? 'requestId' THEN
        RETURN metadata_jsonb->'usage'->>'requestId';
    END IF;
    
    RETURN NULL;
END;
$$;


ALTER FUNCTION archive_schema.extract_usage_request_id(metadata_jsonb jsonb) OWNER TO doran;

--
-- Name: extract_vocabulary_agent_result(jsonb); Type: FUNCTION; Schema: archive_schema; Owner: doran
--

CREATE FUNCTION archive_schema.extract_vocabulary_agent_result(metadata_jsonb jsonb) RETURNS jsonb
    LANGUAGE plpgsql IMMUTABLE
    AS $$
DECLARE
    vocab_node jsonb;
BEGIN
    IF metadata_jsonb IS NULL THEN
        RETURN NULL;
    END IF;
    
    IF metadata_jsonb ? 'botResponseAnalysis' AND 
       metadata_jsonb->'botResponseAnalysis' ? 'vocabulary' THEN
        vocab_node := metadata_jsonb->'botResponseAnalysis'->'vocabulary';
        
        -- words 배열만 추출하여 payload 구성
        IF vocab_node ? 'words' THEN
            RETURN jsonb_build_object('words', vocab_node->'words');
        END IF;
    END IF;
    
    RETURN NULL;
END;
$$;


ALTER FUNCTION archive_schema.extract_vocabulary_agent_result(metadata_jsonb jsonb) OWNER TO doran;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: arch_agent_results; Type: TABLE; Schema: archive_schema; Owner: doran
--

CREATE TABLE archive_schema.arch_agent_results (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    arch_message_id uuid NOT NULL,
    agent_type character varying(20) NOT NULL,
    payload_json jsonb NOT NULL,
    request_id text,
    provider text,
    model text,
    latency_ms integer,
    input_tokens integer,
    output_tokens integer,
    source_created_at timestamp without time zone,
    archived_at timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT arch_agent_type_check CHECK (((agent_type)::text = ANY ((ARRAY['intimacy'::character varying, 'conver'::character varying, 'voca'::character varying])::text[])))
);


ALTER TABLE archive_schema.arch_agent_results OWNER TO doran;

--
-- Name: arch_chatrooms; Type: TABLE; Schema: archive_schema; Owner: doran
--

CREATE TABLE archive_schema.arch_chatrooms (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    source_chatroom_id uuid NOT NULL,
    user_id uuid,
    user_email_snapshot character varying(320),
    chatbot_id uuid,
    chatbot_name_snapshot character varying(100),
    chatbot_type_snapshot character varying(20),
    chatbot_intimacy_level_snapshot integer,
    name character varying(100) NOT NULL,
    description text,
    concept character varying(50),
    last_message_at timestamp without time zone,
    source_last_message_id uuid,
    is_archived boolean DEFAULT false NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    source_created_at timestamp without time zone,
    source_updated_at timestamp without time zone,
    archived_at timestamp without time zone DEFAULT now() NOT NULL,
    source_deleted_at timestamp without time zone,
    meta jsonb DEFAULT '{}'::jsonb NOT NULL
);


ALTER TABLE archive_schema.arch_chatrooms OWNER TO doran;

--
-- Name: arch_ingestion_state; Type: TABLE; Schema: archive_schema; Owner: doran
--

CREATE TABLE archive_schema.arch_ingestion_state (
    id bigint NOT NULL,
    job_name text NOT NULL,
    last_source_chatroom_id uuid,
    last_source_message_id uuid,
    last_source_message_created_at timestamp without time zone,
    status text DEFAULT 'RUNNING'::text NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    note text
);


ALTER TABLE archive_schema.arch_ingestion_state OWNER TO doran;

--
-- Name: arch_ingestion_state_id_seq; Type: SEQUENCE; Schema: archive_schema; Owner: doran
--

ALTER TABLE archive_schema.arch_ingestion_state ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME archive_schema.arch_ingestion_state_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: arch_intimacy_progress; Type: TABLE; Schema: archive_schema; Owner: doran
--

CREATE TABLE archive_schema.arch_intimacy_progress (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    arch_chatroom_id uuid NOT NULL,
    source_intimacy_progress_id uuid NOT NULL,
    user_id uuid NOT NULL,
    intimacy_level integer DEFAULT 1 NOT NULL,
    total_corrections integer DEFAULT 0,
    last_feedback text,
    last_updated timestamp without time zone,
    progress_data jsonb,
    archived_at timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT arch_intimacy_level_check CHECK ((intimacy_level = ANY (ARRAY[1, 2, 3])))
);


ALTER TABLE archive_schema.arch_intimacy_progress OWNER TO doran;

--
-- Name: arch_messages; Type: TABLE; Schema: archive_schema; Owner: doran
--

CREATE TABLE archive_schema.arch_messages (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    arch_chatroom_id uuid NOT NULL,
    source_message_id uuid NOT NULL,
    source_parent_message_id uuid,
    sender_type character varying(20) NOT NULL,
    sender_id uuid,
    content text NOT NULL,
    content_type character varying(20) DEFAULT 'text'::character varying NOT NULL,
    sequence_number bigint NOT NULL,
    turn_number bigint DEFAULT 0 NOT NULL,
    token_count integer,
    processing_time_ms integer,
    is_edited boolean DEFAULT false NOT NULL,
    edited_at timestamp without time zone,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone,
    source_created_at timestamp without time zone,
    source_updated_at timestamp without time zone,
    archived_at timestamp without time zone DEFAULT now() NOT NULL,
    metadata_json jsonb DEFAULT '{}'::jsonb NOT NULL,
    CONSTRAINT arch_messages_content_type_check CHECK (((content_type)::text = ANY ((ARRAY['text'::character varying, 'code'::character varying, 'system'::character varying, 'json'::character varying])::text[]))),
    CONSTRAINT arch_messages_sender_type_check CHECK (((sender_type)::text = ANY ((ARRAY['user'::character varying, 'bot'::character varying, 'system'::character varying])::text[])))
);


ALTER TABLE archive_schema.arch_messages OWNER TO doran;

--
-- Name: arch_stores; Type: TABLE; Schema: archive_schema; Owner: doran
--

CREATE TABLE archive_schema.arch_stores (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    arch_chatroom_id uuid NOT NULL,
    source_store_id uuid NOT NULL,
    source_message_id uuid NOT NULL,
    user_id uuid NOT NULL,
    content text NOT NULL,
    corrected_content text,
    ai_response jsonb NOT NULL,
    bot_type character varying(20) NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone,
    source_created_at timestamp without time zone,
    source_updated_at timestamp without time zone,
    archived_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE archive_schema.arch_stores OWNER TO doran;

--
-- Name: arch_usage_events; Type: TABLE; Schema: archive_schema; Owner: doran
--

CREATE TABLE archive_schema.arch_usage_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    arch_chatroom_id uuid NOT NULL,
    source_usage_event_id uuid NOT NULL,
    user_id uuid NOT NULL,
    event_time timestamp without time zone NOT NULL,
    provider text NOT NULL,
    model text NOT NULL,
    request_id text,
    input_tokens integer DEFAULT 0 NOT NULL,
    output_tokens integer DEFAULT 0 NOT NULL,
    cost_in numeric(18,6) DEFAULT 0 NOT NULL,
    cost_out numeric(18,6) DEFAULT 0 NOT NULL,
    meta jsonb,
    archived_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE archive_schema.arch_usage_events OWNER TO doran;

--
-- Name: management_queue; Type: TABLE; Schema: archive_schema; Owner: doran
--

CREATE TABLE archive_schema.management_queue (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    queue_type character varying(50) NOT NULL,
    status character varying(20) NOT NULL,
    request_data jsonb NOT NULL,
    result_data jsonb,
    admin_name character varying(100),
    admin_ip character varying(45),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone,
    completed_at timestamp without time zone,
    error_message text
);


ALTER TABLE archive_schema.management_queue OWNER TO doran;

--
-- Name: auth_events; Type: TABLE; Schema: auth_schema; Owner: doran
--

CREATE TABLE auth_schema.auth_events (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    event_type character varying(50) NOT NULL,
    metadata text,
    user_id uuid
);


ALTER TABLE auth_schema.auth_events OWNER TO doran;

--
-- Name: auth_events_id_seq; Type: SEQUENCE; Schema: auth_schema; Owner: doran
--

ALTER TABLE auth_schema.auth_events ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME auth_schema.auth_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: email_verifications; Type: TABLE; Schema: auth_schema; Owner: doran
--

CREATE TABLE auth_schema.email_verifications (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    token_hash character varying(128) NOT NULL,
    verified boolean NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE auth_schema.email_verifications OWNER TO doran;

--
-- Name: email_verifications_id_seq; Type: SEQUENCE; Schema: auth_schema; Owner: doran
--

ALTER TABLE auth_schema.email_verifications ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME auth_schema.email_verifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: login_attempts; Type: TABLE; Schema: auth_schema; Owner: doran
--

CREATE TABLE auth_schema.login_attempts (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    email character varying(320),
    ip_address character varying(255),
    succeeded boolean NOT NULL,
    user_agent character varying(500),
    user_id uuid
);


ALTER TABLE auth_schema.login_attempts OWNER TO doran;

--
-- Name: login_attempts_id_seq; Type: SEQUENCE; Schema: auth_schema; Owner: doran
--

ALTER TABLE auth_schema.login_attempts ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME auth_schema.login_attempts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: password_reset_tokens; Type: TABLE; Schema: auth_schema; Owner: doran
--

CREATE TABLE auth_schema.password_reset_tokens (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    token_hash character varying(128) NOT NULL,
    used boolean NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE auth_schema.password_reset_tokens OWNER TO doran;

--
-- Name: password_reset_tokens_id_seq; Type: SEQUENCE; Schema: auth_schema; Owner: doran
--

ALTER TABLE auth_schema.password_reset_tokens ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME auth_schema.password_reset_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: refresh_tokens; Type: TABLE; Schema: auth_schema; Owner: doran
--

CREATE TABLE auth_schema.refresh_tokens (
    id bigint NOT NULL,
    device_id character varying(200),
    expires_at timestamp(6) without time zone NOT NULL,
    ip_address character varying(255),
    issued_at timestamp(6) without time zone NOT NULL,
    revoked boolean NOT NULL,
    rotated_from_id bigint,
    token text NOT NULL,
    user_agent character varying(500),
    user_id uuid NOT NULL
);


ALTER TABLE auth_schema.refresh_tokens OWNER TO doran;

--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE; Schema: auth_schema; Owner: doran
--

ALTER TABLE auth_schema.refresh_tokens ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME auth_schema.refresh_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: token_blacklist; Type: TABLE; Schema: auth_schema; Owner: doran
--

CREATE TABLE auth_schema.token_blacklist (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    reason character varying(200),
    token_hash character varying(128) NOT NULL,
    token_type character varying(20) NOT NULL
);


ALTER TABLE auth_schema.token_blacklist OWNER TO doran;

--
-- Name: token_blacklist_id_seq; Type: SEQUENCE; Schema: auth_schema; Owner: doran
--

ALTER TABLE auth_schema.token_blacklist ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME auth_schema.token_blacklist_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: ai_usage_events; Type: TABLE; Schema: billing; Owner: doran
--

CREATE TABLE billing.ai_usage_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    event_time timestamp without time zone DEFAULT now() NOT NULL,
    user_id uuid NOT NULL,
    chatroom_id uuid NOT NULL,
    provider text NOT NULL,
    model text NOT NULL,
    request_id text,
    input_tokens integer DEFAULT 0 NOT NULL,
    output_tokens integer DEFAULT 0 NOT NULL,
    cost_in numeric(18,6) DEFAULT 0 NOT NULL,
    cost_out numeric(18,6) DEFAULT 0 NOT NULL,
    meta jsonb
);


ALTER TABLE billing.ai_usage_events OWNER TO doran;

--
-- Name: monthly_user_costs; Type: TABLE; Schema: billing; Owner: doran
--

CREATE TABLE billing.monthly_user_costs (
    billing_month date NOT NULL,
    user_id uuid NOT NULL,
    input_tokens bigint DEFAULT 0 NOT NULL,
    output_tokens bigint DEFAULT 0 NOT NULL,
    cost_in numeric(18,6) DEFAULT 0 NOT NULL,
    cost_out numeric(18,6) DEFAULT 0 NOT NULL,
    total_cost numeric(18,6) DEFAULT 0 NOT NULL,
    last_aggregated_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE billing.monthly_user_costs OWNER TO doran;

--
-- Name: chatbots; Type: TABLE; Schema: chat_schema; Owner: doran
--

CREATE TABLE chat_schema.chatbots (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    display_name character varying(100) NOT NULL,
    description text,
    bot_type character varying(50) NOT NULL,
    model_name character varying(100),
    personality jsonb,
    system_prompt text,
    capabilities jsonb,
    settings jsonb,
    intimacy_level integer DEFAULT 1,
    avatar_url character varying(500),
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    created_by uuid,
    intimacy_system_prompt text,
    intimacy_user_prompt text,
    vocabulary_system_prompt text,
    vocabulary_user_prompt text,
    translation_system_prompt text,
    translation_user_prompt text,
    CONSTRAINT chatbots_intimacy_level_check CHECK (((intimacy_level >= 1) AND (intimacy_level <= 3))),
    CONSTRAINT chk_chatbots_type CHECK (((bot_type)::text = ANY (ARRAY[('gpt'::character varying)::text, ('claude'::character varying)::text, ('custom'::character varying)::text])))
);


ALTER TABLE chat_schema.chatbots OWNER TO doran;

--
-- Name: TABLE chatbots; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON TABLE chat_schema.chatbots IS 'AI 챗봇';


--
-- Name: COLUMN chatbots.id; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatbots.id IS '챗봇 아이디';


--
-- Name: COLUMN chatbots.name; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatbots.name IS '챗봇 이름';


--
-- Name: COLUMN chatbots.display_name; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatbots.display_name IS '표시 이름';


--
-- Name: COLUMN chatbots.description; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatbots.description IS '챗봇 설명';


--
-- Name: COLUMN chatbots.bot_type; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatbots.bot_type IS '챗봇 타입 (gpt, claude, custom)';


--
-- Name: COLUMN chatbots.model_name; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatbots.model_name IS 'AI 모델명';


--
-- Name: COLUMN chatbots.personality; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatbots.personality IS '챗봇 성격 설정 (JSONB)';


--
-- Name: COLUMN chatbots.system_prompt; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatbots.system_prompt IS '시스템 프롬프트';


--
-- Name: COLUMN chatbots.capabilities; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatbots.capabilities IS '챗봇 기능 설정 (JSONB)';


--
-- Name: COLUMN chatbots.settings; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatbots.settings IS '챗봇 설정 (JSONB)';


--
-- Name: COLUMN chatbots.intimacy_level; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatbots.intimacy_level IS '친밀도 레벨 (1=격식체, 2=부드러운 존댓말, 3=반말)';


--
-- Name: COLUMN chatbots.avatar_url; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatbots.avatar_url IS '아바타 URL';


--
-- Name: COLUMN chatbots.is_active; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatbots.is_active IS '활성 상태';


--
-- Name: COLUMN chatbots.created_at; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatbots.created_at IS '생성 시간';


--
-- Name: COLUMN chatbots.updated_at; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatbots.updated_at IS '수정 시간';


--
-- Name: COLUMN chatbots.created_by; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatbots.created_by IS '생성자';


--
-- Name: chatrooms; Type: TABLE; Schema: chat_schema; Owner: doran
--

CREATE TABLE chat_schema.chatrooms (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    chatbot_id uuid NOT NULL,
    user_id uuid NOT NULL,
    settings jsonb DEFAULT '{}'::jsonb,
    context_data jsonb,
    last_message_at timestamp without time zone,
    last_message_id uuid,
    is_archived boolean DEFAULT false,
    is_deleted boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now()
);


ALTER TABLE chat_schema.chatrooms OWNER TO doran;

--
-- Name: TABLE chatrooms; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON TABLE chat_schema.chatrooms IS '채팅방';


--
-- Name: COLUMN chatrooms.id; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatrooms.id IS '채팅방 아이디';


--
-- Name: COLUMN chatrooms.name; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatrooms.name IS '채팅방 이름';


--
-- Name: COLUMN chatrooms.description; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatrooms.description IS '채팅방 설명';


--
-- Name: COLUMN chatrooms.chatbot_id; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatrooms.chatbot_id IS '챗봇 아이디';


--
-- Name: COLUMN chatrooms.user_id; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatrooms.user_id IS '사용자 아이디';


--
-- Name: COLUMN chatrooms.settings; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatrooms.settings IS '채팅방 설정 (JSONB)';


--
-- Name: COLUMN chatrooms.context_data; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatrooms.context_data IS '대화 컨텍스트 데이터 (JSONB)';


--
-- Name: COLUMN chatrooms.last_message_at; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatrooms.last_message_at IS '마지막 메시지 시간';


--
-- Name: COLUMN chatrooms.last_message_id; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatrooms.last_message_id IS '마지막 메시지 아이디';


--
-- Name: COLUMN chatrooms.is_archived; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatrooms.is_archived IS '아카이브 여부';


--
-- Name: COLUMN chatrooms.is_deleted; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatrooms.is_deleted IS '삭제 여부';


--
-- Name: COLUMN chatrooms.created_at; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatrooms.created_at IS '생성 시간';


--
-- Name: COLUMN chatrooms.updated_at; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.chatrooms.updated_at IS '수정 시간';


--
-- Name: flyway_schema_history; Type: TABLE; Schema: chat_schema; Owner: doran
--

CREATE TABLE chat_schema.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE chat_schema.flyway_schema_history OWNER TO doran;

--
-- Name: intimacy_progress; Type: TABLE; Schema: chat_schema; Owner: doran
--

CREATE TABLE chat_schema.intimacy_progress (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    chatroom_id uuid NOT NULL,
    user_id uuid NOT NULL,
    intimacy_level integer DEFAULT 1 NOT NULL,
    total_corrections integer DEFAULT 0,
    last_feedback text,
    last_updated timestamp without time zone DEFAULT now(),
    progress_data jsonb,
    CONSTRAINT intimacy_progress_intimacy_level_check CHECK ((intimacy_level = ANY (ARRAY[1, 2, 3])))
);


ALTER TABLE chat_schema.intimacy_progress OWNER TO doran;

--
-- Name: TABLE intimacy_progress; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON TABLE chat_schema.intimacy_progress IS '채팅방별 친밀도 진척 추적';


--
-- Name: COLUMN intimacy_progress.intimacy_level; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.intimacy_progress.intimacy_level IS '현재 친밀도 레벨 (1=격식체, 2=부드러운 존댓말, 3=반말)';


--
-- Name: COLUMN intimacy_progress.total_corrections; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.intimacy_progress.total_corrections IS '누적 교정 횟수';


--
-- Name: COLUMN intimacy_progress.last_feedback; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.intimacy_progress.last_feedback IS '마지막 피드백 메시지';


--
-- Name: COLUMN intimacy_progress.progress_data; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.intimacy_progress.progress_data IS '세부 학습 통계 (JSONB)';


--
-- Name: messages; Type: TABLE; Schema: chat_schema; Owner: doran
--

CREATE TABLE chat_schema.messages (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    chatroom_id uuid NOT NULL,
    sender_type character varying(20) NOT NULL,
    sender_id uuid,
    content text NOT NULL,
    content_type character varying(20) DEFAULT 'text'::character varying,
    metadata jsonb,
    sequence_number bigint NOT NULL,
    token_count integer,
    processing_time_ms integer,
    is_edited boolean DEFAULT false,
    edited_at timestamp without time zone,
    is_deleted boolean DEFAULT false,
    deleted_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    parent_message_id uuid,
    turn_number bigint DEFAULT 0 NOT NULL,
    is_cancelled boolean DEFAULT false NOT NULL,
    cancelled_at timestamp without time zone,
    CONSTRAINT messages_content_type_check CHECK (((content_type)::text = ANY (ARRAY[('text'::character varying)::text, ('code'::character varying)::text, ('system'::character varying)::text, ('json'::character varying)::text]))),
    CONSTRAINT messages_sender_type_check CHECK (((sender_type)::text = ANY (ARRAY[('user'::character varying)::text, ('bot'::character varying)::text, ('system'::character varying)::text])))
);


ALTER TABLE chat_schema.messages OWNER TO doran;

--
-- Name: TABLE messages; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON TABLE chat_schema.messages IS '메시지';


--
-- Name: COLUMN messages.id; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.messages.id IS '메시지 아이디';


--
-- Name: COLUMN messages.chatroom_id; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.messages.chatroom_id IS '채팅방 아이디';


--
-- Name: COLUMN messages.sender_type; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.messages.sender_type IS '발신자 타입 (user, bot, system)';


--
-- Name: COLUMN messages.sender_id; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.messages.sender_id IS '발신자 아이디';


--
-- Name: COLUMN messages.content; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.messages.content IS '메시지 내용';


--
-- Name: COLUMN messages.content_type; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.messages.content_type IS '콘텐츠 타입 (text, code, system)';


--
-- Name: COLUMN messages.metadata; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.messages.metadata IS '메타데이터 (JSONB)';


--
-- Name: COLUMN messages.sequence_number; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.messages.sequence_number IS '대화 순서 번호';


--
-- Name: COLUMN messages.token_count; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.messages.token_count IS '토큰 수';


--
-- Name: COLUMN messages.processing_time_ms; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.messages.processing_time_ms IS '처리 시간 (밀리초)';


--
-- Name: COLUMN messages.is_edited; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.messages.is_edited IS '수정 여부';


--
-- Name: COLUMN messages.edited_at; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.messages.edited_at IS '수정 시간';


--
-- Name: COLUMN messages.is_deleted; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.messages.is_deleted IS '삭제 여부';


--
-- Name: COLUMN messages.deleted_at; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.messages.deleted_at IS '삭제 시간';


--
-- Name: COLUMN messages.created_at; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.messages.created_at IS '생성 시간';


--
-- Name: COLUMN messages.updated_at; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.messages.updated_at IS '수정 시간';


--
-- Name: COLUMN messages.turn_number; Type: COMMENT; Schema: chat_schema; Owner: doran
--

COMMENT ON COLUMN chat_schema.messages.turn_number IS '대화 턴 번호 (Bot 응답부터 User 응답까지 하나의 턴, 미완료 턴은 0)';


--
-- Name: user_chatbot_last_interaction; Type: TABLE; Schema: chat_schema; Owner: doran
--

CREATE TABLE chat_schema.user_chatbot_last_interaction (
    user_id uuid NOT NULL,
    chatbot_id uuid NOT NULL,
    last_interaction_at timestamp with time zone,
    last_room_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE chat_schema.user_chatbot_last_interaction OWNER TO doran;

--
-- Name: stores; Type: TABLE; Schema: store_schema; Owner: doran
--

CREATE TABLE store_schema.stores (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    message_id uuid NOT NULL,
    chatroom_id uuid NOT NULL,
    content text NOT NULL,
    corrected_content text,
    ai_response jsonb NOT NULL,
    bot_type character varying(20),
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE store_schema.stores OWNER TO doran;

--
-- Name: TABLE stores; Type: COMMENT; Schema: store_schema; Owner: doran
--

COMMENT ON TABLE store_schema.stores IS '보관함 - 사용자가 저장한 표현과 AI 응답';


--
-- Name: COLUMN stores.content; Type: COMMENT; Schema: store_schema; Owner: doran
--

COMMENT ON COLUMN store_schema.stores.content IS '표현 원본';


--
-- Name: COLUMN stores.corrected_content; Type: COMMENT; Schema: store_schema; Owner: doran
--

COMMENT ON COLUMN store_schema.stores.corrected_content IS '친밀도 Agent가 교정한 문장 (교정 없으면 NULL)';


--
-- Name: COLUMN stores.ai_response; Type: COMMENT; Schema: store_schema; Owner: doran
--

COMMENT ON COLUMN store_schema.stores.ai_response IS 'Multi-Agent AI 응답 (JSONB)';


--
-- Name: COLUMN stores.bot_type; Type: COMMENT; Schema: store_schema; Owner: doran
--

COMMENT ON COLUMN store_schema.stores.bot_type IS '챗봇 역할 (Honey, Coworker, Senior, Client)';


--
-- Name: admin_audit_logs; Type: TABLE; Schema: user_schema; Owner: doran
--

CREATE TABLE user_schema.admin_audit_logs (
    id bigint NOT NULL,
    admin_user_id uuid NOT NULL,
    action_type character varying(50) NOT NULL,
    target_type character varying(50),
    target_id bigint,
    summary character varying(500),
    before_json jsonb,
    after_json jsonb,
    ip character varying(50),
    user_agent character varying(500),
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE user_schema.admin_audit_logs OWNER TO doran;

--
-- Name: admin_audit_logs_id_seq; Type: SEQUENCE; Schema: user_schema; Owner: doran
--

CREATE SEQUENCE user_schema.admin_audit_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE user_schema.admin_audit_logs_id_seq OWNER TO doran;

--
-- Name: admin_audit_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: user_schema; Owner: doran
--

ALTER SEQUENCE user_schema.admin_audit_logs_id_seq OWNED BY user_schema.admin_audit_logs.id;


--
-- Name: admin_roles; Type: TABLE; Schema: user_schema; Owner: doran
--

CREATE TABLE user_schema.admin_roles (
    id bigint NOT NULL,
    role_name character varying(50) NOT NULL,
    description character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE user_schema.admin_roles OWNER TO doran;

--
-- Name: admin_roles_id_seq; Type: SEQUENCE; Schema: user_schema; Owner: doran
--

CREATE SEQUENCE user_schema.admin_roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE user_schema.admin_roles_id_seq OWNER TO doran;

--
-- Name: admin_roles_id_seq; Type: SEQUENCE OWNED BY; Schema: user_schema; Owner: doran
--

ALTER SEQUENCE user_schema.admin_roles_id_seq OWNED BY user_schema.admin_roles.id;


--
-- Name: admin_user_roles; Type: TABLE; Schema: user_schema; Owner: doran
--

CREATE TABLE user_schema.admin_user_roles (
    admin_user_id bigint NOT NULL,
    admin_role_id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE user_schema.admin_user_roles OWNER TO doran;

--
-- Name: admin_users; Type: TABLE; Schema: user_schema; Owner: doran
--

CREATE TABLE user_schema.admin_users (
    id bigint NOT NULL,
    username character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE user_schema.admin_users OWNER TO doran;

--
-- Name: admin_users_id_seq; Type: SEQUENCE; Schema: user_schema; Owner: doran
--

CREATE SEQUENCE user_schema.admin_users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE user_schema.admin_users_id_seq OWNER TO doran;

--
-- Name: admin_users_id_seq; Type: SEQUENCE OWNED BY; Schema: user_schema; Owner: doran
--

ALTER SEQUENCE user_schema.admin_users_id_seq OWNED BY user_schema.admin_users.id;


--
-- Name: app_user; Type: TABLE; Schema: user_schema; Owner: doran
--

CREATE TABLE user_schema.app_user (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    email character varying(320) NOT NULL,
    first_name character varying(50) NOT NULL,
    last_name character varying(50) NOT NULL,
    name character varying(50) NOT NULL,
    password_hash character varying(100),
    picture character varying(255),
    info character varying(100) DEFAULT ''::character varying NOT NULL,
    last_conn_time timestamp without time zone DEFAULT now() NOT NULL,
    status character varying(255) DEFAULT 'ACTIVE'::character varying NOT NULL,
    role character varying(20) DEFAULT 'ROLE_USER'::character varying NOT NULL,
    coach_check boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    exit_modal_do_not_show_again boolean DEFAULT false NOT NULL,
    oauth_id character varying(255),
    oauth_provider character varying(20),
    is_onboard boolean DEFAULT false NOT NULL,
    birth_date date DEFAULT '1900-01-01'::date NOT NULL,
    signup_question character varying(255) DEFAULT '질문이 설정되지 않았습니다.'::character varying NOT NULL,
    signup_answer character varying(30) DEFAULT '답변이 설정되지 않았습니다.'::character varying NOT NULL,
    CONSTRAINT app_user_oauth_provider_check CHECK (((oauth_provider)::text = ANY ((ARRAY['GOOGLE'::character varying, 'FACEBOOK'::character varying, 'KAKAO'::character varying, 'NAVER'::character varying])::text[]))),
    CONSTRAINT chk_app_user_role CHECK (((role)::text = ANY (ARRAY[('ROLE_USER'::character varying)::text, ('ROLE_ADMIN'::character varying)::text]))),
    CONSTRAINT chk_app_user_status CHECK (((status)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('SUSPENDED'::character varying)::text])))
);


ALTER TABLE user_schema.app_user OWNER TO doran;

--
-- Name: TABLE app_user; Type: COMMENT; Schema: user_schema; Owner: doran
--

COMMENT ON TABLE user_schema.app_user IS '사용자 정보';


--
-- Name: COLUMN app_user.id; Type: COMMENT; Schema: user_schema; Owner: doran
--

COMMENT ON COLUMN user_schema.app_user.id IS '사용자 아이디';


--
-- Name: COLUMN app_user.email; Type: COMMENT; Schema: user_schema; Owner: doran
--

COMMENT ON COLUMN user_schema.app_user.email IS '사용자 이메일';


--
-- Name: COLUMN app_user.first_name; Type: COMMENT; Schema: user_schema; Owner: doran
--

COMMENT ON COLUMN user_schema.app_user.first_name IS '이름';


--
-- Name: COLUMN app_user.last_name; Type: COMMENT; Schema: user_schema; Owner: doran
--

COMMENT ON COLUMN user_schema.app_user.last_name IS '성';


--
-- Name: COLUMN app_user.name; Type: COMMENT; Schema: user_schema; Owner: doran
--

COMMENT ON COLUMN user_schema.app_user.name IS '전체 이름';


--
-- Name: COLUMN app_user.password_hash; Type: COMMENT; Schema: user_schema; Owner: doran
--

COMMENT ON COLUMN user_schema.app_user.password_hash IS '비밀번호 해시';


--
-- Name: COLUMN app_user.picture; Type: COMMENT; Schema: user_schema; Owner: doran
--

COMMENT ON COLUMN user_schema.app_user.picture IS '프로필 사진';


--
-- Name: COLUMN app_user.info; Type: COMMENT; Schema: user_schema; Owner: doran
--

COMMENT ON COLUMN user_schema.app_user.info IS '사용자 정보';


--
-- Name: COLUMN app_user.last_conn_time; Type: COMMENT; Schema: user_schema; Owner: doran
--

COMMENT ON COLUMN user_schema.app_user.last_conn_time IS '마지막 연결 시간';


--
-- Name: COLUMN app_user.status; Type: COMMENT; Schema: user_schema; Owner: doran
--

COMMENT ON COLUMN user_schema.app_user.status IS '사용자 상태 (ACTIVE, INACTIVE, SUSPENDED)';


--
-- Name: COLUMN app_user.role; Type: COMMENT; Schema: user_schema; Owner: doran
--

COMMENT ON COLUMN user_schema.app_user.role IS '사용자 역할 (ROLE_USER, ROLE_ADMIN)';


--
-- Name: COLUMN app_user.coach_check; Type: COMMENT; Schema: user_schema; Owner: doran
--

COMMENT ON COLUMN user_schema.app_user.coach_check IS '코치 체크 여부';


--
-- Name: COLUMN app_user.created_at; Type: COMMENT; Schema: user_schema; Owner: doran
--

COMMENT ON COLUMN user_schema.app_user.created_at IS '생성 시간';


--
-- Name: COLUMN app_user.updated_at; Type: COMMENT; Schema: user_schema; Owner: doran
--

COMMENT ON COLUMN user_schema.app_user.updated_at IS '수정 시간';


--
-- Name: fcm_tokens; Type: TABLE; Schema: user_schema; Owner: doran
--

CREATE TABLE user_schema.fcm_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    token text NOT NULL,
    platform character varying(20) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE user_schema.fcm_tokens OWNER TO doran;

--
-- Name: interest_topics; Type: TABLE; Schema: user_schema; Owner: doran
--

CREATE TABLE user_schema.interest_topics (
    topic_key character varying(50) NOT NULL,
    label character varying(100) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE user_schema.interest_topics OWNER TO doran;

--
-- Name: posts_cache; Type: TABLE; Schema: user_schema; Owner: doran
--

CREATE TABLE user_schema.posts_cache (
    external_id character varying(100) NOT NULL,
    title text,
    image_url text,
    description text,
    permalink text,
    published_at timestamp without time zone,
    fetched_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE user_schema.posts_cache OWNER TO doran;

--
-- Name: profiles; Type: TABLE; Schema: user_schema; Owner: doran
--

CREATE TABLE user_schema.profiles (
    id bigint NOT NULL,
    avatar_url character varying(500),
    bio text,
    created_at timestamp(6) without time zone NOT NULL,
    settings jsonb,
    updated_at timestamp(6) without time zone NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE user_schema.profiles OWNER TO doran;

--
-- Name: profiles_id_seq; Type: SEQUENCE; Schema: user_schema; Owner: doran
--

ALTER TABLE user_schema.profiles ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME user_schema.profiles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: prompt_actives; Type: TABLE; Schema: user_schema; Owner: doran
--

CREATE TABLE user_schema.prompt_actives (
    env character varying(20) DEFAULT 'prod'::character varying NOT NULL,
    agent_type character varying(50) NOT NULL,
    concept character varying(20) NOT NULL,
    intimacy_level integer NOT NULL,
    prompt_version_id bigint NOT NULL,
    activated_by uuid NOT NULL,
    activated_at timestamp without time zone DEFAULT now(),
    CONSTRAINT chk_prompt_active_intimacy_level CHECK ((intimacy_level = ANY (ARRAY[1, 3])))
);


ALTER TABLE user_schema.prompt_actives OWNER TO doran;

--
-- Name: prompt_versions; Type: TABLE; Schema: user_schema; Owner: doran
--

CREATE TABLE user_schema.prompt_versions (
    id bigint NOT NULL,
    agent_type character varying(50) NOT NULL,
    concept character varying(20) NOT NULL,
    intimacy_level integer NOT NULL,
    version character varying(20) NOT NULL,
    content text NOT NULL,
    file_path character varying(500) NOT NULL,
    memo character varying(500),
    parent_version_id bigint,
    created_by uuid NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    CONSTRAINT chk_prompt_intimacy_level CHECK ((intimacy_level = ANY (ARRAY[1, 3])))
);


ALTER TABLE user_schema.prompt_versions OWNER TO doran;

--
-- Name: prompt_versions_id_seq; Type: SEQUENCE; Schema: user_schema; Owner: doran
--

CREATE SEQUENCE user_schema.prompt_versions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE user_schema.prompt_versions_id_seq OWNER TO doran;

--
-- Name: prompt_versions_id_seq; Type: SEQUENCE OWNED BY; Schema: user_schema; Owner: doran
--

ALTER SEQUENCE user_schema.prompt_versions_id_seq OWNED BY user_schema.prompt_versions.id;


--
-- Name: push_delivery_logs; Type: TABLE; Schema: user_schema; Owner: doran
--

CREATE TABLE user_schema.push_delivery_logs (
    id bigint NOT NULL,
    user_id uuid NOT NULL,
    chatroom_id uuid NOT NULL,
    sent_date date NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE user_schema.push_delivery_logs OWNER TO doran;

--
-- Name: push_delivery_logs_id_seq; Type: SEQUENCE; Schema: user_schema; Owner: doran
--

CREATE SEQUENCE user_schema.push_delivery_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE user_schema.push_delivery_logs_id_seq OWNER TO doran;

--
-- Name: push_delivery_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: user_schema; Owner: doran
--

ALTER SEQUENCE user_schema.push_delivery_logs_id_seq OWNED BY user_schema.push_delivery_logs.id;


--
-- Name: review_ticket_items; Type: TABLE; Schema: user_schema; Owner: doran
--

CREATE TABLE user_schema.review_ticket_items (
    id bigint NOT NULL,
    ticket_id bigint NOT NULL,
    message_id uuid,
    agent_type character varying(50) NOT NULL,
    snapshot_json jsonb,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE user_schema.review_ticket_items OWNER TO doran;

--
-- Name: review_ticket_items_id_seq; Type: SEQUENCE; Schema: user_schema; Owner: doran
--

CREATE SEQUENCE user_schema.review_ticket_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE user_schema.review_ticket_items_id_seq OWNER TO doran;

--
-- Name: review_ticket_items_id_seq; Type: SEQUENCE OWNED BY; Schema: user_schema; Owner: doran
--

ALTER SEQUENCE user_schema.review_ticket_items_id_seq OWNED BY user_schema.review_ticket_items.id;


--
-- Name: review_tickets; Type: TABLE; Schema: user_schema; Owner: doran
--

CREATE TABLE user_schema.review_tickets (
    id bigint NOT NULL,
    conversation_id uuid,
    status character varying(20) DEFAULT 'OPEN'::character varying NOT NULL,
    agent_type character varying(50),
    note text,
    created_by uuid,
    assignee uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    done_at timestamp without time zone
);


ALTER TABLE user_schema.review_tickets OWNER TO doran;

--
-- Name: review_tickets_id_seq; Type: SEQUENCE; Schema: user_schema; Owner: doran
--

CREATE SEQUENCE user_schema.review_tickets_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE user_schema.review_tickets_id_seq OWNER TO doran;

--
-- Name: review_tickets_id_seq; Type: SEQUENCE OWNED BY; Schema: user_schema; Owner: doran
--

ALTER SEQUENCE user_schema.review_tickets_id_seq OWNED BY user_schema.review_tickets.id;


--
-- Name: settings; Type: TABLE; Schema: user_schema; Owner: doran
--

CREATE TABLE user_schema.settings (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    setting_key character varying(100) NOT NULL,
    setting_value text,
    updated_at timestamp(6) without time zone NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE user_schema.settings OWNER TO doran;

--
-- Name: settings_id_seq; Type: SEQUENCE; Schema: user_schema; Owner: doran
--

ALTER TABLE user_schema.settings ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME user_schema.settings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: support_requests; Type: TABLE; Schema: user_schema; Owner: doran
--

CREATE TABLE user_schema.support_requests (
    id bigint NOT NULL,
    user_id uuid NOT NULL,
    requester_email character varying(320) NOT NULL,
    type character varying(20) NOT NULL,
    category character varying(100),
    content text NOT NULL,
    reply_requested boolean DEFAULT false NOT NULL,
    reply_email character varying(320),
    chatroom_id uuid,
    message_id uuid,
    message_content text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    ai_response_snapshot jsonb,
    requester_name character varying(100),
    CONSTRAINT chk_support_type CHECK (((type)::text = ANY ((ARRAY['INQUIRY'::character varying, 'REPORT'::character varying])::text[])))
);


ALTER TABLE user_schema.support_requests OWNER TO doran;

--
-- Name: support_requests_id_seq; Type: SEQUENCE; Schema: user_schema; Owner: doran
--

CREATE SEQUENCE user_schema.support_requests_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE user_schema.support_requests_id_seq OWNER TO doran;

--
-- Name: support_requests_id_seq; Type: SEQUENCE OWNED BY; Schema: user_schema; Owner: doran
--

ALTER SEQUENCE user_schema.support_requests_id_seq OWNED BY user_schema.support_requests.id;


--
-- Name: user_interest_topics; Type: TABLE; Schema: user_schema; Owner: doran
--

CREATE TABLE user_schema.user_interest_topics (
    user_id uuid NOT NULL,
    topic_key character varying(50) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE user_schema.user_interest_topics OWNER TO doran;

--
-- Name: user_notification_settings; Type: TABLE; Schema: user_schema; Owner: doran
--

CREATE TABLE user_schema.user_notification_settings (
    user_id uuid NOT NULL,
    push_enabled boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE user_schema.user_notification_settings OWNER TO doran;

--
-- Name: user_stats; Type: TABLE; Schema: user_schema; Owner: doran
--

CREATE TABLE user_schema.user_stats (
    user_id uuid NOT NULL,
    streak_count integer DEFAULT 0 NOT NULL,
    last_active_date date,
    perfect_count integer DEFAULT 0 NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE user_schema.user_stats OWNER TO doran;

--
-- Name: admin_audit_logs id; Type: DEFAULT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.admin_audit_logs ALTER COLUMN id SET DEFAULT nextval('user_schema.admin_audit_logs_id_seq'::regclass);


--
-- Name: admin_roles id; Type: DEFAULT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.admin_roles ALTER COLUMN id SET DEFAULT nextval('user_schema.admin_roles_id_seq'::regclass);


--
-- Name: admin_users id; Type: DEFAULT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.admin_users ALTER COLUMN id SET DEFAULT nextval('user_schema.admin_users_id_seq'::regclass);


--
-- Name: prompt_versions id; Type: DEFAULT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.prompt_versions ALTER COLUMN id SET DEFAULT nextval('user_schema.prompt_versions_id_seq'::regclass);


--
-- Name: push_delivery_logs id; Type: DEFAULT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.push_delivery_logs ALTER COLUMN id SET DEFAULT nextval('user_schema.push_delivery_logs_id_seq'::regclass);


--
-- Name: review_ticket_items id; Type: DEFAULT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.review_ticket_items ALTER COLUMN id SET DEFAULT nextval('user_schema.review_ticket_items_id_seq'::regclass);


--
-- Name: review_tickets id; Type: DEFAULT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.review_tickets ALTER COLUMN id SET DEFAULT nextval('user_schema.review_tickets_id_seq'::regclass);


--
-- Name: support_requests id; Type: DEFAULT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.support_requests ALTER COLUMN id SET DEFAULT nextval('user_schema.support_requests_id_seq'::regclass);


--
-- Name: arch_agent_results arch_agent_results_pkey; Type: CONSTRAINT; Schema: archive_schema; Owner: doran
--

ALTER TABLE ONLY archive_schema.arch_agent_results
    ADD CONSTRAINT arch_agent_results_pkey PRIMARY KEY (id);


--
-- Name: arch_chatrooms arch_chatrooms_pkey; Type: CONSTRAINT; Schema: archive_schema; Owner: doran
--

ALTER TABLE ONLY archive_schema.arch_chatrooms
    ADD CONSTRAINT arch_chatrooms_pkey PRIMARY KEY (id);


--
-- Name: arch_chatrooms arch_chatrooms_source_chatroom_id_key; Type: CONSTRAINT; Schema: archive_schema; Owner: doran
--

ALTER TABLE ONLY archive_schema.arch_chatrooms
    ADD CONSTRAINT arch_chatrooms_source_chatroom_id_key UNIQUE (source_chatroom_id);


--
-- Name: arch_ingestion_state arch_ingestion_state_job_name_key; Type: CONSTRAINT; Schema: archive_schema; Owner: doran
--

ALTER TABLE ONLY archive_schema.arch_ingestion_state
    ADD CONSTRAINT arch_ingestion_state_job_name_key UNIQUE (job_name);


--
-- Name: arch_ingestion_state arch_ingestion_state_pkey; Type: CONSTRAINT; Schema: archive_schema; Owner: doran
--

ALTER TABLE ONLY archive_schema.arch_ingestion_state
    ADD CONSTRAINT arch_ingestion_state_pkey PRIMARY KEY (id);


--
-- Name: arch_intimacy_progress arch_intimacy_progress_pkey; Type: CONSTRAINT; Schema: archive_schema; Owner: doran
--

ALTER TABLE ONLY archive_schema.arch_intimacy_progress
    ADD CONSTRAINT arch_intimacy_progress_pkey PRIMARY KEY (id);


--
-- Name: arch_intimacy_progress arch_intimacy_progress_source_intimacy_progress_id_key; Type: CONSTRAINT; Schema: archive_schema; Owner: doran
--

ALTER TABLE ONLY archive_schema.arch_intimacy_progress
    ADD CONSTRAINT arch_intimacy_progress_source_intimacy_progress_id_key UNIQUE (source_intimacy_progress_id);


--
-- Name: arch_messages arch_messages_pkey; Type: CONSTRAINT; Schema: archive_schema; Owner: doran
--

ALTER TABLE ONLY archive_schema.arch_messages
    ADD CONSTRAINT arch_messages_pkey PRIMARY KEY (id);


--
-- Name: arch_messages arch_messages_source_message_id_key; Type: CONSTRAINT; Schema: archive_schema; Owner: doran
--

ALTER TABLE ONLY archive_schema.arch_messages
    ADD CONSTRAINT arch_messages_source_message_id_key UNIQUE (source_message_id);


--
-- Name: arch_stores arch_stores_pkey; Type: CONSTRAINT; Schema: archive_schema; Owner: doran
--

ALTER TABLE ONLY archive_schema.arch_stores
    ADD CONSTRAINT arch_stores_pkey PRIMARY KEY (id);


--
-- Name: arch_stores arch_stores_source_store_id_key; Type: CONSTRAINT; Schema: archive_schema; Owner: doran
--

ALTER TABLE ONLY archive_schema.arch_stores
    ADD CONSTRAINT arch_stores_source_store_id_key UNIQUE (source_store_id);


--
-- Name: arch_usage_events arch_usage_events_pkey; Type: CONSTRAINT; Schema: archive_schema; Owner: doran
--

ALTER TABLE ONLY archive_schema.arch_usage_events
    ADD CONSTRAINT arch_usage_events_pkey PRIMARY KEY (id);


--
-- Name: arch_usage_events arch_usage_events_source_usage_event_id_key; Type: CONSTRAINT; Schema: archive_schema; Owner: doran
--

ALTER TABLE ONLY archive_schema.arch_usage_events
    ADD CONSTRAINT arch_usage_events_source_usage_event_id_key UNIQUE (source_usage_event_id);


--
-- Name: management_queue management_queue_pkey; Type: CONSTRAINT; Schema: archive_schema; Owner: doran
--

ALTER TABLE ONLY archive_schema.management_queue
    ADD CONSTRAINT management_queue_pkey PRIMARY KEY (id);


--
-- Name: auth_events auth_events_pkey; Type: CONSTRAINT; Schema: auth_schema; Owner: doran
--

ALTER TABLE ONLY auth_schema.auth_events
    ADD CONSTRAINT auth_events_pkey PRIMARY KEY (id);


--
-- Name: email_verifications email_verifications_pkey; Type: CONSTRAINT; Schema: auth_schema; Owner: doran
--

ALTER TABLE ONLY auth_schema.email_verifications
    ADD CONSTRAINT email_verifications_pkey PRIMARY KEY (id);


--
-- Name: login_attempts login_attempts_pkey; Type: CONSTRAINT; Schema: auth_schema; Owner: doran
--

ALTER TABLE ONLY auth_schema.login_attempts
    ADD CONSTRAINT login_attempts_pkey PRIMARY KEY (id);


--
-- Name: password_reset_tokens password_reset_tokens_pkey; Type: CONSTRAINT; Schema: auth_schema; Owner: doran
--

ALTER TABLE ONLY auth_schema.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: auth_schema; Owner: doran
--

ALTER TABLE ONLY auth_schema.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: token_blacklist token_blacklist_pkey; Type: CONSTRAINT; Schema: auth_schema; Owner: doran
--

ALTER TABLE ONLY auth_schema.token_blacklist
    ADD CONSTRAINT token_blacklist_pkey PRIMARY KEY (id);


--
-- Name: token_blacklist uk85fjfavynxyo748kpsj9o6if1; Type: CONSTRAINT; Schema: auth_schema; Owner: doran
--

ALTER TABLE ONLY auth_schema.token_blacklist
    ADD CONSTRAINT uk85fjfavynxyo748kpsj9o6if1 UNIQUE (token_hash);


--
-- Name: ai_usage_events ai_usage_events_pkey; Type: CONSTRAINT; Schema: billing; Owner: doran
--

ALTER TABLE ONLY billing.ai_usage_events
    ADD CONSTRAINT ai_usage_events_pkey PRIMARY KEY (id);


--
-- Name: ai_usage_events ai_usage_events_request_id_key; Type: CONSTRAINT; Schema: billing; Owner: doran
--

ALTER TABLE ONLY billing.ai_usage_events
    ADD CONSTRAINT ai_usage_events_request_id_key UNIQUE (request_id);


--
-- Name: monthly_user_costs monthly_user_costs_pk; Type: CONSTRAINT; Schema: billing; Owner: doran
--

ALTER TABLE ONLY billing.monthly_user_costs
    ADD CONSTRAINT monthly_user_costs_pk PRIMARY KEY (billing_month, user_id);


--
-- Name: chatbots chatbots_pk; Type: CONSTRAINT; Schema: chat_schema; Owner: doran
--

ALTER TABLE ONLY chat_schema.chatbots
    ADD CONSTRAINT chatbots_pk PRIMARY KEY (id);


--
-- Name: chatrooms chatrooms_pk; Type: CONSTRAINT; Schema: chat_schema; Owner: doran
--

ALTER TABLE ONLY chat_schema.chatrooms
    ADD CONSTRAINT chatrooms_pk PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: chat_schema; Owner: doran
--

ALTER TABLE ONLY chat_schema.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: intimacy_progress intimacy_progress_pkey; Type: CONSTRAINT; Schema: chat_schema; Owner: doran
--

ALTER TABLE ONLY chat_schema.intimacy_progress
    ADD CONSTRAINT intimacy_progress_pkey PRIMARY KEY (id);


--
-- Name: messages messages_pk; Type: CONSTRAINT; Schema: chat_schema; Owner: doran
--

ALTER TABLE ONLY chat_schema.messages
    ADD CONSTRAINT messages_pk PRIMARY KEY (id);


--
-- Name: intimacy_progress uq_intimacy_chatroom; Type: CONSTRAINT; Schema: chat_schema; Owner: doran
--

ALTER TABLE ONLY chat_schema.intimacy_progress
    ADD CONSTRAINT uq_intimacy_chatroom UNIQUE (chatroom_id);


--
-- Name: messages uq_messages_room_seq; Type: CONSTRAINT; Schema: chat_schema; Owner: doran
--

ALTER TABLE ONLY chat_schema.messages
    ADD CONSTRAINT uq_messages_room_seq UNIQUE (chatroom_id, sequence_number);


--
-- Name: user_chatbot_last_interaction user_chatbot_last_interaction_pk; Type: CONSTRAINT; Schema: chat_schema; Owner: doran
--

ALTER TABLE ONLY chat_schema.user_chatbot_last_interaction
    ADD CONSTRAINT user_chatbot_last_interaction_pk PRIMARY KEY (user_id, chatbot_id);


--
-- Name: stores stores_pkey; Type: CONSTRAINT; Schema: store_schema; Owner: doran
--

ALTER TABLE ONLY store_schema.stores
    ADD CONSTRAINT stores_pkey PRIMARY KEY (id);


--
-- Name: admin_audit_logs admin_audit_logs_pkey; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.admin_audit_logs
    ADD CONSTRAINT admin_audit_logs_pkey PRIMARY KEY (id);


--
-- Name: admin_roles admin_roles_pkey; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.admin_roles
    ADD CONSTRAINT admin_roles_pkey PRIMARY KEY (id);


--
-- Name: admin_roles admin_roles_role_name_key; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.admin_roles
    ADD CONSTRAINT admin_roles_role_name_key UNIQUE (role_name);


--
-- Name: admin_user_roles admin_user_roles_pkey; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.admin_user_roles
    ADD CONSTRAINT admin_user_roles_pkey PRIMARY KEY (admin_user_id, admin_role_id);


--
-- Name: admin_users admin_users_pkey; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.admin_users
    ADD CONSTRAINT admin_users_pkey PRIMARY KEY (id);


--
-- Name: admin_users admin_users_username_key; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.admin_users
    ADD CONSTRAINT admin_users_username_key UNIQUE (username);


--
-- Name: app_user app_user_pk; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.app_user
    ADD CONSTRAINT app_user_pk PRIMARY KEY (id);


--
-- Name: fcm_tokens fcm_tokens_pkey; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.fcm_tokens
    ADD CONSTRAINT fcm_tokens_pkey PRIMARY KEY (id);


--
-- Name: interest_topics interest_topics_pkey; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.interest_topics
    ADD CONSTRAINT interest_topics_pkey PRIMARY KEY (topic_key);


--
-- Name: posts_cache posts_cache_pkey; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.posts_cache
    ADD CONSTRAINT posts_cache_pkey PRIMARY KEY (external_id);


--
-- Name: profiles profiles_pkey; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.profiles
    ADD CONSTRAINT profiles_pkey PRIMARY KEY (id);


--
-- Name: prompt_actives prompt_actives_pkey; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.prompt_actives
    ADD CONSTRAINT prompt_actives_pkey PRIMARY KEY (env, agent_type, concept, intimacy_level);


--
-- Name: prompt_versions prompt_versions_pkey; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.prompt_versions
    ADD CONSTRAINT prompt_versions_pkey PRIMARY KEY (id);


--
-- Name: push_delivery_logs push_delivery_logs_pkey; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.push_delivery_logs
    ADD CONSTRAINT push_delivery_logs_pkey PRIMARY KEY (id);


--
-- Name: review_ticket_items review_ticket_items_pkey; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.review_ticket_items
    ADD CONSTRAINT review_ticket_items_pkey PRIMARY KEY (id);


--
-- Name: review_tickets review_tickets_pkey; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.review_tickets
    ADD CONSTRAINT review_tickets_pkey PRIMARY KEY (id);


--
-- Name: settings settings_pkey; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.settings
    ADD CONSTRAINT settings_pkey PRIMARY KEY (id);


--
-- Name: support_requests support_requests_pkey; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.support_requests
    ADD CONSTRAINT support_requests_pkey PRIMARY KEY (id);


--
-- Name: profiles uk4ixsj6aqve5pxrbw2u0oyk8bb; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.profiles
    ADD CONSTRAINT uk4ixsj6aqve5pxrbw2u0oyk8bb UNIQUE (user_id);


--
-- Name: prompt_versions uk_prompt_agent_concept_level_version; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.prompt_versions
    ADD CONSTRAINT uk_prompt_agent_concept_level_version UNIQUE (agent_type, concept, intimacy_level, version);


--
-- Name: fcm_tokens uq_fcm_user_token; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.fcm_tokens
    ADD CONSTRAINT uq_fcm_user_token UNIQUE (user_id, token);


--
-- Name: push_delivery_logs uq_push_delivery; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.push_delivery_logs
    ADD CONSTRAINT uq_push_delivery UNIQUE (user_id, chatroom_id, sent_date);


--
-- Name: user_interest_topics user_interest_topics_pkey; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.user_interest_topics
    ADD CONSTRAINT user_interest_topics_pkey PRIMARY KEY (user_id, topic_key);


--
-- Name: user_notification_settings user_notification_settings_pkey; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.user_notification_settings
    ADD CONSTRAINT user_notification_settings_pkey PRIMARY KEY (user_id);


--
-- Name: user_stats user_stats_pkey; Type: CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.user_stats
    ADD CONSTRAINT user_stats_pkey PRIMARY KEY (user_id);


--
-- Name: idx_arch_agent_results_archived; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_agent_results_archived ON archive_schema.arch_agent_results USING btree (archived_at);


--
-- Name: idx_arch_agent_results_payload_gin; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_agent_results_payload_gin ON archive_schema.arch_agent_results USING gin (payload_json);


--
-- Name: idx_arch_agent_results_request_id; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_agent_results_request_id ON archive_schema.arch_agent_results USING btree (request_id);


--
-- Name: idx_arch_chatrooms_archived; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_chatrooms_archived ON archive_schema.arch_chatrooms USING btree (archived_at DESC);


--
-- Name: idx_arch_chatrooms_concept; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_chatrooms_concept ON archive_schema.arch_chatrooms USING btree (concept);


--
-- Name: idx_arch_chatrooms_created; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_chatrooms_created ON archive_schema.arch_chatrooms USING btree (source_created_at DESC);


--
-- Name: idx_arch_chatrooms_meta_gin; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_chatrooms_meta_gin ON archive_schema.arch_chatrooms USING gin (meta);


--
-- Name: idx_arch_chatrooms_user_created; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_chatrooms_user_created ON archive_schema.arch_chatrooms USING btree (user_id, source_created_at DESC);


--
-- Name: idx_arch_intimacy_progress_archived; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_intimacy_progress_archived ON archive_schema.arch_intimacy_progress USING btree (archived_at);


--
-- Name: idx_arch_intimacy_progress_chatroom; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_intimacy_progress_chatroom ON archive_schema.arch_intimacy_progress USING btree (arch_chatroom_id);


--
-- Name: idx_arch_intimacy_progress_data_gin; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_intimacy_progress_data_gin ON archive_schema.arch_intimacy_progress USING gin (progress_data);


--
-- Name: idx_arch_intimacy_progress_user; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_intimacy_progress_user ON archive_schema.arch_intimacy_progress USING btree (user_id);


--
-- Name: idx_arch_messages_archived; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_messages_archived ON archive_schema.arch_messages USING btree (archived_at);


--
-- Name: idx_arch_messages_created; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_messages_created ON archive_schema.arch_messages USING btree (source_created_at);


--
-- Name: idx_arch_messages_metadata_gin; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_messages_metadata_gin ON archive_schema.arch_messages USING gin (metadata_json);


--
-- Name: idx_arch_messages_room_sequence; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_messages_room_sequence ON archive_schema.arch_messages USING btree (arch_chatroom_id, sequence_number);


--
-- Name: idx_arch_messages_room_turn; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_messages_room_turn ON archive_schema.arch_messages USING btree (arch_chatroom_id, turn_number);


--
-- Name: idx_arch_stores_ai_response_gin; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_stores_ai_response_gin ON archive_schema.arch_stores USING gin (ai_response);


--
-- Name: idx_arch_stores_archived; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_stores_archived ON archive_schema.arch_stores USING btree (archived_at);


--
-- Name: idx_arch_stores_chatroom; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_stores_chatroom ON archive_schema.arch_stores USING btree (arch_chatroom_id);


--
-- Name: idx_arch_stores_message; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_stores_message ON archive_schema.arch_stores USING btree (source_message_id);


--
-- Name: idx_arch_stores_user; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_stores_user ON archive_schema.arch_stores USING btree (user_id);


--
-- Name: idx_arch_usage_events_archived; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_usage_events_archived ON archive_schema.arch_usage_events USING btree (archived_at);


--
-- Name: idx_arch_usage_events_chatroom; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_usage_events_chatroom ON archive_schema.arch_usage_events USING btree (arch_chatroom_id);


--
-- Name: idx_arch_usage_events_event_time; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_usage_events_event_time ON archive_schema.arch_usage_events USING btree (event_time);


--
-- Name: idx_arch_usage_events_meta_gin; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_usage_events_meta_gin ON archive_schema.arch_usage_events USING gin (meta);


--
-- Name: idx_arch_usage_events_request_id; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_usage_events_request_id ON archive_schema.arch_usage_events USING btree (request_id);


--
-- Name: idx_arch_usage_events_user; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_arch_usage_events_user ON archive_schema.arch_usage_events USING btree (user_id);


--
-- Name: idx_management_queue_admin; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_management_queue_admin ON archive_schema.management_queue USING btree (admin_name);


--
-- Name: idx_management_queue_created_at; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_management_queue_created_at ON archive_schema.management_queue USING btree (created_at DESC);


--
-- Name: idx_management_queue_request_data_gin; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_management_queue_request_data_gin ON archive_schema.management_queue USING gin (request_data);


--
-- Name: idx_management_queue_status; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_management_queue_status ON archive_schema.management_queue USING btree (status);


--
-- Name: idx_management_queue_type; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE INDEX idx_management_queue_type ON archive_schema.management_queue USING btree (queue_type);


--
-- Name: uq_arch_agent_results_msg_agent; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE UNIQUE INDEX uq_arch_agent_results_msg_agent ON archive_schema.arch_agent_results USING btree (arch_message_id, agent_type);


--
-- Name: uq_arch_messages_room_seq; Type: INDEX; Schema: archive_schema; Owner: doran
--

CREATE UNIQUE INDEX uq_arch_messages_room_seq ON archive_schema.arch_messages USING btree (arch_chatroom_id, sequence_number);


--
-- Name: idx_ai_usage_events_time; Type: INDEX; Schema: billing; Owner: doran
--

CREATE INDEX idx_ai_usage_events_time ON billing.ai_usage_events USING btree (event_time);


--
-- Name: idx_ai_usage_events_user_time; Type: INDEX; Schema: billing; Owner: doran
--

CREATE INDEX idx_ai_usage_events_user_time ON billing.ai_usage_events USING btree (user_id, event_time);


--
-- Name: idx_monthly_user_costs_month; Type: INDEX; Schema: billing; Owner: doran
--

CREATE INDEX idx_monthly_user_costs_month ON billing.monthly_user_costs USING btree (billing_month);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: chat_schema; Owner: doran
--

CREATE INDEX flyway_schema_history_s_idx ON chat_schema.flyway_schema_history USING btree (success);


--
-- Name: idx_chatbots_active; Type: INDEX; Schema: chat_schema; Owner: doran
--

CREATE INDEX idx_chatbots_active ON chat_schema.chatbots USING btree (is_active);


--
-- Name: idx_chatbots_created_by; Type: INDEX; Schema: chat_schema; Owner: doran
--

CREATE INDEX idx_chatbots_created_by ON chat_schema.chatbots USING btree (created_by);


--
-- Name: idx_chatbots_type; Type: INDEX; Schema: chat_schema; Owner: doran
--

CREATE INDEX idx_chatbots_type ON chat_schema.chatbots USING btree (bot_type);


--
-- Name: idx_chatrooms_chatbot; Type: INDEX; Schema: chat_schema; Owner: doran
--

CREATE INDEX idx_chatrooms_chatbot ON chat_schema.chatrooms USING btree (chatbot_id);


--
-- Name: idx_chatrooms_last_message; Type: INDEX; Schema: chat_schema; Owner: doran
--

CREATE INDEX idx_chatrooms_last_message ON chat_schema.chatrooms USING btree (last_message_at DESC);


--
-- Name: idx_chatrooms_user; Type: INDEX; Schema: chat_schema; Owner: doran
--

CREATE INDEX idx_chatrooms_user ON chat_schema.chatrooms USING btree (user_id);


--
-- Name: idx_chatrooms_user_chatbot; Type: INDEX; Schema: chat_schema; Owner: doran
--

CREATE UNIQUE INDEX idx_chatrooms_user_chatbot ON chat_schema.chatrooms USING btree (user_id, chatbot_id) WHERE (NOT is_deleted);


--
-- Name: idx_intimacy_progress_chatroom; Type: INDEX; Schema: chat_schema; Owner: doran
--

CREATE INDEX idx_intimacy_progress_chatroom ON chat_schema.intimacy_progress USING btree (chatroom_id);


--
-- Name: idx_intimacy_progress_user; Type: INDEX; Schema: chat_schema; Owner: doran
--

CREATE INDEX idx_intimacy_progress_user ON chat_schema.intimacy_progress USING btree (user_id);


--
-- Name: idx_messages_chatroom; Type: INDEX; Schema: chat_schema; Owner: doran
--

CREATE INDEX idx_messages_chatroom ON chat_schema.messages USING btree (chatroom_id, sequence_number);


--
-- Name: idx_messages_created_at; Type: INDEX; Schema: chat_schema; Owner: doran
--

CREATE INDEX idx_messages_created_at ON chat_schema.messages USING btree (created_at);


--
-- Name: idx_messages_room_turn; Type: INDEX; Schema: chat_schema; Owner: doran
--

CREATE INDEX idx_messages_room_turn ON chat_schema.messages USING btree (chatroom_id, turn_number);


--
-- Name: idx_messages_sender; Type: INDEX; Schema: chat_schema; Owner: doran
--

CREATE INDEX idx_messages_sender ON chat_schema.messages USING btree (sender_id);


--
-- Name: idx_ucli_user_last_ts; Type: INDEX; Schema: chat_schema; Owner: doran
--

CREATE INDEX idx_ucli_user_last_ts ON chat_schema.user_chatbot_last_interaction USING btree (user_id, last_interaction_at DESC);


--
-- Name: idx_store_ai_response_gin; Type: INDEX; Schema: store_schema; Owner: doran
--

CREATE INDEX idx_store_ai_response_gin ON store_schema.stores USING gin (ai_response);


--
-- Name: idx_store_chatroom; Type: INDEX; Schema: store_schema; Owner: doran
--

CREATE INDEX idx_store_chatroom ON store_schema.stores USING btree (chatroom_id, created_at DESC) WHERE (is_deleted = false);


--
-- Name: idx_store_user_created; Type: INDEX; Schema: store_schema; Owner: doran
--

CREATE INDEX idx_store_user_created ON store_schema.stores USING btree (user_id, created_at DESC) WHERE (is_deleted = false);


--
-- Name: idx_store_user_message; Type: INDEX; Schema: store_schema; Owner: doran
--

CREATE UNIQUE INDEX idx_store_user_message ON store_schema.stores USING btree (user_id, message_id) WHERE (is_deleted = false);


--
-- Name: app_user_email_idx; Type: INDEX; Schema: user_schema; Owner: doran
--

CREATE UNIQUE INDEX app_user_email_idx ON user_schema.app_user USING btree (email);


--
-- Name: idx_admin_audit_logs_action; Type: INDEX; Schema: user_schema; Owner: doran
--

CREATE INDEX idx_admin_audit_logs_action ON user_schema.admin_audit_logs USING btree (action_type);


--
-- Name: idx_admin_audit_logs_admin; Type: INDEX; Schema: user_schema; Owner: doran
--

CREATE INDEX idx_admin_audit_logs_admin ON user_schema.admin_audit_logs USING btree (admin_user_id);


--
-- Name: idx_admin_audit_logs_created; Type: INDEX; Schema: user_schema; Owner: doran
--

CREATE INDEX idx_admin_audit_logs_created ON user_schema.admin_audit_logs USING btree (created_at DESC);


--
-- Name: idx_admin_user_roles_role; Type: INDEX; Schema: user_schema; Owner: doran
--

CREATE INDEX idx_admin_user_roles_role ON user_schema.admin_user_roles USING btree (admin_role_id);


--
-- Name: idx_admin_user_roles_user; Type: INDEX; Schema: user_schema; Owner: doran
--

CREATE INDEX idx_admin_user_roles_user ON user_schema.admin_user_roles USING btree (admin_user_id);


--
-- Name: idx_admin_users_is_active; Type: INDEX; Schema: user_schema; Owner: doran
--

CREATE INDEX idx_admin_users_is_active ON user_schema.admin_users USING btree (is_active);


--
-- Name: idx_fcm_user; Type: INDEX; Schema: user_schema; Owner: doran
--

CREATE INDEX idx_fcm_user ON user_schema.fcm_tokens USING btree (user_id);


--
-- Name: idx_posts_cache_fetched; Type: INDEX; Schema: user_schema; Owner: doran
--

CREATE INDEX idx_posts_cache_fetched ON user_schema.posts_cache USING btree (fetched_at DESC);


--
-- Name: idx_prompt_active_env_agent; Type: INDEX; Schema: user_schema; Owner: doran
--

CREATE INDEX idx_prompt_active_env_agent ON user_schema.prompt_actives USING btree (env, agent_type);


--
-- Name: idx_prompt_agent_concept_level; Type: INDEX; Schema: user_schema; Owner: doran
--

CREATE INDEX idx_prompt_agent_concept_level ON user_schema.prompt_versions USING btree (agent_type, concept, intimacy_level, created_at DESC);


--
-- Name: idx_prompt_parent_version; Type: INDEX; Schema: user_schema; Owner: doran
--

CREATE INDEX idx_prompt_parent_version ON user_schema.prompt_versions USING btree (parent_version_id);


--
-- Name: idx_review_ticket_items_agent; Type: INDEX; Schema: user_schema; Owner: doran
--

CREATE INDEX idx_review_ticket_items_agent ON user_schema.review_ticket_items USING btree (agent_type);


--
-- Name: idx_review_ticket_items_message; Type: INDEX; Schema: user_schema; Owner: doran
--

CREATE INDEX idx_review_ticket_items_message ON user_schema.review_ticket_items USING btree (message_id);


--
-- Name: idx_review_ticket_items_ticket; Type: INDEX; Schema: user_schema; Owner: doran
--

CREATE INDEX idx_review_ticket_items_ticket ON user_schema.review_ticket_items USING btree (ticket_id);


--
-- Name: idx_review_tickets_agent; Type: INDEX; Schema: user_schema; Owner: doran
--

CREATE INDEX idx_review_tickets_agent ON user_schema.review_tickets USING btree (agent_type);


--
-- Name: idx_review_tickets_conversation; Type: INDEX; Schema: user_schema; Owner: doran
--

CREATE INDEX idx_review_tickets_conversation ON user_schema.review_tickets USING btree (conversation_id);


--
-- Name: idx_review_tickets_status; Type: INDEX; Schema: user_schema; Owner: doran
--

CREATE INDEX idx_review_tickets_status ON user_schema.review_tickets USING btree (status);


--
-- Name: idx_support_message_id; Type: INDEX; Schema: user_schema; Owner: doran
--

CREATE INDEX idx_support_message_id ON user_schema.support_requests USING btree (message_id);


--
-- Name: idx_support_type_created; Type: INDEX; Schema: user_schema; Owner: doran
--

CREATE INDEX idx_support_type_created ON user_schema.support_requests USING btree (type, created_at DESC);


--
-- Name: idx_support_user_created; Type: INDEX; Schema: user_schema; Owner: doran
--

CREATE INDEX idx_support_user_created ON user_schema.support_requests USING btree (user_id, created_at DESC);


--
-- Name: idx_user_interest_topic_user; Type: INDEX; Schema: user_schema; Owner: doran
--

CREATE INDEX idx_user_interest_topic_user ON user_schema.user_interest_topics USING btree (user_id);


--
-- Name: arch_agent_results fk_arch_agent_results_message; Type: FK CONSTRAINT; Schema: archive_schema; Owner: doran
--

ALTER TABLE ONLY archive_schema.arch_agent_results
    ADD CONSTRAINT fk_arch_agent_results_message FOREIGN KEY (arch_message_id) REFERENCES archive_schema.arch_messages(id) ON DELETE CASCADE;


--
-- Name: arch_intimacy_progress fk_arch_intimacy_progress_chatroom; Type: FK CONSTRAINT; Schema: archive_schema; Owner: doran
--

ALTER TABLE ONLY archive_schema.arch_intimacy_progress
    ADD CONSTRAINT fk_arch_intimacy_progress_chatroom FOREIGN KEY (arch_chatroom_id) REFERENCES archive_schema.arch_chatrooms(id) ON DELETE CASCADE;


--
-- Name: arch_messages fk_arch_messages_room; Type: FK CONSTRAINT; Schema: archive_schema; Owner: doran
--

ALTER TABLE ONLY archive_schema.arch_messages
    ADD CONSTRAINT fk_arch_messages_room FOREIGN KEY (arch_chatroom_id) REFERENCES archive_schema.arch_chatrooms(id) ON DELETE CASCADE;


--
-- Name: arch_stores fk_arch_stores_chatroom; Type: FK CONSTRAINT; Schema: archive_schema; Owner: doran
--

ALTER TABLE ONLY archive_schema.arch_stores
    ADD CONSTRAINT fk_arch_stores_chatroom FOREIGN KEY (arch_chatroom_id) REFERENCES archive_schema.arch_chatrooms(id) ON DELETE CASCADE;


--
-- Name: arch_usage_events fk_arch_usage_events_chatroom; Type: FK CONSTRAINT; Schema: archive_schema; Owner: doran
--

ALTER TABLE ONLY archive_schema.arch_usage_events
    ADD CONSTRAINT fk_arch_usage_events_chatroom FOREIGN KEY (arch_chatroom_id) REFERENCES archive_schema.arch_chatrooms(id) ON DELETE CASCADE;


--
-- Name: refresh_tokens fk5a9ypl7oycxycfscqnsepj5t8; Type: FK CONSTRAINT; Schema: auth_schema; Owner: doran
--

ALTER TABLE ONLY auth_schema.refresh_tokens
    ADD CONSTRAINT fk5a9ypl7oycxycfscqnsepj5t8 FOREIGN KEY (user_id) REFERENCES user_schema.app_user(id);


--
-- Name: email_verifications fk5vri8t8tr81le36apgppy94ch; Type: FK CONSTRAINT; Schema: auth_schema; Owner: doran
--

ALTER TABLE ONLY auth_schema.email_verifications
    ADD CONSTRAINT fk5vri8t8tr81le36apgppy94ch FOREIGN KEY (user_id) REFERENCES user_schema.app_user(id);


--
-- Name: login_attempts fkeix6yqtdy2p9t2vj8hs6ji8of; Type: FK CONSTRAINT; Schema: auth_schema; Owner: doran
--

ALTER TABLE ONLY auth_schema.login_attempts
    ADD CONSTRAINT fkeix6yqtdy2p9t2vj8hs6ji8of FOREIGN KEY (user_id) REFERENCES user_schema.app_user(id);


--
-- Name: password_reset_tokens fkj9so57i2ys7gbwiljqyrivrnb; Type: FK CONSTRAINT; Schema: auth_schema; Owner: doran
--

ALTER TABLE ONLY auth_schema.password_reset_tokens
    ADD CONSTRAINT fkj9so57i2ys7gbwiljqyrivrnb FOREIGN KEY (user_id) REFERENCES user_schema.app_user(id);


--
-- Name: auth_events fkq2wxphtsj555bl7w9yvcr08q8; Type: FK CONSTRAINT; Schema: auth_schema; Owner: doran
--

ALTER TABLE ONLY auth_schema.auth_events
    ADD CONSTRAINT fkq2wxphtsj555bl7w9yvcr08q8 FOREIGN KEY (user_id) REFERENCES user_schema.app_user(id);


--
-- Name: ai_usage_events fk_ai_usage_events_chatroom; Type: FK CONSTRAINT; Schema: billing; Owner: doran
--

ALTER TABLE ONLY billing.ai_usage_events
    ADD CONSTRAINT fk_ai_usage_events_chatroom FOREIGN KEY (chatroom_id) REFERENCES chat_schema.chatrooms(id) ON DELETE SET NULL;


--
-- Name: ai_usage_events fk_ai_usage_events_user; Type: FK CONSTRAINT; Schema: billing; Owner: doran
--

ALTER TABLE ONLY billing.ai_usage_events
    ADD CONSTRAINT fk_ai_usage_events_user FOREIGN KEY (user_id) REFERENCES user_schema.app_user(id) ON DELETE SET NULL;


--
-- Name: chatbots chatbots_created_by_fkey; Type: FK CONSTRAINT; Schema: chat_schema; Owner: doran
--

ALTER TABLE ONLY chat_schema.chatbots
    ADD CONSTRAINT chatbots_created_by_fkey FOREIGN KEY (created_by) REFERENCES user_schema.app_user(id) ON DELETE SET NULL;


--
-- Name: chatrooms chatrooms_chatbot_id_fkey; Type: FK CONSTRAINT; Schema: chat_schema; Owner: doran
--

ALTER TABLE ONLY chat_schema.chatrooms
    ADD CONSTRAINT chatrooms_chatbot_id_fkey FOREIGN KEY (chatbot_id) REFERENCES chat_schema.chatbots(id) ON DELETE CASCADE;


--
-- Name: chatrooms chatrooms_user_id_fkey; Type: FK CONSTRAINT; Schema: chat_schema; Owner: doran
--

ALTER TABLE ONLY chat_schema.chatrooms
    ADD CONSTRAINT chatrooms_user_id_fkey FOREIGN KEY (user_id) REFERENCES user_schema.app_user(id) ON DELETE CASCADE;


--
-- Name: chatrooms fk_chatrooms_last_message; Type: FK CONSTRAINT; Schema: chat_schema; Owner: doran
--

ALTER TABLE ONLY chat_schema.chatrooms
    ADD CONSTRAINT fk_chatrooms_last_message FOREIGN KEY (last_message_id) REFERENCES chat_schema.messages(id) ON DELETE SET NULL;


--
-- Name: intimacy_progress intimacy_progress_chatroom_id_fkey; Type: FK CONSTRAINT; Schema: chat_schema; Owner: doran
--

ALTER TABLE ONLY chat_schema.intimacy_progress
    ADD CONSTRAINT intimacy_progress_chatroom_id_fkey FOREIGN KEY (chatroom_id) REFERENCES chat_schema.chatrooms(id) ON DELETE CASCADE;


--
-- Name: intimacy_progress intimacy_progress_user_id_fkey; Type: FK CONSTRAINT; Schema: chat_schema; Owner: doran
--

ALTER TABLE ONLY chat_schema.intimacy_progress
    ADD CONSTRAINT intimacy_progress_user_id_fkey FOREIGN KEY (user_id) REFERENCES user_schema.app_user(id) ON DELETE CASCADE;


--
-- Name: messages messages_chatroom_id_fkey; Type: FK CONSTRAINT; Schema: chat_schema; Owner: doran
--

ALTER TABLE ONLY chat_schema.messages
    ADD CONSTRAINT messages_chatroom_id_fkey FOREIGN KEY (chatroom_id) REFERENCES chat_schema.chatrooms(id) ON DELETE CASCADE;


--
-- Name: messages messages_parent_message_id_fkey; Type: FK CONSTRAINT; Schema: chat_schema; Owner: doran
--

ALTER TABLE ONLY chat_schema.messages
    ADD CONSTRAINT messages_parent_message_id_fkey FOREIGN KEY (parent_message_id) REFERENCES chat_schema.messages(id) ON DELETE SET NULL;


--
-- Name: settings fk5w7p1w60kfsalo61akkmfirv3; Type: FK CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.settings
    ADD CONSTRAINT fk5w7p1w60kfsalo61akkmfirv3 FOREIGN KEY (user_id) REFERENCES user_schema.app_user(id);


--
-- Name: admin_user_roles fk_admin_user_roles_role; Type: FK CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.admin_user_roles
    ADD CONSTRAINT fk_admin_user_roles_role FOREIGN KEY (admin_role_id) REFERENCES user_schema.admin_roles(id) ON DELETE CASCADE;


--
-- Name: admin_user_roles fk_admin_user_roles_user; Type: FK CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.admin_user_roles
    ADD CONSTRAINT fk_admin_user_roles_user FOREIGN KEY (admin_user_id) REFERENCES user_schema.admin_users(id) ON DELETE CASCADE;


--
-- Name: review_ticket_items fk_review_ticket_items_ticket; Type: FK CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.review_ticket_items
    ADD CONSTRAINT fk_review_ticket_items_ticket FOREIGN KEY (ticket_id) REFERENCES user_schema.review_tickets(id) ON DELETE CASCADE;


--
-- Name: profiles fko9irkw5uae1s5s10pmstcvipw; Type: FK CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.profiles
    ADD CONSTRAINT fko9irkw5uae1s5s10pmstcvipw FOREIGN KEY (user_id) REFERENCES user_schema.app_user(id);


--
-- Name: prompt_actives prompt_actives_activated_by_fkey; Type: FK CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.prompt_actives
    ADD CONSTRAINT prompt_actives_activated_by_fkey FOREIGN KEY (activated_by) REFERENCES user_schema.app_user(id);


--
-- Name: prompt_actives prompt_actives_prompt_version_id_fkey; Type: FK CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.prompt_actives
    ADD CONSTRAINT prompt_actives_prompt_version_id_fkey FOREIGN KEY (prompt_version_id) REFERENCES user_schema.prompt_versions(id);


--
-- Name: prompt_versions prompt_versions_created_by_fkey; Type: FK CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.prompt_versions
    ADD CONSTRAINT prompt_versions_created_by_fkey FOREIGN KEY (created_by) REFERENCES user_schema.app_user(id);


--
-- Name: prompt_versions prompt_versions_parent_version_id_fkey; Type: FK CONSTRAINT; Schema: user_schema; Owner: doran
--

ALTER TABLE ONLY user_schema.prompt_versions
    ADD CONSTRAINT prompt_versions_parent_version_id_fkey FOREIGN KEY (parent_version_id) REFERENCES user_schema.prompt_versions(id);


--
-- Name: DEFAULT PRIVILEGES FOR SEQUENCES; Type: DEFAULT ACL; Schema: archive_schema; Owner: doran
--

ALTER DEFAULT PRIVILEGES FOR ROLE doran IN SCHEMA archive_schema GRANT ALL ON SEQUENCES TO doran;


--
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: archive_schema; Owner: doran
--

ALTER DEFAULT PRIVILEGES FOR ROLE doran IN SCHEMA archive_schema GRANT ALL ON TABLES TO doran;


--
-- PostgreSQL database dump complete
--

\unrestrict fuzQfIMtI2oiVWC0PR7riVsd5Hp2fA71uO8FPQeFMtrO23jVvRcVqyArq2r0Oad

