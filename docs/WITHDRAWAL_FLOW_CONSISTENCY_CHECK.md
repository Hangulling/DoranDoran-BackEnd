# 탈퇴 플로우 정합성 체크

**대상**: igu worktree, `DELETE /api/users/{userId}/hard` → `UserService.hardDeleteUser`  
**기준**: d: worktree 완성 플로우 및 DB/API 일관성

---

## 1. 플로우 순서 정합성

| 순서 | 단계 | igu 구현 위치 | 정합성 |
|------|------|----------------|--------|
| 1 | 사용자 존재 확인 | `UserService.hardDeleteUser` findById | ✅ |
| 2 | Auth 토큰 무효화 | `authServiceIntegration.invalidateTokensForUser(id)` | ✅ |
| 3 | 아카이브 (채팅 등) | `userWithdrawalArchiveService.archiveUserData(id)` | ✅ |
| 4 | user_stats 삭제 | `userStatsRepository.findById(id).ifPresent(delete)` | ✅ |
| 5 | user_chatbot_last_interaction 삭제 | `userWithdrawalArchiveService.deleteUserChatbotLastInteraction(id)` | ✅ |
| 6 | app_user 삭제 | `userRepository.delete(user)` | ✅ |

→ **d: worktree와 동일한 순서로 일치.**

---

## 2. 의존성 및 API 정합성

| 항목 | igu | 비고 |
|------|-----|------|
| UserService → UserWithdrawalArchiveService | ✅ 주입됨 | archiveUserData, deleteUserChatbotLastInteraction 사용 |
| UserService → UserStatsRepository | ✅ 주입됨 | 선행 삭제 |
| UserService → AuthServiceIntegration | ✅ 주입됨 | invalidateTokensForUser |
| AuthServiceIntegration → ChatServiceRequestSigner | ✅ 주입됨 | HMAC 헤더로 Auth 내부 API 호출 |
| Auth API `POST /api/auth/internal/users/{userId}/invalidate-tokens` | ✅ **추가됨** | igu AuthController에 엔드포인트 반영 |

---

## 3. 보안 정합성

| 항목 | igu | 비고 |
|------|-----|------|
| 완전 삭제 시 본인만 호출 가능 | ✅ **추가됨** | `X-User-Id` 헤더와 path `userId` 일치 시에만 200, 아니면 403 |
| Auth 내부 API | HMAC으로 호출 | requestSigner.createHeaders("user-service") |

---

## 4. DB/스키마 정합성

| 항목 | igu | 비고 |
|------|-----|------|
| V6 마이그레이션 | ✅ 적용 | auth_schema(refresh_tokens 등) CASCADE, auth_events SET NULL, profiles/settings CASCADE |
| V10 마이그레이션 | ✅ 적용 | prompt_actives/prompt_versions FK 제거(고아 ID 보존) |
| user_stats | 코드에서 선행 삭제 | FK 없이 삭제 시 delete(user) 실패 방지 |
| user_chatbot_last_interaction | 코드에서 선행 삭제 | 고아 레코드 방지 |
| archive_schema.* | UserWithdrawalArchiveService에서 사용 | arch_chatrooms, arch_messages, arch_stores, arch_usage_events, arch_intimacy_progress, arch_agent_results |
| store_schema.stores | 아카이브 시 조회 | 동일 DB에 스키마 존재 시 정상 동작 |
| billing.ai_usage_events | 아카이브 시 조회 | 동일 DB에 스키마 존재 시 정상 동작 |

**참고**: archive/ store/ billing 스키마가 DB에 없으면 해당 단계에서 예외 → 서비스 내부에서 catch 후 로그만 남기고 탈퇴는 계속 진행.

---

## 5. 트랜잭션·예외 정합성

| 항목 | 동작 | 정합성 |
|------|------|--------|
| hardDeleteUser | @Transactional | ✅ 단일 트랜잭션 |
| archiveUserData 예외 | catch 후 log.warn, 탈퇴 계속 | ✅ |
| deleteUserChatbotLastInteraction 예외 | catch 후 log.warn, 탈퇴 계속 | ✅ |
| invalidateTokensForUser 실패 | catch 후 log.warn, 탈퇴 계속 | ✅ |
| user not found | DoranDoranException(USER_NOT_FOUND) | ✅ 컨트롤러에서 500 (필요 시 404 분기 가능) |

---

## 6. 수정 반영 사항 (체크 시 적용)

1. **igu Auth**  
   - `AuthController`에 `POST /api/auth/internal/users/{userId}/invalidate-tokens` 추가  
   - User 서비스에서 토큰 무효화 호출 시 404가 나지 않도록 정합성 맞춤.

2. **igu User Controller**  
   - `hardDeleteUser`에 `X-User-Id` 헤더 검증 추가  
   - path `userId`와 일치할 때만 완전 삭제 수행, 아니면 403.

---

## 7. 로그 정합성

| 위치 | 로그 내용 | 레벨 |
|------|-----------|------|
| UserController.hardDeleteUser | 요청/권한거부(403)/성공/실패(400·500) | info, warn, error |
| UserService.hardDeleteUser | 시작, 대상 사용자, 1/4~4/4 단계별 시작·완료, user_stats 유무, 최종 완료 | info, debug, warn |
| AuthServiceIntegration.invalidateTokensForUser | 호출(debug), 성공/비정상응답/실패(warn) | debug, info, warn |
| AuthController.invalidateTokensForUser | 요청 수신, 완료, 실패 | info, error |
| AuthService.invalidateAllTokensForUser | 완료/실패 | info, warn |
| UserWithdrawalArchiveService.archiveUserData | 아카이브 시작·채팅방 수·완료·예외 | info, warn |
| UserWithdrawalArchiveService.deleteUserChatbotLastInteraction | 삭제 건수 또는 없음(스킵), 예외 | info, debug, warn |
| UserWithdrawalArchiveService (채팅방/메시지 등) | 채팅방별 이관 시작·완료·스킵·예외 | info, warn |

- 공통 접두어: `[탈퇴]` 또는 `[아카이브]`로 탈퇴 플로우 추적 가능.
- 예외 시: 아카이브/ucli/토큰 무효화는 log.warn 후 탈퇴 계속 진행.

---

## 8. 요약

| 구분 | 결과 |
|------|------|
| 플로우 순서 | ✅ d:와 동일 |
| Auth 내부 API | ✅ igu에 엔드포인트 추가됨 |
| 본인만 완전 삭제 | ✅ X-User-Id 검증 추가됨 |
| FK/선행 삭제 | ✅ user_stats, user_chatbot_last_interaction 처리됨 |
| 마이그레이션 | ✅ V6, V10 반영 |
| 예외 처리 | ✅ 아카이브/ucli/토큰 무효화 실패 시에도 탈퇴 계속 진행 |

이 문서는 정합성 체크, 수정 반영, 로그 보강 후 기준으로 작성되었습니다.
