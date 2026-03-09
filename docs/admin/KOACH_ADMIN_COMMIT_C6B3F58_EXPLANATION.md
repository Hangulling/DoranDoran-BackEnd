# Koach-Admin 커밋 c6b3f58 수정 내용 설명

- **커밋**: [c6b3f58](https://github.com/Hangulling/Koach-Admin/commit/c6b3f5891b8722b092eed526615a7263bf8364a5)
- **메시지**: `fix(admin): use axios api for prompts/reviewTickets (token+refresh), auth:expired redirect, 404 file-content`
- **변경 파일**: `src/App.tsx`, `src/api/admin/prompts.ts`, `src/api/admin/reviewTickets.ts`

---

## 1. 수정 배경 (요약)

프로덕션(api.doran-chat.com)에서 Admin 페이지 진입 시 `/api/admin/prompts/options/agent-types`, `/api/admin/review-tickets` 등이 **401 Unauthorized**를 반환하며 "AgentType 옵션 조회 실패", "티켓 목록 조회 실패"가 발생했습니다.

- **원인**: Admin API를 `fetch` + `getAuthHeaders()`로만 호출해, 토큰 만료 시 **리프레시(갱신) 로직이 없었음**.  
  axios `api` 인스턴스에는 401 시 리프레시 후 재시도 + 실패 시 `auth:expired` 이벤트 발생 로직이 있으나, Admin 모듈은 fetch를 사용해 해당 흐름이 적용되지 않았음.
- **대응**: Admin API 호출을 **axios `api`로 통일**하고, **auth:expired 시 로그인 페이지로 리다이렉트**하는 리스너를 추가했습니다.  
  추가로 `getPromptFileContent`는 **404일 때 예외 대신 빈 문자열 반환**하도록 해, "파일 없음"을 에러가 아닌 정상 케이스로 처리했습니다.

---

## 2. `src/App.tsx` — 코드별 설명

| 수정 내용 | 이유 |
|-----------|------|
| `import { Suspense, useEffect } from 'react'` | `useEffect`로 전역 이벤트 리스너를 등록하기 위해 import 추가. |
| `useEffect` 안에 `onAuthExpired` 함수 정의 | `api.ts`의 axios 인터셉터가 401 후 리프레시 실패 시 `auth:expired` / `auth:inactive` 이벤트를 발생시킴. 이 이벤트가 발생했을 때 **세션 정리 + 로그인 페이지 이동**을 한 곳에서 처리하기 위함. |
| `sessionStorage.removeItem('adminUser')` | Admin 전용 라우트가 참조하는 `adminUser`(role 등)를 제거해, 다음 접근 시 AdminPrivateRoute가 로그인으로 보내도록 함. |
| `sessionStorage.removeItem('accessToken')`, `removeItem('refreshToken')` | 만료/무효가 된 토큰을 저장소에서 제거해, 다음 로그인 시 깨끗한 상태로 시작하도록 함. |
| `window.location.href = '/login'` | React Router가 아닌 **전체 페이지 이동**으로 로그인 페이지로 보냄. 세션 정리 후 확실히 로그인 화면만 보이게 함. |
| `window.addEventListener('auth:expired', onAuthExpired)` | 401 → 리프레시 실패 시 인터셉터가 발생시키는 `auth:expired`를 구독해 위 정리·리다이렉트 실행. |
| `window.addEventListener('auth:inactive', onAuthExpired)` | 네트워크/서버 문제 등으로 인한 비활성 상태도 동일하게 로그인으로 보냄. |
| `return () => { ... removeEventListener }` | 컴포넌트 언마운트 시 리스너 제거로 메모리 누수 및 중복 리다이렉트 방지. |

---

## 3. `src/api/admin/prompts.ts` — 코드별 설명

### 3.1 공통 변경

| 수정 내용 | 이유 |
|-----------|------|
| `const API_BASE_URL = ...` 제거, `import api from '../api'` 추가 | Admin API도 **axios `api` 인스턴스**를 사용하도록 통일. `api`는 요청 시 `Authorization` 자동 부착 + 401 시 리프레시 후 재시도 로직이 있어, fetch + 수동 헤더보다 토큰/만료 처리가 안정적임. |
| `const getAuthHeaders = () => { ... }` 제거 | 토큰 첨부는 `api` 인터셉터가 담당하므로 불필요. |
| `const BASE = '/api/admin/prompts'` 추가 | URL 중복 제거 및 `api`의 baseURL과 조합해 경로를 한 곳에서 관리. |

### 3.2 함수별 변경

| 함수 | 기존 (fetch) | 변경 (axios api) | 이유 |
|------|--------------|------------------|------|
| `getActivePrompt` | `fetch(..., getAuthHeaders())` → `response.json()` | `api.get<...>(...)` → `return data` | GET에도 토큰 자동 부착 + 401 시 리프레시/재시도 적용. |
| `testPrompt` | `fetch` POST + `getAuthHeaders()` | `api.post<...>(...)` → `return data` | 동일하게 토큰·리프레시 공통 처리. |
| `createPromptVersion` | `fetch` POST | `api.post<...>(...)` → `return data` | 동일. |
| `activatePrompt` | `fetch` POST 후 `if (!response.ok) throw` | `await api.post(...)` (반환값 없음) | 401이 나면 인터셉터가 갱신 시도 후 재요청하므로, 성공 시 별도 분기만 유지. |
| `rollbackPrompt` | `fetch` POST | `await api.post(...)` | activate와 동일. |
| `getPromptVersions` | `fetch` GET → `response.json()` | `api.get<...>(...)` → `return data` | 목록 조회도 토큰·리프레시 공통화. |
| `getPromptVersion` | `fetch` GET | `api.get<...>(...)` → `return data` | 상세 조회도 동일. |
| `getPromptFileContent` | `fetch` GET → `response.ok` 아니면 throw | `api.get` + **try/catch에서 `status === 404`면 `return ''`** | 해당 조합의 프롬프트 파일이 없을 때 서버가 404를 주므로, "파일 없음"은 에러가 아니라 빈 문자열로 처리해 UI가 깨지지 않게 함. |
| `getAgentTypeOptions` | `fetch` GET | `api.get<AgentTypeOption[]>(...)` → `return data` | 옵션 조회도 401 시 리프레시 후 재시도되도록 통일. |
| `getConceptOptions` | `fetch` GET | `api.get<ConceptOption[]>(...)` → `return data` | 동일. |
| `getIntimacyLevelOptions` | `fetch` GET | `api.get<IntimacyLevelOption[]>(...)` → `return data` | 동일. |
| `saveAndActivatePrompt` | `fetch` POST | `api.post<...>(...)` → `return data` | 저장·적용도 토큰·리프레시 공통 처리. |
| `getVersionActiveStatus` | `fetch` GET | `api.get<...>(...)` → `return data` | 활성 상태 조회도 동일. |

### 3.3 기타

| 수정 내용 | 이유 |
|-----------|------|
| `...(promptVersionId && { promptVersionId })` → `...(promptVersionId != null && { promptVersionId })` | `promptVersionId`가 `0`일 때도 옵션으로 넘기도록, "falsy"가 아닌 "null/undefined만 제외"하도록 변경. |

---

## 4. `src/api/admin/reviewTickets.ts` — 코드별 설명

| 수정 내용 | 이유 |
|-----------|------|
| `const API_BASE_URL = ...` 제거, `import api from '../api'` 추가 | prompts와 동일하게 **토큰 자동 부착 + 401 시 리프레시**를 쓰기 위해 `api` 사용. |
| `getAuthHeaders()` 제거, `const BASE = '/api/admin/review-tickets'` 추가 | 토큰은 `api`가 처리하고, 경로만 상수로 관리. |
| `getReviewTickets` | `fetch` GET → `api.get<ReviewTicketListResponse>(...)` → `return data`. 401 시 리프레시 후 재요청 가능. |
| `getReviewTicketCounts` | `fetch` GET → `api.get<...>(...)` → `return data`. 동일. |
| `updateReviewTicket` | `fetch` PATCH → `api.patch<...>(...)` → `return data`. 동일. |
| `deleteReviewTicket` | `fetch` DELETE 후 `if (!response.ok) throw` → `await api.delete(...)`. 실패 시 axios가 reject 하므로 별도 분기 제거. |
| `completeReviewTickets` | `fetch` POST → `await api.post(...)`. 토큰·리프레시 공통. |
| `createReviewTicket` | `fetch` POST → `api.post<...>(...)` → `return data`. 동일. |
| `getReviewTicketDetail` | `fetch` GET → `api.get<...>(...)` → `return data`. 동일. |

---

## 5. 요약

| 파일 | 목적 |
|------|------|
| **App.tsx** | 401 후 토큰 갱신 실패 시 `auth:expired` / `auth:inactive` 수신 → 세션 제거 후 `/login`으로 리다이렉트. |
| **prompts.ts** | 모든 Admin 프롬프트 API를 axios `api`로 통일해 토큰 자동 부착·401 시 리프레시 재시도 적용; `getPromptFileContent`는 404 시 빈 문자열 반환. |
| **reviewTickets.ts** | 모든 Admin 리뷰 티켓 API를 axios `api`로 통일해 토큰·리프레시 동일 적용. |

이 변경으로 "옵션 조회 실패", "티켓 목록 조회 실패" 등 401이 나던 상황을 줄이고, 세션이 완전히 무효화되었을 때는 로그인 페이지로 자동 이동하도록 했습니다.
