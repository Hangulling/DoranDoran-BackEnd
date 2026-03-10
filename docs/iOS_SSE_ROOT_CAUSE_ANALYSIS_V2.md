# iOS SSE 연결 실패 원인 분석 (V2) - 로그 기반

> **이전 시도**: EventSourcePolyfill 적용을 시도했으나 동일한 오류가 발생한 것으로 보고됨.  
> 본 문서에는 기존 방안(A~C)과 함께 추가 대안(D~I)을 정리함.

---

## 0. 최신 로그 분석 (2026-02-22 갱신)

### 0.1 stream/ 요청 분포

| 시간 | 클라이언트 | origin | 경로 | 결과 |
|------|-----------|--------|------|------|
| 17:40:14 | **Android** (SM-S931N) | `https://localhost` | `/api/chat/stream/...` | ✅ Gateway 도달 |
| 21:59:44 | **iPhone** (iOS 18.7) | `capacitor://localhost` | `/api/chat/chatrooms` (POST) | ✅ 성공 |
| 21:59:45 | **iPhone** | `capacitor://localhost` | `/api/chat/chatrooms/.../messages` | ✅ 성공 |
| 22:01:47 | **iPhone** | `capacitor://localhost` | `/api/chat/chatrooms` (POST) | ✅ 성공 |
| 22:01:48 | **iPhone** | `capacitor://localhost` | `/api/chat/chatrooms/.../messages` | ✅ 성공 |

**핵심: 24시간 내 `/api/chat/stream/` 요청은 Android 1건뿐. iOS에서는 0건.**

- iOS: chatrooms, messages 등 일반 API는 정상 도달
- iOS: **stream/ 요청만 절대 Gateway에 기록되지 않음** → 클라이언트에서 즉시 "Load failed" 발생
- 에러 객체의 `_listeners` (intimacy_analysis, vocabulary_extracted 등)는 fetch-event-source 내부 구조

### 0.2 결론: Polyfill로 해결 불가

- **fetch-event-source** (fetch + ReadableStream): iOS에서 Load failed
- **EventSourcePolyfill** (XHR 기반) 시도: 동일한 오류
- **원인**: iOS WKWebView/WebKit이 스트리밍 응답(text/event-stream, chunked)을 fetch/XHR 양쪽에서 제대로 처리하지 못하는 것으로 추정
- **SSE/EventSource 방식은 iOS 앱에서는 사실상 사용 불가** → WebSocket 또는 폴링으로 대체 필요

### 0.3 iOS에서 답이 없는가?

**아니요.** SSE 방식만 iOS WKWebView에서 불가능한 것이고, 아래 방식으로는 해결 가능합니다.

| 방안 | iOS 지원 | 난이도 | 비고 |
|------|----------|--------|------|
| **WebSocket** | ✅ | 중 | 기존 `/ws/chat/` 활용, 이벤트 푸시 확장 |
| **폴링(Polling)** | ✅ | 하 | 실시간성 감소, 구현 간단 |
| **SSE** | ❌ | - | fetch/XHR/polyfill 모두 동일 실패 |

**권장**: 방안 D(WebSocket 마이그레이션) 또는 방안 C(폴링 폴백) 적용

---

## 1. 로그 분석 결과 요약 (이전 분석)

### 1.1 Gateway 로그 분석

| 시간 | 클라이언트 | origin | 경로 | 결과 |
|------|-----------|--------|------|------|
| 04:11:42 | **iPhone** (iOS 18.7) | `capacitor://localhost` | `/api/chat/chatrooms/last-interactions` | ✅ 성공 |
| 04:11:44 | **iPhone** | `capacitor://localhost` | `/api/chat/chatrooms` (POST) | ✅ 성공 |
| 04:11:45 | **iPhone** | `capacitor://localhost` | `/api/chat/chatrooms/.../messages` | ✅ 성공 |
| 05:40:44 | **Android** | `https://localhost` | `/api/chat/chatrooms/last-interactions` | ✅ 성공 |

**일관된 발견: `/api/chat/stream/{chatroomId}` (SSE) 요청이 iOS에서는 Gateway 로그에 전혀 존재하지 않음.**

- 일반 API(chatrooms, messages, last-interactions)는 iOS에서 정상적으로 Gateway에 도달
- **SSE 스트림 요청만 서버에 도달하지 않고 클라이언트에서 즉시 실패**

### 1.2 Chat 서비스 로그

- PushNotificationTextService, OpenAI 호출 등 일반 로그만 존재
- SSE 연결 관련 로그 없음 (SSE 요청이 Chat에 도달하지 않음)

---

## 2. iosScheme 변경이 해결되지 않은 이유

- **CORS는 원인이 아님**: `capacitor://localhost` origin으로 다른 API들이 정상 동작함
- **서버 측 문제 아님**: SSE 요청이 서버에 도달하지 않음 → Gateway/Chat에서 처리할 요청이 없음
- **클라이언트 측에서 fetch 호출 직후 실패** → "Load failed" 발생

---

## 3. 근본 원인: iOS WKWebView + fetch-event-source

### 3.1 현재 구현

- `@microsoft/fetch-event-source` 사용
- 내부적으로 **fetch()** + **ReadableStream**으로 스트리밍 응답 처리

### 3.2 iOS에서의 제약

1. **fetch() + 스트리밍 응답(ReadableStream)**  
   - iOS Safari/WKWebView는 스트리밍 응답 처리에 제약이 있음  
   - `text/event-stream` + chunked 응답 시 fetch가 "Load failed"로 실패하는 사례 존재

2. **WebKit 이슈**  
   - Bug 138968: Fetch API로 HTTP 데이터를 ReadableStream으로 consume  
   - Bug 203617: ReadableStream 관련 에러  
   - 일부 버전/상황에서 스트리밍 응답 처리 시 `NotReadableError` 또는 유사 오류 발생

3. **동작 차이**  
   - 일반 API: 짧은 JSON 응답 → 정상 동작  
   - SSE: 오래 유지되는 chunked 스트림 → iOS에서 fetch 기반 처리 시 실패

---

## 4. 해결 방안

> **참고**: EventSourcePolyfill은 이미 시도했으나 동일한 문제가 발생한 것으로 알려져 있음. 아래 대안들을 함께 검토할 것.

### 방안 A: iOS에서 EventSourcePolyfill 사용

**아이디어**: iOS 감지 시 `fetch-event-source` 대신 `event-source-polyfill` 사용

- `event-source-polyfill`: **XMLHttpRequest** 기반 → ReadableStream 미사용
- `EventSource`/polyfill은 iOS에서 상대적으로 안정적으로 동작한다는 보고가 있으나, **이미 시도했을 때 해결되지 않았을 수 있음**
- 이미 `event-source-polyfill` 패키지가 `package.json`에 있음

### 방안 B: Capacitor HTTP 플러그인

- `@capacitor/core`의 `CapacitorHttp`로 네이티브 HTTP 요청
- **한계**: Capacitor 공식 이슈(#6582)에 따르면 CapacitorHttp는 `text/event-stream` 스트리밍 응답을 지원하지 않음. 아키텍처 전면 개편이 필요하다는 의견이 있음

### 방안 C: 폴링(Polling) 폴백

- SSE 실패 시 일정 간격으로 REST API 폴링
- 실시간성은 떨어지지만, 동작은 확보 가능
- 예: `/api/chat/chatrooms/{id}/messages` 주기적 조회 또는 신규 이벤트 전용 폴링 엔드포인트 추가

---

### 방안 D: WebSocket으로 마이그레이션 (서버·클라이언트 수정)

**아이디어**: SSE 대신 WebSocket으로 실시간 이벤트 수신

- **현재 상태**: Chat 서비스에 `/ws/chat/{chatroomId}` WebSocket 핸들러가 이미 존재 (메시지 전송용)
- **추가 작업**: 서버에서 AI 응답 이벤트(intimacy_analysis, vocabulary_extracted 등)를 WebSocket으로도 푸시하도록 SSEManager와 연동
- **장점**: WebSocket은 iOS WKWebView에서 안정적으로 동작. SSE/fetch 스트리밍 이슈 우회
- **단점**: 서버 구조 변경 필요, 양방향 프로토콜이 필요한 만큼 리소스 사용 증가 가능

### 방안 E: capacitor-eventsource 네이티브 플러그인 (아카이브)

- **저장소**: https://github.com/RangerRick/capacitor-eventsource
- **상태**: 2024년 4월 아카이브됨 (read-only)
- **특징**: iOS/Android 네이티브 EventSource 구현으로 WebView 제약 우회
- **고려사항**: 유지보수 중단, Capacitor 최신 버전과 호환성 확인 필요. fork 후 필요한 수정 가능

### 방안 F: 커스텀 Capacitor 플러그인 (네이티브 iOS 개발)

- **아이디어**: Swift 네이티브 EventSource 라이브러리(예: CocoaPods `EventSource`)를 사용하는 Capacitor 플러그인 제작
- **장점**: 네이티브 구현으로 WKWebView/WebKit 제약 완전 우회
- **단점**: Swift/Objective-C 개발 역량 필요, 플러그인 개발·유지보수 부담

### 방안 G: Web Worker를 통한 fetch 우회 (실험적)

- **출처**: Capacitor 이슈 #6582 코멘트 (2026-01)
- **아이디어**: Web Worker 내부에서 fetch 실행 후, 스트림 청크를 메인 스레드로 postMessage로 전달
- **고려사항**: iOS WKWebView에서 Web Worker 지원은 Service Worker보다 나은 편이지만, `capacitor://` 또는 `https://localhost` 환경에서 동작 검증 필요. Worker 등록 실패 가능성 있음

### 방안 H: App-Bound Domains + PWA/원격 웹뷰

- **아이디어**: Chat 화면만 `https://doran-chat.com` 등 원격 URL에서 로드하고, App-Bound Domains로 iOS에 등록
- **특징**: 실제 웹 환경과 동일한 컨텍스트에서 실행 → Service Worker, fetch 스트리밍 등이 더 잘 동작할 수 있음
- **단점**: 앱 구조 변경, 오프라인 지원 제한, 인증/세션 연동 복잡도 증가

### 방안 I: 서드파티 실시간 서비스 (Mercure, Ably, Centrifugo)

- **Mercure**: HTTP/SSE 기반 퍼블리시-서브스크라이브. JWT 인증, HTTP/2 지원
- **Ably**: SSE 포함 다양한 프로토콜 지원
- **Centrifugo**: WebSocket, SSE, HTTP-streaming 등 다중 전송 방식 지원
- **장점**: 전송 계층을 외부 서비스에 위임하여 플랫폼별 이슈 회피
- **단점**: 인프라 추가, 비용, 기존 백엔드 연동 작업 필요

---

## 5. 권장 조치 순서

1. **방안 D(WebSocket 마이그레이션)** 검토: 기존 WebSocket 인프라 활용, 서버 변경 범위 파악
2. **방안 C(폴링 폴백)** 적용: 빠른 대체 수단으로 iOS에서 채팅 기능 동작 보장
3. **방안 E(capacitor-eventsource)** 실험: 아카이브 플러그인 fork 후 현재 프로젝트에 적용 가능 여부 확인
4. 장기적으로 **방안 F(커스텀 플러그인)** 또는 **방안 I(서드파티 서비스)** 검토

---

## 6. 참고

- `event-source-polyfill` 사용 예:
  ```ts
  import { EventSourcePolyfill } from 'event-source-polyfill'
  const es = new EventSourcePolyfill(url, {
    headers: { Authorization: `Bearer ${token}` },
    heartbeatTimeout: 3600000,
  })
  ```
- Capacitor SSE 이슈: https://github.com/ionic-team/capacitor/issues/6582 (2026년 1월 기준 오픈)
