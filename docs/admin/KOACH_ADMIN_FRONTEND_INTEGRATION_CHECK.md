# Koach-Admin 프론트엔드 ↔ 백엔드 Admin API 연동 점검

점검일: 2025-02-05

## 1. 요약

| 구분 | 상태 | 비고 |
|------|------|------|
| 인증 | ✅ 연동됨 | 모든 Admin API 호출에 `Authorization: Bearer` 또는 axios `api` 사용 |
| 경로/메서드 | ✅ 일치 | 백엔드 엔드포인트와 프론트 호출 경로·메서드 일치 |
| 응답 형식 | ✅ 대부분 일치 | Admin 컨트롤러는 대부분 raw body 반환, axios/fetch가 그대로 사용 |
| 타입/필드명 | ✅ 수정 완료 | `admin.ts` ChatroomOption `userEmailSnapshot` → `userEmail` (백엔드와 동일) |
| 미연동 기능 | ⚠️ 2건 | 문의/신고(Support), IP 블랙리스트 — 백엔드만 있고 Koach-Admin UI 없음 |

---

## 2. 백엔드 Admin API vs Koach-Admin 연동 현황

### 2.1 User 서비스 (`/api/admin/*`)

| 백엔드 API | Koach-Admin 연동 | 프론트 모듈/페이지 |
|------------|------------------|---------------------|
| `GET/POST /api/admin/support`, `GET /api/admin/support/{id}` | ❌ 미연동 | API 클라이언트·페이지 없음 |
| `GET/POST/PUT/PATCH/DELETE /api/admin/management-queue` | ✅ | `api/admin/myManagementQueue.ts` → MyManagementQueuePage, MyAuditLogPage(감사 로그) |
| `GET /api/admin/management-queue/audit-logs` | ✅ | `myManagementQueue.getAuditLogs` → MyAuditLogPage |
| `GET /api/admin/management-queue/count` | ✅ | `myManagementQueue.getManagementQueueCount` |
| `GET /api/admin/chat-logs/chatrooms` | ✅ | `admin.getChatroomOptions`, `myChatLogs.getChatroomOptions` → ChatLogsUserHistoryPage, MyChatLogListPage |
| `GET /api/admin/chat-logs/intimacy-levels` | ✅ | `myChatLogs.getIntimacyLevelOptions` |
| `GET /api/admin/chat-logs/search` | ✅ | `myChatLogs.searchChatLogs` → MyChatLogListPage |
| `GET /api/admin/chat-logs/{chatroomId}/timeline` | ✅ | `myChatLogs.getChatLogTimeline` → MyChatLogDetailPage |
| `GET /api/admin/conversations`, `GET /api/admin/conversations/{id}` | ✅ | `conversations.ts` → ChatLogsUserHistoryPage |
| `GET/POST/PATCH /api/admin/prompts/*` | ✅ | `api/admin/prompts.ts` → PromptsTestAndApplyPage, PromptVersionsPage |
| `GET /api/admin/audit-logs` | ✅ | `api/admin/auditLogs.ts` → HistoryPage |
| `GET/POST/PATCH/DELETE /api/admin/review-tickets*` | ✅ | `api/admin/reviewTickets.ts` → ChatLogsManagementNeededPage |

### 2.2 Gateway (`/api/admin/*`)

| 백엔드 API | Koach-Admin 연동 | 비고 |
|------------|------------------|------|
| `GET/POST/DELETE /api/admin/blacklist`, `GET /api/admin/blacklist/{ip}` | ❌ 미연동 | API 클라이언트·페이지 없음 |

### 2.3 기타 (User 서비스 비 Admin)

| API | Koach-Admin 연동 | 비고 |
|-----|------------------|------|
| `GET /api/users/{userId}` | ✅ | `admin.getAdminUser` → AdminSidebar, useAdminUser |

---

## 3. 인증 방식 점검

- **로그인**: `api/admin/auth.ts` → `POST /api/auth/login` (publicApi), 성공 시 `user.role === 'ROLE_ADMIN'` 검사 후 세션 저장.
- **Admin API 호출**  
  - **axios `api` 사용**: `api.ts`의 `api` 인스턴스에 요청 인터셉터로 `Authorization: Bearer ${accessToken}` 자동 부착.  
    사용처: `myManagementQueue.ts`, `myChatLogs.ts`.
  - **fetch 사용**: `getAuthHeaders()`에서 `sessionStorage.getItem('accessToken')`으로 `Authorization: Bearer` 부착.  
    사용처: `reviewTickets.ts`, `prompts.ts`, `conversations.ts`, `auditLogs.ts`, `admin.ts`.
- Gateway에서 `/api/admin/*`는 JWT 검증 + ROLE_ADMIN 검증 후 통과하므로, 위 인증이 모두 적용되면 백엔드와 정상 연동됨.

---

## 4. 응답 형식

- **User 서비스 Admin 컨트롤러**: 대부분 `ResponseEntity.ok(body)`로 **ApiResponse 래퍼 없이** raw body 반환.  
  예: ManagementQueueController, ChatLogController, ReviewTicketController, AdminAuditLogController.
- **UserController `GET /api/users/{userId}`**: `ResponseEntity.ok(user)`로 UserDto 직접 반환.
- **프론트**  
  - axios: `response.data`가 위 raw body와 동일.  
  - fetch: `response.json()`이 위 raw body와 동일.  
  → 별도 `data` 언래핑 없이 사용 가능.

단, **AdminSupportController**만 `ApiResponse.success(response)` 사용.  
향후 Koach-Admin에 문의/신고 화면을 넣을 경우 응답에서 `data` 필드를 꺼내 써야 함.

---

## 5. 수정 사항 (점검 중 반영)

- **`Koach-Admin/src/api/admin.ts`**  
  - `ChatroomOption`의 `userEmailSnapshot`을 **`userEmail`**로 변경.  
  - 백엔드 `ChatroomOptionResponse`가 `userEmail`을 반환하므로 필드명 통일.

---

## 6. 미연동 기능 (선택 개선)

1. **문의/신고 (Support)**  
   - 백엔드: `AdminSupportController` — `GET /api/admin/support`, `GET /api/admin/support/{id}`.  
   - Koach-Admin: 해당 API를 호출하는 클라이언트와 페이지 없음.  
   - 필요 시: `api/admin/support.ts` 추가 후 문의/신고 목록·상세 페이지 연동.

2. **IP 블랙리스트**  
   - 백엔드: Gateway `BlacklistController` — `GET/POST/DELETE /api/admin/blacklist` 등.  
   - Koach-Admin: 블랙리스트 관리 UI 없음.  
   - 필요 시: `api/admin/blacklist.ts` 및 설정 페이지 추가.

---

## 7. 라우트 ↔ API 매핑

| 라우트 | 페이지 | 주요 API |
|--------|--------|-----------|
| `/login` | AdminLoginPage | `POST /api/auth/login` (adminLogin) |
| `/` | AdminPage | (대시보드, getAdminUser 등) |
| `/chat-logs/user-history` | ChatLogsUserHistoryPage | getChatroomOptions, getAdminConversations, getAdminConversationDetail |
| `/chat-logs/management-needed` | ChatLogsManagementNeededPage | reviewTickets (목록/카운트/완료 등) |
| `/prompts/test-and-apply` | PromptsTestAndApplyPage | prompts (active, test, activate, options 등), reviewTickets |
| `/prompts/versions` | PromptVersionsPage | prompts (versions, file-content 등) |
| `/history` | HistoryPage | auditLogs.getAuditLogs |
| `/my/chat-logs` | MyChatLogListPage | myChatLogs (chatrooms, search) |
| `/my/chat-logs/:chatroomId` | MyChatLogDetailPage | myChatLogs.getChatLogTimeline, myManagementQueue.create |
| `/my/management-queue` | MyManagementQueuePage | myManagementQueue (목록/등록/완료/배치 등) |
| `/my/audit-logs` | MyAuditLogPage | myManagementQueue.getAuditLogs (management-queue 감사 로그) |

---

## 8. 결론

- 구현된 Admin 기능 대부분은 **Koach-Admin 프론트와 잘 연동**되어 있음.
- 인증( Bearer + ROLE_ADMIN ) 및 경로/메서드/응답 형식이 맞고, ChatroomOption 필드명 불일치만 수정 완료.
- **문의/신고(Support)**와 **IP 블랙리스트**는 백엔드만 있고 Koach-Admin UI는 없음. 필요 시 위와 같이 API 클라이언트와 페이지를 추가하면 됨.
