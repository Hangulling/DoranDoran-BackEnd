-- Apple Sign In 지원: oauth_provider CHECK 제약조건에 APPLE 추가
-- 기존: GOOGLE, FACEBOOK, KAKAO, NAVER만 허용 → APPLE 추가

ALTER TABLE user_schema.app_user
    DROP CONSTRAINT IF EXISTS app_user_oauth_provider_check;

ALTER TABLE user_schema.app_user
    ADD CONSTRAINT app_user_oauth_provider_check CHECK (
        (oauth_provider)::text = ANY ((ARRAY[
            'GOOGLE'::character varying,
            'FACEBOOK'::character varying,
            'KAKAO'::character varying,
            'NAVER'::character varying,
            'APPLE'::character varying
        ])::text[])
    );
