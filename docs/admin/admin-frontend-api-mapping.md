# Admin 프론트 요구사항/호출 API 매핑

**작성일**: 2026-01-17  
**기준**: `dorandoran-admin-frontend`

## 1. 인증/공통

- 로그인: `POST /api/auth/login`  
  - 파일: `dorandoran-admin-frontend/src/api/admin/auth.ts`
- 토큰 갱신: `POST /api/auth/refresh`  
  - 파일: `dorandoran-admin-frontend/src/api/api.ts`
- 관리자 정보 조회: `GET /api/users/{userId}`  
  - 파일: `dorandoran-admin-frontend/src/api/admin.ts`

---

## 2. 프롬프트 관리

### 2.1 프롬프트 테스트/적용 화면

- 화면: `PromptsTestAndApplyPage`
  - 파일: `dorandoran-admin-frontend/src/pages/admin/PromptsTestAndApplyPage.tsx`
- 사용 API:
  - `GET /api/admin/prompts/options/agent-types`
  - `GET /api/admin/prompts/options/concepts`
  - `GET /api/admin/prompts/options/intimacy-levels`
  - `GET /api/admin/prompts/active`
  - `GET /api/admin/prompts/file-content`
  - `POST /api/admin/prompts/test`
  - `POST /api/admin/prompts/versions`
  - `POST /api/admin/prompts/save-and-activate`
  - `POST /api/admin/review-tickets/complete`
  - `GET /api/admin/review-tickets`

### 2.2 프롬프트 버전 관리 화면

- 화면: `PromptVersionsPage`
  - 파일: `dorandoran-admin-frontend/src/pages/admin/PromptVersionsPage.tsx`
- 사용 API:
  - `GET /api/admin/prompts/options/agent-types`
  - `GET /api/admin/prompts/options/concepts`
  - `GET /api/admin/prompts/options/intimacy-levels`
  - `GET /api/admin/prompts/versions`
  - `GET /api/admin/prompts/versions/{versionId}`
  - `POST /api/admin/prompts/activate`
  - `POST /api/admin/prompts/rollback`

---

## 3. 관리 필요 내역

- 화면: `ChatLogsManagementNeededPage`
  - 파일: `dorandoran-admin-frontend/src/pages/admin/ChatLogsManagementNeededPage.tsx`
- 현재 상태: UI만 존재(구현 중)
- 필요 API(기획 기준):
  - `GET /api/admin/review-tickets`
  - `GET /api/admin/review-tickets/{ticketId}`
  - `POST /api/admin/review-tickets`
  - `PATCH /api/admin/review-tickets/{ticketId}`
  - `DELETE /api/admin/review-tickets/{ticketId}`
  - `POST /api/admin/review-tickets/complete`
  - `GET /api/admin/review-tickets/counts`

---

## 4. 사용자 채팅 내역

- 화면: `ChatLogsUserHistoryPage`
  - 파일: `dorandoran-admin-frontend/src/pages/admin/ChatLogsUserHistoryPage.tsx`
- 현재 상태: UI만 존재(구현 중)
- 필요 API(기획 기준):
  - `GET /api/admin/conversations`
  - `GET /api/admin/conversations/{conversationId}`

---

## 5. 관리 이력

- 화면: `HistoryPage`
  - 파일: `dorandoran-admin-frontend/src/pages/admin/HistoryPage.tsx`
- 현재 상태: UI만 존재(구현 중)
- 필요 API(기획 기준):
  - `GET /api/admin/audit-logs`
