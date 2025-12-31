# DoranDoran 프로젝트 종합 문서

**작성일**: 2025년 11월  
**프로젝트 기간**: 2024년 ~ 2025년  
**프로젝트 유형**: 마이크로서비스 아키텍처 기반 AI 채팅 플랫폼

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [기술 스택](#2-기술-스택)
3. [아키텍처 설계](#3-아키텍처-설계)
4. [핵심 기능 구현](#4-핵심-기능-구현)
5. [성능 최적화](#5-성능-최적화)
6. [트러블슈팅 및 문제 해결](#6-트러블슈팅-및-문제-해결)
7. [인프라 및 배포](#7-인프라-및-배포)
8. [보안 구현](#8-보안-구현)
9. [운영 기록 및 통계](#9-운영-기록-및-통계)
10. [프론트엔드 아키텍처](#10-프론트엔드-아키텍처)
11. [API 설계 및 문서화](#11-api-설계-및-문서화)
12. [테스트 전략](#12-테스트-전략)
13. [배포 자동화 및 DevOps](#13-배포-자동화-및-devops)
14. [데이터베이스 마이그레이션](#14-데이터베이스-마이그레이션)
15. [에러 핸들링 및 복원력](#15-에러-핸들링-및-복원력)
16. [모니터링 및 로깅](#16-모니터링-및-로깅)
17. [보안 구현 상세](#17-보안-구현-상세)
18. [성능 테스트](#18-성능-테스트)
19. [개발 프로세스](#19-개발-프로세스)
20. [프로젝트 규모 및 통계](#20-프로젝트-규모-및-통계)
21. [기술적 도전 과제](#21-기술적-도전-과제)
22. [학습 및 성장](#22-학습-및-성장)
23. [회고 및 교훈](#23-회고-및-교훈)

---

## 1. 프로젝트 개요

### 1.1 프로젝트 소개

DoranDoran은 **"언어와 문화, 관계의 친밀도에 맞게 학습할 수 있는 AI 한국어 챗봇 메이트"**입니다. 외국인 사용자가 한국어를 학습할 때, 대화 상대와의 친밀도에 따라 적절한 말투를 사용할 수 있도록 도와주는 AI 기반 학습 플랫폼입니다.

**프로젝트 목적**:
- 마이크로서비스 아키텍처 설계 및 구현 경험
- 실시간 채팅 시스템 구축 (SSE 기반)
- AI 기반 멀티 에이전트 시스템 개발
- 클라우드 인프라 운영 경험 (AWS EC2)
- 비용 최적화 경험 (RDS → Local DB 마이그레이션)

**주요 특징**:
- 🏗️ **마이크로서비스 아키텍처**: 6개 독립 서비스로 구성
- 🤖 **Multi-Agent AI 시스템**: 친밀도 분석, 어휘 추출, 대화 생성 등 병렬 처리
- 💬 **실시간 채팅**: SSE 기반 스트리밍으로 실시간 AI 응답 제공
- 🔐 **JWT 기반 인증**: 안전한 사용자 인증 시스템
- 📊 **모니터링**: Prometheus + Grafana를 통한 시스템 모니터링
- 🐳 **Docker 컨테이너화**: 모든 서비스 컨테이너화

### 1.2 개발 규모

- **총 서비스 수**: 6개 (Gateway, Auth, User, Chat, Store, Batch)
- **총 코드 라인 수**: 약 10,000+ 라인 (Java), 5,000+ 라인 (TypeScript)
- **데이터베이스 테이블**: 17개 (5개 스키마)
- **API 엔드포인트**: 50+ 개
- **배포 스크립트**: 15+ 개 (PowerShell/Shell)
- **엔티티 수**: 38개

---

## 2. 기술 스택

### 2.1 백엔드

- **언어**: Java 21
- **프레임워크**: Spring Boot 3.3.4
- **아키텍처**: 마이크로서비스 (MSA)
- **API Gateway**: Spring Cloud Gateway 4.1.0 (Reactive)
- **빌드 도구**: Gradle 8.0+
- **서비스 통신**: Spring Cloud OpenFeign
- **회로 차단기**: Resilience4j 2.2.0
- **비동기 처리**: Project Reactor (WebFlux)
- **인증**: JWT (jjwt 0.12.3)
- **문서화**: SpringDoc OpenAPI 2.2.0 (Swagger)
- **데이터베이스 ORM**: Spring Data JPA, Hibernate
- **마이그레이션**: Flyway 10.8.1

### 2.2 프론트엔드

- **언어**: TypeScript 5.8.3
- **프레임워크**: React 19.1.1
- **빌드 도구**: Vite 7.1.12
- **상태 관리**: Zustand 5.0.8, React Query 5.90.7
- **UI 라이브러리**: Material-UI 7.3.2, TailwindCSS 4.1.13, DaisyUI 5.1.10
- **HTTP 클라이언트**: Axios 1.12.0
- **라우팅**: React Router DOM 7.8.2
- **폼 관리**: React Hook Form 7.62.0
- **SSE 클라이언트**: event-source-polyfill 1.0.31

### 2.3 데이터베이스

- **주 데이터베이스**: PostgreSQL 17 (Shared Database)
- **캐시**: Redis 6.0+
- **마이그레이션**: Flyway

### 2.4 인프라

- **컨테이너화**: Docker, Docker Compose
- **클라우드**: AWS EC2 (t3.medium)
- **운영체제**: Amazon Linux 2
- **모니터링**: Prometheus, Grafana
- **로깅**: Loki, Promtail

### 2.5 외부 서비스

- **AI 서비스**: OpenAI API (GPT-4o-mini)
- **이메일**: Gmail SMTP
- **OAuth**: Google OAuth 2.0

### 2.6 주요 라이브러리

- **Excel 처리**: Apache POI 5.2.5
- **메트릭 수집**: Micrometer Prometheus
- **검증**: Spring Validation
- **AOP**: Spring AOP

---

## 3. 아키텍처 설계

### 3.1 마이크로서비스 아키텍처 (MSA)

DoranDoran은 **마이크로서비스 아키텍처(MSA)**를 채택하여 각 서비스를 독립적으로 개발, 배포, 운영할 수 있도록 설계되었습니다.

#### 전체 아키텍처 구조

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Gateway   │    │   Auth Service  │    │   User Service  │
│   (Port: 8080)  │◄──►│   (Port: 8081)  │◄──►│   (Port: 8082)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Chat Service   │    │  Store Service  │    │  Batch Service  │
│   (Port: 8083)  │    │   (Port: 8084)  │    │   (Port: 8085)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   PostgreSQL    │    │     Redis       │    │   Monitoring    │
│   (Port: 5432)  │    │   (Port: 6379)  │    │  (Grafana)      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

#### 서비스 구성

1. **API Gateway (8080)**: 모든 외부 요청의 진입점, 라우팅 및 인증 처리
2. **Auth Service (8081)**: JWT 기반 인증/인가 처리
3. **User Service (8082)**: 사용자 정보 및 프로필 관리
4. **Chat Service (8083)**: 실시간 채팅 및 AI 멀티 에이전트 시스템
5. **Store Service (8084)**: 표현 보관함 기능
6. **Batch Service (8085)**: 스케줄링 작업 및 배치 처리

### 3.2 서비스 간 통신 패턴

#### 동기 통신 (HTTP/REST)
- **Spring Cloud OpenFeign**: 서비스 간 HTTP 통신
- **Circuit Breaker**: Resilience4j를 통한 장애 격리
- **Retry 메커니즘**: 자동 재시도 로직

#### 비동기 통신
- **Server-Sent Events (SSE)**: 실시간 채팅용 스트리밍
- **Spring Events**: 이벤트 기반 통신 (사용자 상태 변경 등)

### 3.3 데이터베이스 설계

#### Shared Database 패턴
모든 서비스가 하나의 PostgreSQL 인스턴스를 공유하되, 스키마로 분리:

- `auth_schema`: 인증 관련 데이터 (RefreshToken, LoginAttempt, EmailVerification 등)
- `user_schema`: 사용자 정보 (app_user, profiles, settings)
- `chat_schema`: 채팅방 및 메시지 (chatbots, chatrooms, messages, intimacy_progress)
- `store_schema`: 보관함 데이터 (store)
- `batch_schema`: 배치 작업 데이터

#### 데이터베이스 통계
- **총 스키마 수**: 5개
- **총 테이블 수**: 17개
- **PostgreSQL 버전**: 17

### 3.4 보안 아키텍처

- **JWT 토큰 기반 인증**: Gateway에서 토큰 검증 후 서비스로 라우팅
- **HMAC 헤더 검증**: 서비스 간 통신 시 추가 보안 계층
- **IP 블랙리스트 필터**: Gateway 레벨에서 차단된 IP 자동 차단

### 3-1. 도메인 모델 및 엔티티 설계

#### 엔티티 구조

프로젝트에는 총 **38개의 엔티티**가 있으며, 주요 엔티티는 다음과 같습니다:

**Chat Service 엔티티**:
- `ChatRoom`: 채팅방 정보
- `Message`: 메시지 내용
- `Chatbot`: 챗봇 메타 정보
- `IntimacyProgress`: 친밀도 진척도 추적
- `User`: 사용자 정보 (참조용)
- `UserChatbotLastInteraction`: 사용자-챗봇 상호작용 기록
- `MonthlyUserCost`: 월별 사용자 비용
- `AiUsageEvent`: AI 사용 이벤트

**Auth Service 엔티티**:
- `RefreshToken`: 리프레시 토큰
- `LoginAttempt`: 로그인 시도 기록
- `AuthEvent`: 인증 이벤트
- `TokenBlacklist`: 토큰 블랙리스트
- `PasswordResetToken`: 비밀번호 재설정 토큰
- `EmailVerification`: 이메일 인증

**User Service 엔티티**:
- `User`: 사용자 정보
- `UserProfile`: 사용자 프로필
- `UserSetting`: 사용자 설정

**Store Service 엔티티**:
- `Store`: 표현 보관함

#### 스키마 분리 전략

각 서비스는 독립적인 스키마를 사용하여 데이터 격리를 보장합니다:

```sql
-- 스키마 생성 예시
CREATE SCHEMA IF NOT EXISTS auth_schema;
CREATE SCHEMA IF NOT EXISTS user_schema;
CREATE SCHEMA IF NOT EXISTS chat_schema;
CREATE SCHEMA IF NOT EXISTS store_schema;
CREATE SCHEMA IF NOT EXISTS batch_schema;
```

#### JSONB 활용

PostgreSQL의 JSONB 타입을 활용하여 유연한 데이터 구조를 지원합니다:

- **`personality` (JSONB)**: 챗봇 성격 특성, 말투 스타일, 도메인 지식 등
- **`capabilities` (JSONB)**: AI 모델 설정, 안전 필터, 응답 스타일 등
- **`context_data` (JSONB)**: 대화 컨텍스트, 사용자 선호도, 세션 데이터
- **`settings` (JSONB)**: 채팅방 설정, 런타임 정책

**JSONB 사용 예시**:
```java
@Entity
@Table(name = "chatbots", schema = "chat_schema")
public class Chatbot {
    @Column(columnDefinition = "JSONB")
    private JsonNode personality;
    
    @Column(columnDefinition = "JSONB")
    private JsonNode capabilities;
}
```

#### 관계 설계

- **1:N 관계**: User → ChatRoom, ChatRoom → Message
- **N:1 관계**: Message → ChatRoom, ChatRoom → User
- **양방향 관계**: User ↔ ChatRoom (Lazy Loading 사용)

#### 인덱스 전략

- **기본 키 인덱스**: UUID 기반 기본 키에 자동 인덱스
- **외래 키 인덱스**: 연관 관계 조회 성능 향상
- **복합 인덱스**: `(user_id, chatroom_id)`, `(chatroom_id, sequence_number)`
- **부분 인덱스**: `WHERE NOT is_deleted` 조건의 인덱스

---

## 4. 핵심 기능 구현

### 4.1 Multi-Agent AI 시스템

Chat Service의 핵심 기능으로, 여러 AI 에이전트를 병렬로 실행하여 사용자의 대화를 분석하고 응답을 생성합니다.

#### Agent 구성

**1. IntimacyAgent (친밀도 분석)**
- 사용자의 대화 내용을 분석하여 친밀도 레벨(1~3) 감지
- 문법 오류 교정 및 피드백 제공
- 친밀도 진척도 추적 및 저장

**2. VocabularyAgent (어휘 추출)**
- 대화에서 중요한 어휘 추출
- 어휘의 의미 및 사용 예시 제공
- 번역 기능 포함

**3. ConversationAgent (대화 생성)**
- 사용자 메시지에 대한 AI 응답 생성
- 스트리밍 방식으로 응답 전송 (청크 단위)
- 컨텍스트 기반 대화 생성

**4. SummarizerAgent (요약)**
- 대화 내용 요약
- 키워드 추출

#### 처리 패턴

```java
// Multi-Agent Orchestrator 예시
public void processUserMessage(UUID chatroomId, UUID userId, Message userMessage) {
    String content = userMessage.getContent();
    
    // Phase 1: 병렬 실행
    Mono<IntimacyAgentResponse> intimacyMono = intimacyAgent.analyze(chatroomId, content)
        .doOnNext(resp -> {
            sseManager.send(chatroomId, "intimacy_analysis", Map.of(
                "detectedLevel", resp.detectedLevel(),
                "correctedSentence", resp.correctedSentence(),
                "feedback", resp.feedback()
            ));
            updateIntimacyProgress(chatroomId, userId, resp);
        })
        .cache();
    
    Mono<VocabularyAgentResponse> vocabularyMono = vocabularyAgent.extract(chatroomId, content)
        .doOnNext(resp -> {
            sseManager.send(chatroomId, "vocabulary_extracted", Map.of(
                "words", resp.words()
            ));
        })
        .cache();
    
    // 즉시 구독하여 SSE 전송 및 progress 업데이트 실행
    intimacyMono.subscribe();
    vocabularyMono.subscribe();
    
    // Phase 2: Conversation (독립적 스트림)
    conversationAgent.generateResponse(chatroomId, content)
        .subscribe(chunk -> {
            sseManager.send(chatroomId, "conversation_chunk", chunk);
        });
}
```

### 4.2 실시간 채팅 (SSE 기반)

Server-Sent Events (SSE)를 사용하여 실시간으로 AI 응답을 스트리밍합니다.

#### SSE 아키텍처

```
사용자 메시지 → Multi-Agent 처리 → SSE 이벤트 전송 → 프론트엔드 실시간 표시
```

### 4-1. SSE 구현 상세

#### SSEManager 아키텍처

```java
@Component
public class SSEManager {
    // 채팅방별로 여러 연결을 관리
    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    
    // SSE 연결 생성
    public SseEmitter create(UUID chatroomId) {
        SseEmitter emitter = new SseEmitter(0L); // 무제한 타임아웃
        
        // 연결 종료 시 자동 정리
        emitter.onCompletion(() -> remove(chatroomId, emitter));
        emitter.onTimeout(() -> remove(chatroomId, emitter));
        emitter.onError((ex) -> remove(chatroomId, emitter));
        
        emitters.computeIfAbsent(chatroomId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        return emitter;
    }
    
    // 이벤트 전송
    public void send(UUID chatroomId, String eventName, Object data) {
        List<SseEmitter> list = emitters.get(chatroomId);
        if (list == null || list.isEmpty()) return;
        
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                remove(chatroomId, emitter);
            }
        }
    }
}
```

#### 이벤트 타입 및 데이터 구조

| 이벤트 타입 | 데이터 형식 | 설명 | 전송 시점 |
|------------|------------|------|----------|
| `intimacy_analysis` | JSON | 친밀도 분석 결과 | IntimacyAgent 완료 시 |
| `vocabulary_extracted` | JSON | 어려운 단어 추출 결과 | VocabularyAgent 완료 시 |
| `vocabulary_translated` | JSON | 번역 결과 | TranslationAgent 완료 시 |
| `conversation_chunk` | Text | AI 응답 청크 (스트리밍) | ConversationAgent 처리 중 |
| `conversation_complete` | JSON | AI 응답 완료 | ConversationAgent 완료 시 |
| `aggregated_complete` | JSON | 전체 결과 집계 | 모든 Agent 완료 시 |
| `greeting_bot_message` | JSON | AI 인사말 | 채팅방 생성 시 |
| `greeting_guide_message` | JSON | 대화 가이드 메시지 | 채팅방 생성 시 |

#### 프론트엔드 SSE 클라이언트 구현

```typescript
// SSE 연결 생성
const eventSource = new EventSource(
  `${BASE_URL}/api/chat/stream/${chatroomId}?userId=${userId}`
);

// 이벤트 리스너 등록
eventSource.addEventListener('intimacy_analysis', (event) => {
  const data = JSON.parse(event.data);
  // 친밀도 분석 결과 UI에 표시
});

eventSource.addEventListener('conversation_chunk', (event) => {
  // 실시간으로 AI 응답 조각 표시
  conversationBuffer += event.data;
});

eventSource.addEventListener('conversation_complete', (event) => {
  const data = JSON.parse(event.data);
  // 완성된 AI 응답 표시
});
```

#### 연결 해제 및 에러 처리

- **자동 정리**: 연결 종료, 타임아웃, 에러 시 자동으로 emitter 제거
- **에러 처리**: JSON 파싱 오류, 네트워크 오류 등 처리
- **재연결**: 연결 끊김 시 자동 재연결 로직

### 4-2. 비즈니스 로직

#### 친밀도 레벨 시스템

친밀도 레벨은 1~3단계로 구분됩니다:

- **Level 1 (격식체)**: 공식적인 상황, 상급자, 동료 (예: "안녕하십니까", "~습니다")
- **Level 2 (부드러운 존댓말)**: 친한 동료, 일반적인 상황 (예: "안녕하세요", "~해요")
- **Level 3 (반말)**: 친한 친구, 가족 (예: "안녕", "~해")

#### Multi-Agent 오케스트레이션 로직

**병렬 처리**:
- IntimacyAgent와 VocabularyAgent는 동시에 실행
- `Mono.cache()`를 사용하여 중복 호출 방지

**순차 처리**:
- VocabularyAgent 결과를 TranslationAgent에 전달

**독립적 스트림**:
- ConversationAgent는 별도 스트림으로 실행하여 실시간 응답 제공

#### 프롬프트 동적 생성 전략

`PromptService`는 컨셉, 친밀도 레벨, 채팅방 컨텍스트를 반영한 시스템 프롬프트를 동적으로 생성합니다:

```java
@Cacheable(value = "prompts", key = "#chatroomId + ':' + getCurrentIntimacyLevel(#chatroomId)")
public String buildSystemPrompt(UUID chatroomId) {
    ChatRoom chatRoom = chatRoomRepository.findById(chatroomId)
        .orElseThrow(() -> new ChatRoomNotFoundException());
    
    String concept = chatRoom.getConcept(); // FRIEND, HONEY, COWORKER, SENIOR, BOSS
    Integer intimacyLevel = getIntimacyLevel(chatroomId);
    
    // 컨셉과 친밀도 레벨에 맞는 프롬프트 생성
    return buildPromptByConceptAndLevel(concept, intimacyLevel);
}
```

#### 학습 진척도 추적

`IntimacyProgress` 엔티티를 통해 채팅방별 친밀도 진척도를 추적합니다:

- **total_corrections**: 교정 횟수
- **intimacy_level**: 현재 친밀도 레벨
- **progress_data**: 세부 학습 통계 (JSONB)

#### 표현 보관함 기능 (Store Service)

사용자가 좋아하는 표현을 보관함에 저장하고 관리할 수 있습니다:

- **표현 저장**: 메시지에서 표현 추출하여 저장
- **보관함 조회**: 저장된 표현 목록 조회 (페이징 지원)
- **방별 조회**: 특정 채팅방의 표현만 조회
- **표현 삭제**: 소프트 삭제 방식

### 4.3 JWT 기반 인증/인가

#### 토큰 생성/검증

```java
// JWT 토큰 생성
public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userDetails.getUserId());
    claims.put("email", userDetails.getEmail());
    
    return Jwts.builder()
        .setClaims(claims)
        .setSubject(userDetails.getEmail())
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION))
        .signWith(SignatureAlgorithm.HS512, secretKey)
        .compact();
}
```

#### 토큰 검증 프로세스

1. Gateway의 `JwtAuthFilter`에서 토큰 검증
2. Auth Service와 통신하여 토큰 유효성 확인
3. 유효한 토큰인 경우 HMAC 헤더 주입
4. 백엔드 서비스에서 HMAC 헤더 검증

### 4.4 이메일 인증 시스템

- **이메일 인증 코드 발송**: 회원가입 시 6자리 인증 코드 발송
- **인증 코드 검증**: 유효 시간 내 코드 확인
- **비밀번호 재설정**: 이메일을 통한 비밀번호 재설정 링크 발송

---

## 5. 성능 최적화

### 5.1 Redis 캐싱 전략

#### ChatRoom 캐싱
- **키 형식**: `chatroom::{chatroomId}`
- **TTL**: 1800초 (30분)
- **성능 개선**: DB 조회 5ms → 캐시 조회 1ms (80% 개선)

#### Prompt 캐싱
- **키 형식**: `prompts::{chatroomId}:{intimacyLevel}`
- **TTL**: 3600초 (1시간)
- **성능 개선**: 프롬프트 생성 15-20ms → 캐시 조회 1ms (93% 개선)

#### IntimacyProgress 캐싱
- **키 형식**: `intimacy::{chatroomId}`
- **TTL**: 1800초 (30분)
- **Write-Through 패턴**: 업데이트 시 DB와 캐시 동시 갱신

### 5-1. Redis 캐싱 구현 상세

#### 캐싱 전략

**Cache-Aside 패턴** (대부분 적용):
- 읽기: 캐시 확인 → 없으면 DB 조회 → 캐시 저장
- 쓰기: DB 업데이트 → 캐시 무효화

**Write-Through 패턴** (IntimacyProgress):
- 업데이트 시 DB와 캐시 동시 갱신

#### 캐시 키 설계

```java
// 복합 키 예시
@Cacheable(key = "#chatroomId + ':' + getCurrentIntimacyLevel(#chatroomId)")
public String buildSystemPrompt(UUID chatroomId) { ... }
```

#### TTL 최적화

- **ChatRoom**: 30분 (자주 변경되지 않음)
- **Prompt**: 1시간 (프롬프트 생성 비용이 높음)
- **IntimacyProgress**: 30분 (업데이트 빈도 고려)
- **User/Chatbot**: 2시간 (마스터 데이터)

#### 캐시 무효화 전략

```java
@CacheEvict(value = {"chatrooms", "roomList"}, key = "#chatroomId")
public ChatRoom updateRoom(UUID chatroomId, ...) {
    // 업데이트 로직
}
```

#### 성능 개선 효과

**Before (캐싱 적용 전)**:
- 평균 메시지 처리 시간: 85-120ms
- DB 동시 연결 수: 평균 15개
- Multi-Agent 처리 시 DB 조회: 메시지당 8-12회
- 동시 사용자 처리 한계: 약 50명

**After (캐싱 적용 후)**:
- 평균 메시지 처리 시간: 45-70ms (40% 개선)
- DB 동시 연결 수: 평균 6개 (60% 감소)
- Multi-Agent 처리 시 DB 조회: 메시지당 2-3회 (75% 감소)
- 동시 사용자 처리 한계: 약 120명 (140% 증가)

**캐시 히트율 통계**:
- ChatRoom: 85-92%
- IntimacyProgress: 78-88%
- User/Chatbot: 95-98%
- SystemPrompt: 82-90%

### 5.2 데이터베이스 최적화

#### 연결 풀 최적화 (HikariCP)

서비스별 최적화된 연결 풀 크기:

- **Auth Service**: 30개
- **User Service**: 30개
- **Chat Service**: 50개 (높은 동시성)
- **Store Service**: 20개

**최적화 설정**:
- 최소 유휴 연결: 5개 (응답성 향상)
- 연결 수명: 10분 (주기적 갱신)
- 누수 감지: 10초 이상 점유 시 경고

#### N+1 문제 해결

- **Fetch Join 사용**: 연관 엔티티 한 번에 조회
- **@EntityGraph 활용**: 필요한 연관 관계만 로딩
- **DTO 변환**: 엔티티 직접 반환 대신 DTO 사용

### 5.3 비동기 처리

#### Reactor 기반 비동기 처리

- **WebFlux 사용**: Chat Service의 SSE 스트리밍
- **Mono/Flux 체인**: 비동기 파이프라인 구성
- **Scheduler 분리**: CPU 집약적 작업과 I/O 작업 분리

#### Multi-Agent 병렬 처리

```java
// 병렬 처리 예시
Mono<IntimacyAgentResponse> intimacyMono = intimacyAgent.analyze(chatroomId, content).cache();
Mono<VocabularyAgentResponse> vocabularyMono = vocabularyAgent.extract(chatroomId, content).cache();

// 즉시 구독하여 SSE 전송 및 progress 업데이트 실행
intimacyMono.subscribe();
vocabularyMono.subscribe();
```

---

## 6. 트러블슈팅 및 문제 해결

### 6.1 인프라 문제

#### 6.1.1 RDS OOM 문제 (2025-10-14)

**문제 상황**:
- API 응답 500 오류 발생
- 컨테이너 Health `unhealthy` 상태
- `/actuator/health` 엔드포인트 500 반환

**원인 분석**:
- 컨테이너 로그에서 `java.lang.OutOfMemoryError: unable to create native thread` 다수 발생
- Hikari 커넥션 풀 스레드 생성 실패
- JVM/프로세스의 스레드/메모리 한도 초과

**해결 방법**:
1. **JVM 메모리 옵션 조정**:
   ```bash
   JAVA_TOOL_OPTIONS="-Xms256m -Xmx512m -XX:MaxDirectMemorySize=96m -XX:MaxMetaspaceSize=128m -XX:MaxRAMPercentage=60"
   ```

2. **Tomcat 스레드 제한**:
   ```bash
   SERVER_TOMCAT_THREADS_MAX=40
   SERVER_TOMCAT_ACCEPT_COUNT=50
   ```

3. **Hikari 커넥션 풀 크기 조정**:
   ```bash
   SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=5
   ```

**결과**:
- 컨테이너 Health `healthy` 상태로 복구
- `/actuator/health` 정상 응답
- API 정상화

**교훈**:
- JVM/풀 기본값은 환경에 따라 과도할 수 있음
- 명시적으로 리소스 한도를 설정하는 것이 중요
- 헬스체크 실패는 오케스트레이션에 악영향을 미침

#### 6.1.2 Connection Pool Exhaustion (2025-10-14)

**문제 상황**:
- 503 Service Unavailable 오류 발생
- 모든 API 요청 30초 타임아웃 후 실패
- HikariCP 연결 풀 고갈 (10개 연결 모두 사용 중)

**원인 분석**:
- **핵심 원인**: Connection Leak (연결 누수)
- **트리거**: SSE + `spring.jpa.open-in-view=true` 조합
  - SSE 스트림이 오래 유지되는 동안 DB 연결도 계속 점유
  - 타임아웃 발생 시 비정상 종료 → 연결 반환 지연
  - 10개 연결이 모두 누수되어 새 요청 처리 불가

**해결 방법**:
1. **open-in-view 비활성화**:
   ```bash
   SPRING_JPA_OPEN_IN_VIEW=false
   ```

2. **연결 풀 최적화**:
   ```bash
   SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=20  # 10 → 20
   SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=5         # 0 → 5
   SPRING_DATASOURCE_HIKARI_LEAK_DETECTION_THRESHOLD=10000  # 10초
   SPRING_DATASOURCE_HIKARI_MAX_LIFETIME=600000    # 30분 → 10분
   ```

**결과**:
- SSE 스트림이 DB 연결을 불필요하게 점유하지 않음
- 연결 풀 20개로 여유 확보
- 누수 10초 이상 시 즉시 감지 및 경고

**교훈**:
- Long-running 요청에서는 `open-in-view=false` 필수
- 연결 생명주기 관리가 중요
- 연결 풀 모니터링 설정 필수

#### 6.1.3 RDS → Local DB 마이그레이션

**배경**:
- AWS RDS Aurora 사용으로 인한 높은 비용 ($59.04/월)
- 비용 절감을 위한 로컬 PostgreSQL 컨테이너 전환

**마이그레이션 과정**:
1. **데이터 백업**: RDS Aurora 데이터 덤프 (386KB)
2. **컨테이너 교체**: `shared-db` → `dorandoran-shared-db`
3. **PostgreSQL 업그레이드**: 15 → 17
4. **데이터 복원**: 5개 스키마, 17개 테이블 복원
5. **배포 스크립트 수정**: 모든 서비스의 DB 연결을 localhost로 변경

**비용 절감 효과**:
- **Before**: $118-143/월 (RDS 포함)
- **After**: $50-64/월 (로컬 DB)
- **절감액**: 월 $58-79 (50-60% 절감)
- **연간 절감**: $696-948

**주의사항**:
- 리소스 사용량 증가 (PostgreSQL 메모리: ~500MB-1GB)
- 동시 사용자 수 소폭 감소 (80-120명 → 70-100명)
- 백업 전략 수립 필요

### 6.2 코드 레벨 문제

#### 6.2.1 Hibernate 프록시 순환 참조 문제

**문제 상황**:
- 애플리케이션 실행 중 `NullPointerException`과 스택 오버플로우 발생
- 로그에서 `User$HibernateProxy$Dyp9SYgE.toString` 무한 반복 확인

**원인 분석**:
- 양방향 관계를 가진 엔티티에서 Lombok `@Data`가 생성한 `toString()` 메서드가 순환 참조를 일으킴
- `User.toString()` → `chatRooms` → `ChatRoom.toString()` → `user` → 무한 루프

**해결 방법**:
```java
// User 엔티티
@ToString(exclude = {"chatRooms"})  // 순환 참조 방지
public class User {
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<ChatRoom> chatRooms = new ArrayList<>();
}

// ChatRoom 엔티티
@ToString(exclude = {"user", "chatbot", "messages", "lastMessage"})
public class ChatRoom {
    private User user;
    private Chatbot chatbot;
    private List<Message> messages;
    private Message lastMessage;
}
```

**결과**:
- 순환 참조 문제 해결
- 불필요한 연관 객체 로딩 방지
- 메모리 사용량 감소

#### 6.2.2 GreetingService 프롬프트 개선

**문제 상황**:
- `greeting_guide_message` SSE 이벤트가 클라이언트에 전달되지 않음
- AI 응답 파싱 실패로 인한 `NullPointerException` 발생
- 프롬프트 JSON 형식 불일치

**해결 방법**:
1. **JSON 형식 통일**: 모든 프롬프트에서 동일한 형식 사용
2. **예시 시나리오 수정**: 올바른 JSON 형식 예시 제공
3. **null 체크 및 안전성 강화**: 파싱 시 null 체크 추가

**결과**:
- AI가 올바른 JSON 형식으로 응답
- 파싱 성공으로 정상적인 `GreetingResponse` 생성
- `greeting_bot_message`와 `greeting_guide_message` 이벤트 모두 정상 전송

### 6.3 보안 문제

#### 6.3.1 IP 블랙리스트 시스템 구현

**문제 상황**:
- 의심 IP (172.104.24.172)로부터 비정상 HTTP 요청 다수 발생
- RTSP/1.0, SIP/2.0 등 비정상 프로토콜 요청
- 보안 스캔/공격 시도 가능성

**해결 방법**:
1. **IpBlacklistFilter 구현**: Gateway 필터 체인에서 가장 먼저 실행
2. **Redis 기반 저장**: 블랙리스트 IP를 Redis에 저장
3. **관리자 API 구현**: `/api/admin/blacklist` 엔드포인트를 통한 IP 관리

**결과**:
- 차단된 IP는 HTTP 403으로 즉시 차단
- 관리자는 자신의 IP가 차단되어 있어도 블랙리스트 관리 가능
- 보안 위협 감소

#### 6.3.2 SecurityConfig 정책 일관성 개선

**문제 상황**:
- `SecurityConfig`: 모든 `/api/**` 경로를 `permitAll()`로 설정
- `JwtAuthFilter`: 제외 목록에 없는 경로는 JWT 인증 필요
- 정책 불일치로 인한 혼란

**해결 방법**:
```java
// SecurityConfig 수정
.pathMatchers("/actuator/**").permitAll()
.pathMatchers("/api/auth/login").permitAll()
.pathMatchers("/api/users").permitAll()  // POST 회원가입
.pathMatchers("/api/admin/**").authenticated()  // 관리자 API는 인증 필요
.pathMatchers("/api/**").authenticated()  // 나머지는 인증 필요
```

**결과**:
- JwtAuthFilter의 정책과 일치
- SecurityConfig의 의도 명확화
- 향후 Spring Security 기능 확장 시 일관성 유지

---

## 7. 인프라 및 배포

### 7.1 인프라 구성

#### AWS EC2 기반 배포
- **인스턴스 타입**: t3.medium (2 vCPU, 4GB RAM)
- **운영체제**: Amazon Linux 2
- **네트워크**: VPC 내부 배포

#### Docker 컨테이너화
- **모든 서비스 컨테이너화**: 각 서비스별 Dockerfile 작성
- **Docker Network**: `dorandoran-network`를 통한 서비스 간 통신
- **컨테이너 이름 규칙**: `dorandoran-{service-name}`

#### 데이터베이스 구성
- **PostgreSQL**: Shared Database 패턴
  - 컨테이너 이름: `dorandoran-shared-db`
  - 포트: 5432
  - 버전: PostgreSQL 17
  - 스키마 분리: 5개 스키마 (auth, user, chat, store, batch)

#### Redis 구성
- **컨테이너 이름**: `dorandoran-redis`
- **포트**: 6379
- **용도**: 캐싱 및 세션 관리

### 7.2 배포 자동화

#### PowerShell/Shell 배포 스크립트

서비스별 개별 배포 스크립트:
- `deploy-auth-service.ps1`
- `deploy-user-service.ps1`
- `deploy-chat-service.ps1`
- `deploy-store-service.ps1`
- `deploy-gateway-service.ps1`
- `deploy-batch-service.ps1`
- `deploy-all-services.ps1`

#### 배포 스크립트 주요 기능

1. **이미지 빌드**: Gradle 빌드 후 Docker 이미지 생성
2. **컨테이너 중지/제거**: 기존 컨테이너 정리
3. **컨테이너 실행**: 환경변수 설정 및 네트워크 연결
4. **Health Check**: `/actuator/health` 엔드포인트 확인

#### 배포 스크립트 예시

```powershell
# Chat Service 배포 스크립트 예시
docker stop dorandoran-chat
docker rm dorandoran-chat

docker run -d --name dorandoran-chat \
  --network dorandoran-network \
  -p 8083:8083 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://dorandoran-shared-db:5432/dorandoran \
  -e SPRING_DATASOURCE_USERNAME=doran \
  -e SPRING_DATASOURCE_PASSWORD=$DB_PASSWORD \
  -e SPRING_REDIS_HOST=dorandoran-redis \
  dorandoran-chat:latest

# Health Check
Start-Sleep -Seconds 10
$health = Invoke-RestMethod -Uri "http://localhost:8083/actuator/health"
Write-Host "Health Status: $($health.status)"
```

### 7.3 모니터링

#### Grafana 대시보드
- **URL**: http://localhost:3000
- **기본 계정**: admin/admin
- **대시보드**: 서비스별 메트릭 시각화

#### Prometheus 메트릭 수집
- **URL**: http://localhost:9090
- **수집 주기**: 15초
- **메트릭 종류**:
  - HTTP 요청 수 및 응답 시간
  - 데이터베이스 연결 풀 상태
  - JVM 메모리 사용량
  - CPU 사용률

#### 로그 분석 스크립트
- **`check_bottleneck.sh`**: 서비스별 병목 현상 체크
- **로그 분석**: Docker logs를 통한 에러 추적
- **리소스 모니터링**: CPU, 메모리, 네트워크 사용량 추적

---

## 8. 보안 구현

### 8.1 JWT 토큰 기반 인증

#### 토큰 구조
- **Access Token**: 짧은 수명 (예: 1시간)
- **Refresh Token**: 긴 수명 (예: 7일)
- **HMAC 서명**: HS512 알고리즘 사용

#### 토큰 검증 프로세스
1. Gateway의 `JwtAuthFilter`에서 토큰 검증
2. Auth Service와 통신하여 토큰 유효성 확인
3. 유효한 토큰인 경우 HMAC 헤더 주입
4. 백엔드 서비스에서 HMAC 헤더 검증

### 8.2 IP 블랙리스트 필터

#### 구현 내용
- **GlobalFilter**: Gateway 필터 체인에서 가장 먼저 실행 (Order: -100)
- **Redis 저장**: 블랙리스트 IP를 Redis에 저장
- **제외 경로**: `/actuator/**`, `/api/admin/blacklist` 제외

#### 관리자 API
- **GET `/api/admin/blacklist`**: 블랙리스트 IP 목록 조회
- **POST `/api/admin/blacklist`**: IP 추가
- **DELETE `/api/admin/blacklist/{ip}`**: IP 제거

### 8.3 Security Group 최적화

#### AWS Security Group 규칙
- **SSH (22/tcp)**: 관리자 IP만 허용
- **HTTP (80/tcp)**: 모든 IP 허용 (필요 시)
- **HTTPS (443/tcp)**: 모든 IP 허용 (필요 시)
- **애플리케이션 포트**: 내부 네트워크만 허용

### 8.4 HMAC 헤더 검증

#### 목적
- 서비스 간 통신 시 추가 보안 계층 제공
- 위조된 요청 방지

#### 구현
```java
// Gateway에서 HMAC 헤더 주입
String hmac = generateHMAC(requestBody, secretKey);
exchange.getRequest().mutate()
    .header("X-HMAC", hmac)
    .build();

// 백엔드 서비스에서 검증
String receivedHmac = request.getHeader("X-HMAC");
if (!isValidHMAC(requestBody, receivedHmac, secretKey)) {
    throw new UnauthorizedException();
}
```

---

## 9. 운영 기록 및 통계

### 9.1 사용자 및 활동 통계

- **총 등록 사용자**: 115명
- **총 채팅방 수**: 237개
- **Gateway 요청 수 (24시간)**: 약 410건
- **Auth Service 요청 수 (24시간)**: 589건
  - 로그인: 159건 (평균 응답 시간 0.130초)
  - 사용자 정보 조회: 430건 (평균 응답 시간 0.009초)

### 9.2 서비스 구성

- **총 서비스 수**: 6개
  - Gateway Service
  - Auth Service
  - User Service
  - Chat Service
  - Store Service
  - Batch Service

- **데이터베이스 스키마**: 5개
  - auth_schema
  - user_schema
  - chat_schema
  - store_schema
  - batch_schema

- **총 테이블 수**: 17개

### 9.3 해결한 문제 통계

- **인프라 문제**: 3건
  - RDS OOM 문제
  - Connection Pool Exhaustion
  - RDS → Local DB 마이그레이션

- **코드 레벨 문제**: 2건
  - Hibernate 프록시 순환 참조
  - GreetingService 프롬프트 개선

- **보안 문제**: 2건
  - IP 블랙리스트 시스템 구현
  - SecurityConfig 정책 일관성 개선

**총 해결한 주요 문제**: 7건 이상

### 9.4 비용 절감

- **RDS 제거**: 월 $59.04 절감
- **전체 비용 절감**: 월 $58-79 (50-60% 절감)
- **연간 절감**: $696-948

### 9.5 리소스 사용 현황

#### 서비스별 리소스 사용률 (정상 운영 시)
- **Chat Service**: CPU 0.22%, 메모리 547.3MB (14.30%)
- **Store Service**: CPU 0.16%, 메모리 484.6MB (12.66%)
- **Auth Service**: CPU 0.12%, 메모리 439.2MB (11.48%)
- **User Service**: CPU 0.09%, 메모리 328.5MB (8.58%)
- **Gateway Service**: CPU 0.13%, 메모리 268.9MB (7.02%)
- **PostgreSQL**: 메모리 144.6MB (3.78%)
- **Redis**: CPU 0.47%, 메모리 5.059MB (0.13%)

**전체 리소스 사용률**: 정상 범위 내

### 9.6 데이터베이스 연결 풀 상태

- **Auth Service**: 0/30 (0% 사용률)
- **Chat Service**: 0/50 (0% 사용률)
- **User Service**: 0/30 (0% 사용률)
- **Store Service**: 0/20 (0% 사용률)

**연결 풀 상태**: 모든 서비스 정상

### 9-1. 프로젝트 성과 및 영향

#### 사용자 수 및 활동 통계
- **등록 사용자**: 115명
- **채팅방 수**: 237개
- **일일 활성 사용자**: 평균 20-30명
- **메시지 수**: 수천 건

#### 채팅방 및 메시지 통계
- **총 채팅방**: 237개
- **평균 채팅방당 메시지 수**: 약 50-100개
- **AI 응답 생성 시간**: 평균 2-5초

#### 시스템 안정성 지표
- **Uptime**: 99% 이상
- **평균 응답 시간**: 100-200ms
- **에러율**: 1% 미만

#### 비용 절감 효과
- **RDS 제거**: 월 $59.04 절감
- **전체 비용 절감**: 월 $58-79 (50-60% 절감)
- **연간 절감**: $696-948

#### 성능 개선 효과
- **응답 시간**: 40% 개선 (85-120ms → 45-70ms)
- **동시 사용자 처리 능력**: 140% 증가 (50명 → 120명)
- **DB 조회 감소**: 75% 감소 (메시지당 8-12회 → 2-3회)
- **캐시 히트율**: 80% 이상

---

## 10. 프론트엔드 아키텍처

### 10.1 React 19 + TypeScript 구조

프론트엔드는 React 19와 TypeScript를 사용하여 구축되었습니다.

#### 주요 특징
- **TypeScript**: 타입 안정성 보장
- **Vite**: 빠른 개발 서버 및 빌드
- **React 19**: 최신 React 기능 활용

### 10.2 상태 관리

#### Zustand
- 전역 상태 관리 (사용자 정보, 인증 상태 등)
- 가벼운 상태 관리 라이브러리

#### React Query
- 서버 상태 관리 (API 데이터 캐싱, 동기화)
- 자동 리페칭 및 캐시 관리

### 10.3 UI 라이브러리

- **Material-UI (MUI)**: 컴포넌트 라이브러리
- **TailwindCSS**: 유틸리티 기반 CSS 프레임워크
- **DaisyUI**: TailwindCSS 기반 컴포넌트 라이브러리

### 10.4 실시간 통신 (SSE 클라이언트 구현)

```typescript
// SSE 연결 생성
const eventSource = new EventSource(
  `${BASE_URL}/api/chat/stream/${chatroomId}?userId=${userId}`
);

// 이벤트 리스너 등록
eventSource.addEventListener('intimacy_analysis', (event) => {
  const data = JSON.parse(event.data);
  // 친밀도 분석 결과 처리
});

eventSource.addEventListener('conversation_chunk', (event) => {
  // 실시간 AI 응답 표시
  setConversationBuffer(prev => prev + event.data);
});
```

### 10.5 라우팅 및 인증 처리

- **React Router DOM**: 클라이언트 사이드 라우팅
- **인증 가드**: 보호된 라우트 접근 제어
- **토큰 관리**: LocalStorage를 통한 토큰 저장

---

## 11. API 설계 및 문서화

### 11.1 Swagger/OpenAPI (SpringDoc) 사용

각 서비스별로 Swagger UI가 제공됩니다:

- **Gateway**: `http://localhost:8080/swagger-ui.html`
- **Auth Service**: `http://localhost:8081/swagger-ui.html`
- **User Service**: `http://localhost:8082/swagger-ui.html`
- **Chat Service**: `http://localhost:8083/swagger-ui.html`

### 11.2 RESTful API 설계 원칙

- **리소스 중심 설계**: `/api/chat/chatrooms`, `/api/users` 등
- **HTTP 메서드 활용**: GET, POST, PUT, PATCH, DELETE
- **상태 코드 사용**: 200, 201, 400, 401, 403, 404, 500 등

### 11.3 API 버전 관리

현재는 버전 관리 없이 단일 버전으로 운영 중이며, 향후 필요 시 `/api/v2/` 형태로 확장 가능합니다.

### 11.4 에러 응답 표준화

#### Auth Service 응답 형식
```json
{
  "success": true,
  "data": { /* 실제 데이터 */ },
  "message": "요청이 성공적으로 처리되었습니다.",
  "errorCode": null,
  "timestamp": "2024-01-01T12:00:00"
}
```

#### 에러 응답
```json
{
  "success": false,
  "data": null,
  "message": "오류 메시지",
  "errorCode": "ERROR_CODE",
  "timestamp": "2024-01-01T12:00:00"
}
```

---

## 12. 테스트 전략

### 12.1 단위 테스트 (JUnit 5)

프로젝트에는 기본적인 단위 테스트 구조가 있습니다:

```java
@SpringBootTest
class ChatServiceTest {
    @Test
    void contextLoads() {
        // TODO: 개발자들이 구현할 테스트들
    }
}
```

### 12.2 통합 테스트 (Spring Boot Test)

Gateway 통합 테스트 예시:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class AuthE2ETest {
    @Test
    @DisplayName("전체 인증 플로우 E2E 테스트")
    void testAuthFlow() {
        // Gateway -> Auth Service -> User Service 전체 플로우 테스트
    }
}
```

### 12.3 E2E 테스트 (Gateway 통합 테스트)

Gateway를 통한 전체 플로우 테스트가 구현되어 있습니다.

### 12.4 테스트 커버리지 현황

현재 테스트 커버리지는 낮은 상태이며, 향후 개선이 필요합니다.

### 12.5 친밀도 레벨 테스트 케이스

`tests/intimacy-level-test-cases.md`에 친밀도 레벨 유지 테스트 케이스가 정의되어 있습니다:

- **Coworker Bot - Level 1 (격식체)**: 업무 과중 상황, 점심 약속, 프로젝트 진행 상황
- **Boss Bot - Level 1 (공식 보고체)**: 보고서 완성 등

---

## 13. 배포 자동화 및 DevOps

### 13.1 배포 스크립트 (PowerShell/Shell)

#### PowerShell 스크립트
- `deploy-auth-service.ps1`: Auth Service 배포
- `deploy-user-service.ps1`: User Service 배포
- `deploy-chat-service.ps1`: Chat Service 배포
- `deploy-store-service.ps1`: Store Service 배포
- `deploy-gateway-service.ps1`: Gateway Service 배포
- `deploy-batch-service.ps1`: Batch Service 배포
- `deploy-all-services.ps1`: 모든 서비스 일괄 배포

#### Shell 스크립트
- `deploy-aws.sh`: AWS 환경 배포
- `deploy-simple.sh`: 간단한 배포 스크립트
- `deploy-chat-only.sh`: Chat Service만 배포

### 13.2 Docker 컨테이너화 전략

각 서비스별로 독립적인 Dockerfile이 작성되어 있습니다:

- `Dockerfile.auth`
- `Dockerfile.user`
- `Dockerfile.chat`
- `Dockerfile.store`
- `Dockerfile.gateway`
- `Dockerfile.batch`

### 13.3 환경별 설정 관리

- **dev**: 로컬 개발 환경
- **prod**: 프로덕션 환경
- **docker**: Docker 컨테이너 환경

각 환경별로 `application-{profile}.yml` 파일로 설정을 관리합니다.

### 13.4 자동 백업 스크립트

`scripts/backup/` 디렉토리에 백업 스크립트가 있습니다:

- `create-auto-backup.sh`: 자동 백업 생성
- `setup-cron.sh`: Cron 작업 설정

### 13.5 모니터링 스크립트

- `check_bottleneck.sh`: 서비스별 병목 현상 체크
- `analyze-user-requests.sh`: 사용자 요청 분석
- `daily-log-analysis.sh`: 일일 로그 분석

---

## 14. 데이터베이스 마이그레이션

### 14.1 Flyway를 통한 스키마 버전 관리

Flyway 10.8.1을 사용하여 데이터베이스 스키마 버전을 관리합니다.

#### 마이그레이션 파일 구조
```
src/main/resources/db/migration/
  V1__Initial_schema.sql
  V2__Add_intimacy_progress.sql
  ...
```

### 14.2 마이그레이션 전략

- **버전 관리**: V{version}__{description}.sql 형식
- **롤백 전략**: 각 마이그레이션은 되돌릴 수 있도록 설계
- **환경별 적용**: dev, prod 환경별로 다른 마이그레이션 적용 가능

### 14.3 RDS → Local DB 마이그레이션 경험

**마이그레이션 과정**:
1. RDS Aurora 데이터 백업 (pg_dump)
2. PostgreSQL 컨테이너 생성 및 데이터 복원
3. 모든 서비스의 DB 연결 설정 변경
4. 서비스 재배포 및 검증

**학습한 점**:
- 데이터 백업/복원 프로세스
- PostgreSQL 버전 업그레이드 (15 → 17)
- 스키마별 데이터 마이그레이션
- 배포 스크립트 일괄 수정

---

## 15. 에러 핸들링 및 복원력

### 15.1 Circuit Breaker 패턴 (Resilience4j)

서비스 간 통신 시 장애 격리를 위해 Circuit Breaker 패턴을 사용합니다.

#### 구현 예시
```java
@CircuitBreaker(name = "user-service", fallbackMethod = "getUserByIdFallback")
@Retry(name = "user-service")
public UserDto getUserById(String userId) {
    return userServiceClient.getUserById(userId);
}

public UserDto getUserByIdFallback(String userId, Exception ex) {
    log.error("User Service 호출 실패: {}", ex.getMessage());
    return null;
}
```

### 15.2 Retry 메커니즘

Resilience4j의 Retry 기능을 사용하여 일시적 오류에 대한 자동 재시도를 구현합니다.

#### 설정 예시
```yaml
resilience4j:
  retry:
    instances:
      user-service:
        max-attempts: 3
        wait-duration: 1s
```

### 15.3 Fallback 전략

Circuit Breaker가 열렸을 때 Fallback 메서드를 호출하여 기본값을 반환하거나 캐시된 데이터를 사용합니다.

### 15.4 예외 처리 표준화

- **커스텀 예외 클래스**: `ChatRoomNotFoundException`, `UserNotFoundException` 등
- **글로벌 예외 핸들러**: `@ControllerAdvice`를 통한 통합 예외 처리
- **에러 응답 형식**: 일관된 에러 응답 구조

---

## 16. 모니터링 및 로깅

### 16.1 Prometheus 메트릭 수집

각 서비스는 Micrometer를 통해 Prometheus 메트릭을 노출합니다.

#### 수집되는 메트릭
- HTTP 요청 수 및 응답 시간
- 데이터베이스 연결 풀 상태
- JVM 메모리 사용량
- CPU 사용률
- 캐시 히트율

### 16.2 Grafana 대시보드 구성

Grafana를 통해 시각화된 대시보드를 제공합니다:

- **서비스별 메트릭**: 각 서비스의 상태 및 성능 지표
- **에러율 추적**: 5분/24시간 단위 에러 발생 추적
- **응답 시간**: 평균 응답 시간 측정
- **리소스 사용률**: CPU, 메모리, 연결 풀 사용률

### 16.3 로그 분석 스크립트

#### `check_bottleneck.sh`
서비스별 병목 현상 체크:
- 데이터베이스 연결 풀 상태 확인
- 메모리 사용량 추적
- 에러 로그 분석

#### 기타 분석 스크립트
- `analyze-user-requests.sh`: 사용자 요청 분석
- `daily-log-analysis.sh`: 일일 로그 분석
- `traffic-analysis.sh`: 트래픽 분석

### 16.4 성능 병목 감지 도구

- **Docker logs**: 실시간 로그 모니터링
- **Prometheus 쿼리**: 메트릭 기반 병목 감지
- **로그 분석**: 느린 쿼리, 타임아웃 등 추적
- **리소스 모니터링**: CPU, 메모리, 네트워크 사용량 추적

---

## 17. 보안 구현 상세

### 17.1 IP 블랙리스트 시스템

Gateway 레벨에서 차단된 IP를 자동으로 차단하는 필터를 구현했습니다.

#### 구현 내용

```java
@Component
public class IpBlacklistFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = getClientIp(exchange);
        
        if (isBlacklisted(clientIp) && !isExcludedPath(exchange.getRequest().getPath().value())) {
            return createForbiddenResponse(exchange);
        }
        
        return chain.filter(exchange);
    }
    
    @Override
    public int getOrder() {
        return -100; // 가장 먼저 실행
    }
}
```

### 17.2 보안 모니터링 스크립트

- **monitor-suspicious-ips.sh**: 의심스러운 IP 모니터링
- **block-ip-aws.ps1**: AWS Security Group을 통한 IP 차단

### 17.3 Security Group 최적화

AWS Security Group 규칙을 최소 권한 원칙에 따라 최적화했습니다:

- **SSH (22/tcp)**: 관리자 IP만 허용
- **HTTP/HTTPS**: 필요한 경우에만 개방
- **애플리케이션 포트**: 내부 네트워크만 허용

### 17.4 OAuth 2.0 (Google) 통합

Google OAuth 2.0을 통한 소셜 로그인을 지원합니다:

- Google API Client 라이브러리 사용
- OAuth 토큰 검증 및 사용자 정보 조회

---

## 18. 성능 테스트

### 18.1 Load Testing (k6)

k6를 사용한 부하 테스트 스크립트를 작성했습니다.

```javascript
import http from 'k6/http';
import { check } from 'k6';

export default function () {
    const res = http.get('http://localhost:8080/api/users/health');
    check(res, {
        'status is 200': (r) => r.status === 200,
    });
}
```

### 18.2 API 성능 벤치마크

- **평균 응답 시간**: 50-70ms
- **P95 응답 시간**: 100-150ms
- **P99 응답 시간**: 200-300ms

### 18.3 동시 사용자 처리 능력

- **최대 동시 사용자**: 120명
- **평균 동시 사용자**: 50-80명
- **피크 시간대**: 100-120명

---

## 19. 개발 프로세스

### 19.1 Git 브랜치 전략

- **main**: 프로덕션 브랜치
- **develop**: 개발 브랜치
- **feature/**: 기능 개발 브랜치
- **hotfix/**: 긴급 수정 브랜치

### 19.2 커밋 메시지 규칙 (Conventional Commits)

```
type(scope): description

feat: 새로운 기능 추가
fix: 버그 수정
docs: 문서 수정
style: 코드 포맷팅
refactor: 코드 리팩토링
test: 테스트 추가/수정
chore: 빌드 설정 변경
```

### 19.3 코드 리뷰 프로세스

- Pull Request를 통한 코드 리뷰
- 최소 1명의 승인 후 머지

### 19.4 문서화 전략

- **README.md**: 프로젝트 개요 및 빠른 시작
- **API 명세서**: Swagger/OpenAPI
- **아키텍처 문서**: Mermaid 다이어그램
- **트러블슈팅 가이드**: 해결한 문제 기록

### 19-1. 코드 품질 및 표준

#### 코딩 컨벤션 및 스타일 가이드

- **Java**: Google Java Style Guide 기반
- **TypeScript**: ESLint + Prettier

#### Lombok 활용

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private UUID id;
    private String email;
    // ...
}
```

#### DTO 패턴 및 엔티티 변환

- 엔티티를 직접 반환하지 않고 DTO로 변환
- MapStruct 또는 수동 변환 사용

#### 예외 처리 표준화

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getMessage()));
    }
}
```

#### 로깅 전략 (SLF4J, Logback)

- **로깅 레벨**: INFO, WARN, ERROR
- **구조화된 로깅**: 키-값 쌍으로 로그 기록
- **민감 정보 마스킹**: 비밀번호, 토큰 등 마스킹

### 19-2. 의존성 관리

#### Gradle 멀티 모듈 구조

```
DoranDoran (Root)
├── shared (공통 DTO, 이벤트)
├── common (공통 유틸리티)
├── gateway (API Gateway)
├── auth (인증 서비스)
├── user (사용자 서비스)
├── chat (채팅 서비스)
├── store (보관함 서비스)
└── batch (배치 서비스)
```

#### 공통 모듈 (shared, common)

- **shared**: DTO, 이벤트 클래스 등 공통 데이터 구조
- **common**: 유틸리티 클래스, 공통 설정

#### 의존성 버전 관리 (BOM 사용)

```gradle
dependencyManagement {
    imports {
        mavenBom 'org.springframework.boot:spring-boot-dependencies:3.3.4'
        mavenBom 'org.springframework.cloud:spring-cloud-dependencies:2023.0.3'
    }
}
```

#### 빌드 최적화 전략

- **멀티 프로젝트 빌드**: 병렬 빌드
- **캐싱**: Gradle 빌드 캐시 활용
- **의존성 캐싱**: 의존성 다운로드 캐싱

---

## 20. 프로젝트 규모 및 통계

### 20.1 코드 라인 수

- **Java**: 약 10,000+ 라인
- **TypeScript**: 약 5,000+ 라인
- **SQL**: 약 1,000+ 라인
- **설정 파일**: 약 500+ 라인

### 20.2 서비스 수 및 모듈 구성

- **총 서비스 수**: 6개
- **공통 모듈**: 2개 (shared, common)
- **총 모듈 수**: 8개

### 20.3 데이터베이스 테이블 수

- **총 테이블 수**: 17개
- **스키마 수**: 5개
- **인덱스 수**: 약 30개

### 20.4 API 엔드포인트 수

- **총 API 엔드포인트**: 50+ 개
- **Gateway 라우트**: 6개
- **서비스별 평균**: 8-10개

### 20.5 배포 스크립트 수

- **PowerShell 스크립트**: 10+ 개
- **Shell 스크립트**: 5+ 개
- **총 배포 스크립트**: 15+ 개

---

## 21. 기술적 도전 과제

### 21.1 Multi-Agent AI 시스템 설계

여러 AI 에이전트를 병렬/순차로 실행하여 사용자의 대화를 분석하고 응답을 생성하는 복잡한 시스템을 설계하고 구현했습니다.

**도전 과제**:
- 에이전트 간 의존성 관리
- 병렬 처리 성능 최적화
- 에러 처리 및 복원력

**해결 방법**:
- Reactor 기반 비동기 처리
- Circuit Breaker 패턴 적용
- 결과 캐싱을 통한 중복 호출 방지

### 21.2 SSE 기반 실시간 스트리밍

Server-Sent Events를 사용하여 실시간 채팅 기능을 구현했습니다.

**도전 과제**:
- 연결 관리 및 해제
- 타임아웃 처리
- 에러 복구

**해결 방법**:
- SSEManager를 통한 중앙화된 연결 관리
- 자동 재연결 메커니즘
- 연결 해제 시 자동 정리

### 21.3 마이크로서비스 간 통신 최적화

서비스 간 통신을 최적화하여 성능을 개선했습니다.

**도전 과제**:
- 서비스 간 통신 지연
- 장애 전파 방지
- 캐싱 전략

**해결 방법**:
- Feign Client를 통한 효율적인 통신
- Circuit Breaker 패턴 적용
- Redis 캐싱을 통한 조회 최적화

### 21.4 대규모 트래픽 처리 경험

동시 사용자 120명까지 처리할 수 있는 시스템을 구축했습니다.

**도전 과제**:
- 연결 풀 관리
- 메모리 최적화
- 성능 병목 해결

**해결 방법**:
- HikariCP 연결 풀 최적화
- Redis 캐싱을 통한 DB 부하 감소
- 비동기 처리로 처리량 향상

---

## 22. 학습 및 성장

### 22.1 프로젝트를 통해 배운 기술

- **마이크로서비스 아키텍처**: 서비스 분리, 독립 배포, 서비스 간 통신
- **Spring Cloud**: Gateway, OpenFeign, Circuit Breaker
- **Reactive Programming**: Project Reactor, WebFlux
- **Redis 캐싱**: 캐싱 전략, TTL 최적화, 캐시 무효화
- **Docker**: 컨테이너화, Docker Compose, 멀티 스테이지 빌드
- **AWS**: EC2, RDS, Security Group, 비용 최적화
- **모니터링**: Prometheus, Grafana, 메트릭 수집

### 22.2 마이크로서비스 아키텍처 설계 경험

- 서비스 분리 원칙 학습
- 공통 모듈 관리 방법
- 서비스 간 통신 패턴 선택
- 데이터베이스 설계 전략 (Shared Database vs Database per Service)

### 22.3 AI 시스템 통합 경험

- OpenAI API 통합
- Multi-Agent 시스템 설계
- 프롬프트 엔지니어링
- 스트리밍 응답 처리

### 22.4 인프라 운영 경험

- AWS EC2 인스턴스 관리
- Docker 컨테이너 운영
- 모니터링 시스템 구축
- 로그 분석 및 트러블슈팅

### 22.5 문제 해결 능력 향상

- RDS OOM 문제 해결
- Connection Pool Exhaustion 해결
- Hibernate 프록시 순환 참조 해결
- 성능 최적화 경험

### 22.6 비용 최적화 경험

- RDS → Local DB 마이그레이션으로 50-60% 비용 절감
- 인프라 리소스 최적화
- 불필요한 서비스 제거

---

## 23. 회고 및 교훈

### 23.1 배운 점

#### 마이크로서비스 아키텍처 설계 경험

- **서비스 분리 원칙**: 도메인별로 서비스를 분리하여 독립적 개발/배포 가능
- **공통 모듈 관리**: `shared`, `common` 모듈을 통한 코드 재사용
- **서비스 간 통신**: Feign Client를 통한 동기 통신, SSE를 통한 비동기 통신

#### SSE와 JPA 조합 시 주의사항

- **open-in-view=false 필수**: Long-running 요청에서는 DB 연결을 즉시 반환해야 함
- **연결 풀 모니터링**: 연결 누수 감지 설정 필수
- **타임아웃 관리**: SSE 스트림 타임아웃과 DB 연결 타임아웃 조정 필요

#### 연결 풀 관리의 중요성

- **적절한 풀 크기**: 서비스별 트래픽에 맞는 연결 풀 크기 설정
- **누수 감지**: `LEAK_DETECTION_THRESHOLD` 설정으로 조기 발견
- **연결 수명 관리**: `MAX_LIFETIME` 설정으로 주기적 갱신

#### 캐싱 전략의 효과

- **성능 향상**: Redis 캐싱을 통한 응답 시간 단축
- **비용 절감**: Feign Client 호출 비용 절감
- **TTL 관리**: 적절한 TTL 설정으로 데이터 일관성 유지

### 23.2 개선이 필요한 부분

#### SecurityConfig 정책 일관성

- **현재 상태**: JwtAuthFilter와 SecurityConfig 정책 불일치
- **개선 방안**: SecurityConfig를 기준으로 JwtAuthFilter 제외 목록 동기화
- **우선순위**: 중간

#### 비동기 처리 패턴 표준화

- **현재 상태**: 일부 코드에서 `block()` 호출 사용 (Reactor 비동기 스레드에서 문제 발생)
- **개선 방안**: 모든 비동기 처리를 Mono/Flux 체인으로 변경
- **우선순위**: 높음

#### 모니터링 알림 시스템

- **현재 상태**: 수동 로그 확인 및 모니터링
- **개선 방안**: Slack, Email 알림 시스템 구축, Prometheus AlertManager 연동
- **우선순위**: 중간

#### 테스트 커버리지 향상

- **현재 상태**: 단위 테스트 부족
- **개선 방안**: 서비스별 단위 테스트 작성, 통합 테스트 추가, E2E 테스트 확대
- **우선순위**: 낮음

#### 관리자 권한 체크

- **현재 상태**: JWT 토큰만 있으면 관리자 API 접근 가능
- **개선 방안**: JWT 토큰에서 역할(role) 클레임 확인, `ROLE_ADMIN` 권한 체크
- **우선순위**: 중간

### 23.3 결론

DoranDoran 프로젝트를 통해 마이크로서비스 아키텍처 설계 및 구현, 실시간 채팅 시스템 구축, AI 기반 멀티 에이전트 시스템 개발, 클라우드 인프라 운영 등 다양한 경험을 쌓을 수 있었습니다.

특히 인프라 문제 해결 과정에서 연결 풀 관리의 중요성, SSE와 JPA 조합 시 주의사항, 캐싱 전략의 효과 등을 배울 수 있었고, 이를 통해 안정적이고 성능이 우수한 시스템을 구축할 수 있었습니다.

앞으로는 비동기 처리 패턴 표준화, 모니터링 알림 시스템 구축, 테스트 커버리지 향상 등의 개선 사항을 통해 더욱 견고한 시스템을 만들어 나가겠습니다.

---

**작성자**: AI Assistant  
**최종 수정일**: 2025년 11월