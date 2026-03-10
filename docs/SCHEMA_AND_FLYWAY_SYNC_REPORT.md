# 스키마 및 Flyway 동기화 보고서

> 2026-03-03 작성 · 서버 스키마 덤프 기반

---

## 1. 서버 스키마 덤프 갱신

- **명령**: `ssh -i $KEY ec2-user@3.21.177.186 "docker exec dorandoran-shared-db pg_dump -U doran -d dorandoran --schema-only --no-owner --no-privileges"`
- **저장**: `docs/schema_dump_server.sql` (덮어쓰기 완료)

---

## 2. 서버 Flyway 이력 (2026-03-03 기준)

| 스키마 | flyway_schema_history | 비고 |
|--------|------------------------|------|
| auth_schema | 있음 | baseline만 적용됨 (`<< Flyway Baseline >>`) |
| chat_schema | 있음 | baseline만 적용됨 |
| user_schema | 없음 | Flyway 미사용 또는 별도 설정으로 보임 |

- **user_schema**: `flyway_schema_history`가 없음. user-service 배포 시 `baseline-on-migrate: true`로 첫 실행 시 baseline 생성 후 V1~V11 적용 가능.

---

## 3. Flyway 마이그레이션 목록 (현재 코드 기준)

### User 서비스 (`user_schema`)
| 버전 | 스크립트 | 내용 |
|------|----------|------|
| V1 | add_prompt_tables | prompt_versions, prompt_actives |
| V2 | sync_user_schema_columns | app_user, profiles, settings 컬럼/제약 동기화 |
| V3 | add_support_and_preferences | support_requests, interest_topics, user_interest_topics, user_notification_settings, user_stats, fcm_tokens, posts_cache |
| V4 | add_admin_user_role_tables | admin_users, admin_roles, admin_user_roles |
| V5 | add_admin_review_audit_tables | review_tickets, review_ticket_items, admin_audit_logs |
| V6 | update_user_deletion_fk_constraints | auth/user FK ON DELETE 변경 |
| V7 | add_onboarding_survey_table | onboarding_survey |
| V8 | seed_interest_topics | interest_topics 초기 데이터 |
| V9 | add_posts_cache_media_fields | posts_cache media_type, cover_image_url, assets |
| V10 | drop_prompt_user_fk | prompt_actives, prompt_versions FK 제거 |
| V11 | add_apple_to_oauth_provider_check | app_user oauth_provider CHECK에 APPLE 추가 |

### Chat 서비스 (`chat_schema`)
| 버전 | 스크립트 |
|------|----------|
| V1 | baseline |
| V2~V8 | chatrooms, messages, indexes 등 |

### Auth 서비스 (`auth_schema`)
| 버전 | 스크립트 |
|------|----------|
| V1 | sync_auth_schema |

### Batch 서비스 (`archive_schema`, `batch_schema`)
| 버전 | 스크립트 |
|------|----------|
| V1 | create_archive_schema |

---

## 4. 서버 스키마 구조 요약

- **스키마**: archive_schema, auth_schema, batch_schema, billing, chat_schema, store_schema, user_schema
- **user_schema 테이블**: admin_audit_logs, admin_roles, admin_user_roles, admin_users, app_user, fcm_tokens, interest_topics, onboarding_survey, posts_cache, profiles, prompt_actives, prompt_versions, push_delivery_logs, review_ticket_items, review_tickets, settings, support_requests, user_interest_topics, user_notification_settings, user_stats

---

## 5. 권장 사항

1. **새 환경 배포 시**: `init-shared-db.sql` → 각 서비스 Flyway 순서대로 실행
2. **기존 서버 배포 시**: user-service는 `baseline-on-migrate`로 자동 baseline 후 V1~V11 적용. 대부분 `IF NOT EXISTS`/`IF EXISTS` 사용으로 중복 실행 시에도 오류 없음.
3. **스키마 덤프 갱신**: 배포·마이그레이션 후 위 pg_dump 명령으로 `docs/schema_dump_server.sql`을 주기적으로 갱신 권장.
