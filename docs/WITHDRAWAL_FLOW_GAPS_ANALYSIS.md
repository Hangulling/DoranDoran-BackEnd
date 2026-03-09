# 탈퇴 플로우 (igu worktree / feature/admin_and_app)

**대상**: `DELETE /api/users/{userId}/hard` → `UserService.hardDeleteUser(id)`  
**d: worktree 완성 플로우와 동일하게 이식됨.**

---

## 1. 탈퇴 플로우 요약

- **소프트 삭제**: `DELETE /api/users/{userId}` → `deleteUser()` → status를 INACTIVE로 변경만 수행.
- **완전 삭제**: `DELETE /api/users/{userId}/hard` → `hardDeleteUser()` → 물리적 DELETE.

**완전 삭제 처리 순서 (d: worktree와 동일)**:

| 단계 | 담당 | 동작 |
|------|------|------|
| 1 | User | 사용자 존재 확인 |
| 2 | User → Auth | `authServiceIntegration.invalidateTokensForUser(userId)` (토큰 즉시 무효화) |
| 3 | User | `userWithdrawalArchiveService.archiveUserData(id)` (채팅방·메시지 등 archive_schema 이관) |
| 4 | User | user_stats 삭제, `userWithdrawalArchiveService.deleteUserChatbotLastInteraction(id)` |
| 5 | User | `userRepository.delete(user)` → CASCADE로 chatrooms, messages, intimacy_progress, onboarding_survey 등 정리 |

**prompt_actives / prompt_versions**: V10 마이그레이션에서 **FK 제거**. app_user 삭제 시 해당 행은 삭제하지 않고, activated_by/created_by는 고아 ID로 이력 보존.

---

## 2. user_chatbot_last_interaction 역할

- **역할**: `chat_schema.user_chatbot_last_interaction`은 **사용자별·챗봇별 “마지막 상호작용” 시각과 방 ID**를 저장.
- **용도**: 사용자가 메시지를 보낼 때마다 Chat 서비스에서 upsert하여, “최근에 이 챗봇과 대화한 시각/방”을 유지. 채팅방 목록 최신순 정렬 등에 사용.
- **구조**: (user_id, chatbot_id) PK, last_interaction_at, last_room_id 등.
- **탈퇴 시**: 삭제하지 않으면 app_user 삭제 후 고아 레코드가 남음. **hardDeleteUser에서 선행 삭제 반영됨.**

---

## 3. V6 / V10 마이그레이션 요약

### V6 (기존)

- auth_schema: refresh_tokens, email_verifications, login_attempts, password_reset_tokens → **ON DELETE CASCADE**
- auth_schema.auth_events → **ON DELETE SET NULL**
- user_schema.profiles, settings → **ON DELETE CASCADE**

### V10 (추가)

- `prompt_actives_activated_by_fkey` 제거 → activated_by는 고아 ID로 보존.
- `prompt_versions_created_by_fkey` 제거 → created_by는 고아 ID로 보존.

---

## 4. 반영된 완전 삭제 순서 (hardDeleteUser)

1. 사용자 존재 확인.
2. (선택) 아카이브 — 현재 미구현.
3. **user_stats** 삭제: `userStatsRepository.findById(id).ifPresent(userStatsRepository::delete)`.
4. **user_chatbot_last_interaction** 삭제: `DELETE FROM chat_schema.user_chatbot_last_interaction WHERE user_id = ?`.
5. **userRepository.deleteById(id)**  
   → CASCADE로 chatrooms, messages, intimacy_progress, onboarding_survey 등 정리.

---

## 5. 요약 표

| 구분 | 내용 |
|------|------|
| Auth 스키마 | ✅ V6 CASCADE/SET NULL로 자동 처리. |
| user_stats | ✅ hardDeleteUser에서 선행 삭제 반영. |
| prompt_actives / prompt_versions | ✅ V10에서 FK 제거, 고아 ID로 이력 보존. |
| user_chatbot_last_interaction | ✅ hardDeleteUser에서 선행 삭제 반영. |
| 채팅방 아카이브 | 🟡 미구현, 추후 구현 시 플로우에 추가. |

이 문서는 igu worktree의 `UserService.hardDeleteUser()`, V6, V10 마이그레이션을 기준으로 작성되었습니다.
