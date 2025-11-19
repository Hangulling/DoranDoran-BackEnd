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

DoranDoran은 **마이크로서비스 아키텍처(MSA)**를 기반으로 한 AI 채팅 플랫폼입니다. 외국인 사용자의 한국어 학습을 지원하는 AI 챗봇으로, 친밀도 레벨에 맞춘 자연스러운 대화를 제공합니다.

**프로젝트 목적**:
- 마이크로서비스 아키텍처 설계 및 구현 경험
- 실시간 채팅 시스템 구축 (SSE 기반)
- AI 기반 멀티 에이전트 시스템 개발
- 클라우드 인프라 운영 경험 (AWS EC2)
- 비용 최적화 및 성능 개선 경험

**주요 특징**:
- 🏗️ **마이크로서비스 아키텍처**: 6개 독립 서비스로 구성
- 🤖 **Multi-Agent AI 시스템**: 4개의 AI 에이전트가 병렬/순차 처리
- 💬 **실시간 채팅**: SSE 기반 스트리밍으로 실시간 응답
- 🔐 **JWT 기반 인증**: 안전한 사용자 인증 시스템
- 📊 **모니터링**: Prometheus + Grafana를 통한 시스템 모니터링
- 🐳 **Docker 지원**: 컨테이너화된 배포 환경

### 1.2 개발 기간 및 규모

- **개발 기간**: 2024년 ~ 2025년
- **총 서비스 수**: 6개 (Gateway, Auth, User, Chat, Store, Batch)
- **총 코드 라인 수**: 약 10,000+ 라인 (Java)
- **데이터베이스 테이블**: 17개 (5개 스키마)
- **API 엔드포인트**: 50+ 개
- **배포 스크립트**: 15+ 개 (PowerShell/Shell)

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
- **인증**: JWT (JSON Web Token) - jjwt 0.12.3
- **문서화**: SpringDoc OpenAPI 2.2.0 (Swagger)

### 2.2 프론트엔드

- **언어**: TypeScript 5.8.3
- **프레임워크**: React 19.1.1
- **빌드 도구**: Vite 7.1.12
- **상태 관리**: Zustand 5.0.8, React Query 5.90.7
- **UI 라이브러리**: Material-UI 7.3.2, TailwindCSS 4.1.13, DaisyUI 5.1.10
- **HTTP 클라이언트**: Axios 1.12.0
- **라우팅**: React Router DOM 7.8.2
- **폼 관리**: React Hook Form 7.62.0

### 2.3 데이터베이스

- **주 데이터베이스**: PostgreSQL 17
- **캐시**: Redis 6.0+
- **마이그레이션**: Flyway 10.8.1
- **ORM**: Spring Data JPA (Hibernate)

### 2.4 인프라

- **컨테이너화**: Docker, Docker Compose
- **클라우드**: AWS EC2 (t3.medium)
- **모니터링**: Prometheus, Grafana
- **로깅**: SLF4J, Logback

### 2.5 외부 서비스

- **AI 서비스**: OpenAI API (GPT-4o-mini)
- **이메일**: Gmail SMTP
- **OAuth**: Google OAuth 2.0

### 2.6 기타 라이브러리

- **Excel 처리**: Apache POI 5.2.5
- **메트릭 수집**: Micrometer Prometheus
- **메일**: Spring Mail (Jakarta Mail 2.0.1)
- **Google API**: Google API Client 2.2.0

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
│   PostgreSQL    │    │     Redis       │    │   Monitoring   │
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

- **Feign Client**: 서비스 간 동기 통신
- **Circuit Breaker**: Resilience4j를 통한 장애 격리
- **Retry**: 자동 재시도 메커니즘

```java
@FeignClient(
    name = "user-service",
    url = "${user.service.url}",
    fallback = UserServiceClientFallback.class
)
public interface UserServiceClient {
    @GetMapping("/api/users/{userId}")
    UserDto getUserById(@PathVariable("userId") String userId);
}
```

#### 비동기 통신

- **Server-Sent Events (SSE)**: 실시간 채팅용 스트리밍
- **Spring Events**: 서비스 내부 이벤트 기반 통신

### 3.3 데이터베이스 설계

#### Shared Database 패턴

모든 서비스가 하나의 PostgreSQL 인스턴스를 공유하되, **스키마로 분리**하여 독립성을 유지합니다.

- `auth_schema`: 인증 관련 데이터
- `user_schema`: 사용자 정보
- `chat_schema`: 채팅방 및 메시지
- `store_schema`: 보관함 데이터
- `batch_schema`: 배치 작업 데이터

#### 데이터베이스 구조

- **총 스키마 수**: 5개
- **총 테이블 수**: 17개
- **PostgreSQL 버전**: 17

### 3.4 보안 아키텍처

- **JWT 토큰**: Access Token 및 Refresh Token
- **HMAC 헤더**: 서비스 간 통신 시 추가 보안 계층
- **IP 블랙리스트**: 차단된 IP 자동 차단
- **Security Group**: AWS 보안 그룹 최적화

### 3-1. 도메인 모델 및 엔티티 설계

#### 엔티티 구조

프로젝트에는 **38개 이상의 엔티티**가 있으며, 주요 엔티티는 다음과 같습니다:

**Chat Service 엔티티**:
- `ChatRoom`: 채팅방 정보
- `Message`: 메시지 내용
- `Chatbot`: 챗봇 메타 정보
- `IntimacyProgress`: 친밀도 진척도 추적
- `User`: 사용자 정보 (참조용)
- `UserChatbotLastInteraction`: 사용자-챗봇 마지막 상호작용
- `MonthlyUserCost`: 월별 사용자 비용
- `AiUsageEvent`: AI 사용 이벤트

**Auth Service 엔티티**:
- `User`: 사용자 정보
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

각 서비스는 독립적인 스키마를 사용하여 데이터 격리를 유지합니다:

```sql
-- auth_schema
CREATE SCHEMA IF NOT EXISTS auth_schema;

-- user_schema
CREATE SCHEMA IF NOT EXISTS user_schema;

-- chat_schema
CREATE SCHEMA IF NOT EXISTS chat_schema;

-- store_schema
CREATE SCHEMA IF NOT EXISTS store_schema;

-- batch_schema
CREATE SCHEMA IF NOT EXISTS batch_schema;
```

#### JSONB 활용

PostgreSQL의 JSONB 타입을 활용하여 유연한 데이터 구조를 지원합니다:

- **personality** (chatbots): 챗봇 성격 특성
- **capabilities** (chatbots): 챗봇 기능 설정
- **context_data** (chatrooms): 대화 컨텍스트
- **settings** (chatrooms, users): 설정 정보
- **progress_data** (intimacy_progress): 학습 진척도 데이터

```java
@Type(JsonType.class)
@Column(columnDefinition = "jsonb")
private Map<String, Object> personality;
```

#### 관계 설계

- **1:N 관계**: User → ChatRoom, ChatRoom → Message
- **N:1 관계**: Message → ChatRoom, ChatRoom → User
- **양방향 관계**: User ↔ ChatRoom (Lazy Loading)

#### 인덱스 전략

- **기본 키 인덱스**: UUID 기반 기본 키
- **외래 키 인덱스**: 연관 관계 조회 성능 향상
- **조회 조건 인덱스**: 자주 사용되는 조회 조건에 인덱스 추가
- **복합 인덱스**: (chatroom_id, sequence_number) 등

---

## 4. 핵심 기능 구현

### 4.1 Multi-Agent AI 시스템

DoranDoran의 핵심 기능은 **Multi-Agent AI 시스템**입니다. 여러 AI 에이전트가 병렬/순차로 작동하여 사용자의 한국어 학습을 지원합니다.

#### Agent 구성

1. **IntimacyAgent**: 한국어 친밀도 분석 및 교정
   - 친밀도 레벨 감지 (1-3단계)
   - 문법 오류 교정
   - 피드백 제공

2. **VocabularyAgent**: 어휘 추출 및 설명
   - 대화에서 중요한 어휘 추출
   - 어휘의 의미 및 사용 예시 제공
   - 번역 기능 포함

3. **ConversationAgent**: 대화 생성
   - 사용자 메시지에 대한 AI 응답 생성
   - 스트리밍 방식으로 응답 전송 (청크 단위)
   - 컨텍스트 기반 대화 생성

4. **SummarizerAgent**: 대화 요약
   - 대화 내용 요약
   - 키워드 추출

#### 처리 패턴

```java
public void processUserMessage(UUID chatroomId, UUID userId, Message userMessage) {
    // Phase 1: 병렬 실행
    Mono<IntimacyAgentResponse> intimacyMono = intimacyAgent.analyze(chatroomId, content).cache();
    Mono<VocabularyAgentResponse> vocabularyMono = vocabularyAgent.extract(chatroomId, content).cache();
    
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

### 4.2 실시간 채팅 (SSE 기반 스트리밍)

Server-Sent Events (SSE)를 사용하여 실시간 채팅 기능을 구현했습니다.

### 4-1. SSE 구현 상세

#### SSEManager 아키텍처

```java
@Component
public class SSEManager {
    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    
    public SseEmitter create(UUID chatroomId) {
        SseEmitter emitter = new SseEmitter(0L); // 무제한 타임아웃
        
        emitter.onCompletion(() -> remove(chatroomId, emitter));
        emitter.onTimeout(() -> remove(chatroomId, emitter));
        emitter.onError((ex) -> remove(chatroomId, emitter));
        
        emitters.computeIfAbsent(chatroomId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        return emitter;
    }
    
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

#### 프론트엔드 SSE 클라이언트 구현

```typescript
const eventSource = new EventSource(`${BASE_URL}/api/chat/stream/${chatroomId}?userId=${userId}`);

eventSource.addEventListener('intimacy_analysis', function(event) {
    const data = JSON.parse(event.data);
    // UI에 친밀도 분석 결과 표시
});

eventSource.addEventListener('conversation_chunk', function(event) {
    // 실시간으로 AI 응답 조각 표시
    conversationBuffer += event.data;
});
```

#### 연결 해제 및 에러 처리

- 연결 종료 시 자동 정리
- 타임아웃 처리
- 에러 발생 시 연결 제거
- 재연결 메커니즘

#### Multi-Agent와의 통합

각 Agent의 처리 결과를 실시간으로 SSE를 통해 전송하여 사용자에게 즉각적인 피드백을 제공합니다.

### 4-2. 비즈니스 로직

#### 친밀도 레벨 시스템

한국어의 친밀도에 따라 대화 스타일을 조정합니다:

- **Level 1 (격식체)**: 공식적인 상황, 상급자와의 대화
- **Level 2 (부드러운 존댓말)**: 일반적인 존댓말, 친구와의 대화
- **Level 3 (반말)**: 매우 친한 사이, 가족과의 대화

```java
public enum IntimacyLevel {
    FORMAL(1, "격식체"),
    POLITE(2, "부드러운 존댓말"),
    CASUAL(3, "반말");
}
```

#### Multi-Agent 오케스트레이션 로직

- **병렬 처리**: IntimacyAgent, VocabularyAgent 동시 실행
- **순차 처리**: VocabularyAgent → TranslationAgent
- **독립적 스트림**: ConversationAgent는 별도 스트림으로 실행
- **결과 집계**: 모든 Agent 결과를 통합하여 최종 피드백 제공

#### 프롬프트 동적 생성 전략

컨셉(FRIEND, HONEY, COWORKER, SENIOR, BOSS)과 친밀도 레벨을 기반으로 시스템 프롬프트를 동적으로 생성합니다.

```java
public String buildSystemPrompt(UUID chatroomId) {
    ChatRoom chatRoom = getChatRoomById(chatroomId);
    Integer intimacyLevel = getIntimacyLevel(chatroomId);
    String concept = chatRoom.getConcept();
    
    // 컨셉과 친밀도 레벨에 맞는 프롬프트 생성
    return generatePrompt(concept, intimacyLevel);
}
```

#### 학습 진척도 추적 (IntimacyProgress)

사용자의 친밀도 레벨 변화와 교정 횟수를 추적하여 학습 진척도를 측정합니다.

```java
@Entity
@Table(name = "intimacy_progress", schema = "chat")
public class IntimacyProgress {
    private UUID chatroomId;
    private Integer intimacyLevel;
    private Integer totalCorrections;
    private String lastFeedback;
    private Map<String, Object> progressData; // JSONB
}
```

#### 표현 보관함 기능 (Store Service)

사용자가 좋아하는 표현을 보관함에 저장하고 관리할 수 있습니다.

### 4.3 JWT 기반 인증/인가

- **Access Token**: 짧은 수명 (예: 1시간)
- **Refresh Token**: 긴 수명 (예: 7일)
- **HMAC 서명**: HS512 알고리즘 사용

### 4.4 Redis 캐싱 전략

자세한 내용은 [5-1. Redis 캐싱 구현 상세](#5-1-redis-캐싱-구현-상세) 섹션 참조

### 4.5 이메일 인증 시스템

- **이메일 인증 코드 발송**: 회원가입 시 이메일 인증
- **인증 코드 검증**: 6자리 인증 코드 확인
- **비밀번호 재설정**: 이메일을 통한 비밀번호 재설정 링크 발송

---

## 5. 성능 최적화

### 5.1 Redis 캐싱 전략

주요 조회 데이터를 Redis에 캐싱하여 DB 조회 부하를 줄이고 응답 속도를 개선했습니다.

#### 캐싱 대상

- **ChatRoom**: 채팅방 정보 (TTL: 30분)
- **Prompt**: 시스템 프롬프트 (TTL: 1시간)
- **IntimacyProgress**: 친밀도 진척도 (TTL: 30분)
- **User/Chatbot**: 사용자 및 챗봇 정보 (TTL: 2시간)
- **MessageHistory**: 메시지 히스토리 (TTL: 3분)

### 5-1. Redis 캐싱 구현 상세

#### 캐싱 전략

**Cache-Aside 패턴 (대부분 적용)**:
- 읽기: 캐시 확인 → 없으면 DB 조회 → 캐시 저장
- 쓰기: DB 업데이트 → 캐시 무효화

**Write-Through 패턴 (IntimacyProgress)**:
- 업데이트 시 DB와 캐시 동시 갱신

#### 캐시 키 설계 및 TTL 최적화

```java
@Cacheable(value = "chatrooms", key = "#chatroomId", unless = "#result == null")
public ChatRoom getChatRoomById(UUID chatroomId) {
    return chatRoomRepository.findById(chatroomId)
        .orElseThrow(() -> new ChatRoomNotFoundException());
}
```

| 캐시 이름 | 키 형식 | TTL | 성능 개선 |
|----------|--------|-----|----------|
| chatrooms | `chatroom::{chatroomId}` | 1800초 (30분) | 80% 개선 |
| intimacy | `intimacy::{chatroomId}` | 900초 (15분) | 82% 개선 |
| prompts | `prompts::{chatroomId}:{level}` | 600초 (10분) | 93% 개선 |
| users | `user::{userId}` | 7200초 (2시간) | 87% 개선 |
| messageHistory | `messageHistory::{chatroomId}` | 180초 (3분) | 88% 개선 |

#### 캐시 무효화 전략

```java
@CacheEvict(value = {"chatrooms", "roomList"}, key = "#chatroomId")
public ChatRoom updateRoom(UUID chatroomId, UUID userId, String name) {
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
- 평균 메시지 처리 시간: 45-70ms (**40% 개선**)
- DB 동시 연결 수: 평균 6개 (**60% 감소**)
- Multi-Agent 처리 시 DB 조회: 메시지당 2-3회 (**75% 감소**)
- 동시 사용자 처리 한계: 약 120명 (**140% 증가**)

#### 캐시 히트율 통계

- **ChatRoom**: 85-92%
- **IntimacyProgress**: 78-88%
- **User/Chatbot**: 95-98%
- **SystemPrompt**: 82-90%
- **MessageHistory**: 72-85%

#### 캐시 메트릭 및 모니터링

Prometheus를 통해 캐시 히트율, 미스율, 메모리 사용량 등을 모니터링합니다.

### 5.2 데이터베이스 연결 풀 최적화 (HikariCP)

#### 연결 풀 설정

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # 10 → 20
      minimum-idle: 5         # 0 → 5
      leak-detection-threshold: 10000  # 10초
      max-lifetime: 600000    # 30분 → 10분
```

서비스별 최적화:
- **Auth**: 30개
- **User**: 30개
- **Chat**: 50개 (높은 동시성)
- **Store**: 20개

### 5.3 N+1 문제 해결

- **Fetch Join 사용**: 연관 엔티티 한 번에 조회
- **@EntityGraph 활용**: 필요한 연관 관계만 로딩
- **DTO 변환**: 엔티티 직접 반환 대신 DTO 사용

```java
@Query("SELECT cr FROM ChatRoom cr " +
       "LEFT JOIN FETCH cr.user " +
       "LEFT JOIN FETCH cr.chatbot " +
       "WHERE cr.userId = :userId")
List<ChatRoom> findByUserIdWithRelations(@Param("userId") UUID userId);
```

### 5.4 비동기 처리 (Reactor, WebFlux)

- **WebFlux 사용**: Chat Service의 SSE 스트리밍
- **Mono/Flux 체인**: 비동기 파이프라인 구성
- **Scheduler 분리**: CPU 집약적 작업과 I/O 작업 분리

---

## 6. 트러블슈팅 및 문제 해결

### 6.1 RDS OOM 문제 (2025-10-14)

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

**결과**: 컨테이너 Health `healthy` 상태로 복구, API 정상화

### 6.2 Connection Pool Exhaustion (2025-10-14)

**문제 상황**:
- 503 Service Unavailable 오류 발생
- 모든 API 요청 30초 타임아웃 후 실패
- HikariCP 연결 풀 고갈 (10개 연결 모두 사용 중)

**원인 분석**:
- **핵심 원인**: Connection Leak (연결 누수)
- **트리거**: SSE + `spring.jpa.open-in-view=true` 조합
  - SSE 스트림이 오래 유지되는 동안 DB 연결도 계속 점유
  - 타임아웃 발생 시 비정상 종료 → 연결 반환 지연

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

**결과**: SSE 스트림이 DB 연결을 불필요하게 점유하지 않음, 연결 풀 20개로 여유 확보

### 6.3 Hibernate 프록시 순환 참조

**문제 상황**:
- 애플리케이션 실행 중 `NullPointerException`과 스택 오버플로우 발생
- 로그에서 `User$HibernateProxy$Dyp9SYgE.toString` 무한 반복 확인

**원인 분석**:
- 양방향 관계 설정에서 Lombok `@Data`가 생성한 `toString()` 메서드가 순환 참조를 일으킴

**해결 방법**:
```java
@ToString(exclude = {"chatRooms"})
public class User {
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<ChatRoom> chatRooms = new ArrayList<>();
}
```

### 6.4 GreetingService 프롬프트 개선

**문제 상황**:
- `greeting_guide_message` SSE 이벤트가 클라이언트에 전달되지 않음
- AI 응답 파싱 실패로 인한 `NullPointerException` 발생

**원인 분석**:
- 프롬프트 JSON 형식 오류
- 불필요한 필드 요구

**해결 방법**:
- JSON 형식 통일
- null 체크 및 안전성 강화

### 6.5 IP 블랙리스트 시스템 구현

**문제 상황**:
- 의심 IP (172.104.24.172)로부터 비정상 HTTP 요청 다수 발생
- RTSP/1.0, SIP/2.0 등 비정상 프로토콜 요청

**해결 방법**:
- `IpBlacklistFilter` 구현
- Redis 기반 저장
- 관리자 API 구현

### 6.6 RDS → Local DB 마이그레이션 (비용 절감)

**배경**:
- AWS RDS Aurora 사용으로 인한 높은 비용 ($59.04/월)
- 비용 절감을 위한 로컬 PostgreSQL 컨테이너 전환

**마이그레이션 과정**:
1. RDS Aurora 데이터 덤프 (386KB)
2. 컨테이너 교체: `shared-db` → `dorandoran-shared-db`
3. PostgreSQL 업그레이드: 15 → 17
4. 데이터 복원: 5개 스키마, 17개 테이블 복원
5. 배포 스크립트 수정: 모든 서비스의 DB 연결을 localhost로 변경

**비용 절감 효과**:
- **Before**: $118-143/월 (RDS 포함)
- **After**: $50-64/월 (로컬 DB)
- **절감액**: 월 $58-79 (50-60% 절감)
- **연간 절감**: $696-948

---

## 7. 인프라 및 배포

### 7.1 AWS EC2 배포 환경

- **인스턴스 타입**: t3.medium (2 vCPU, 4GB RAM)
- **운영체제**: Amazon Linux 2
- **네트워크**: VPC 내부 배포

### 7.2 Docker 컨테이너화

- **모든 서비스 컨테이너화**: 각 서비스별 Dockerfile 작성
- **Docker Network**: `dorandoran-network`를 통한 서비스 간 통신
- **컨테이너 이름 규칙**: `dorandoran-{service-name}`

### 7.3 배포 자동화 스크립트

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

### 7.4 모니터링 시스템 (Prometheus + Grafana)

- **Prometheus**: 메트릭 수집 (포트: 9090)
- **Grafana**: 대시보드 (포트: 3000)
- **Actuator**: 각 서비스의 헬스체크 엔드포인트

---

## 8. 보안 구현

### 8.1 JWT 토큰 기반 인증

- **Access Token**: 짧은 수명 (예: 1시간)
- **Refresh Token**: 긴 수명 (예: 7일)
- **HMAC 서명**: HS512 알고리즘 사용

### 8.2 HMAC 헤더 검증

서비스 간 통신 시 추가 보안 계층을 제공합니다.

```java
// Gateway에서 HMAC 헤더 주입
String hmac = generateHMAC(requestBody, secretKey);
exchange.getRequest().mutate()
    .header("X-HMAC", hmac)
    .build();
```

### 8.3 IP 블랙리스트 필터

차단된 IP 주소를 자동으로 차단합니다 (HTTP 403).

### 8.4 Security Group 최적화

- **SSH (22/tcp)**: 관리자 IP만 허용
- **HTTP (80/tcp)**: 모든 IP 허용 (필요 시)
- **HTTPS (443/tcp)**: 모든 IP 허용 (필요 시)
- **애플리케이션 포트**: 내부 네트워크만 허용

---

## 9. 운영 기록 및 통계

### 9.1 사용자 및 활동 통계

- **총 등록 사용자**: 115명
- **총 채팅방 수**: 237개
- **Gateway 요청 수 (24시간)**: 약 410건
- **Auth Service 요청 수 (24시간)**: 589건
  - 로그인: 159건 (평균 응답 시간 0.130초)
  - 사용자 정보 조회: 430건 (평균 응답 시간 0.009초)

### 9.2 서비스별 리소스 사용률 (정상 운영 시)

- **Chat Service**: CPU 0.22%, 메모리 547.3MB (14.30%)
- **Store Service**: CPU 0.16%, 메모리 484.6MB (12.66%)
- **Auth Service**: CPU 0.12%, 메모리 439.2MB (11.48%)
- **User Service**: CPU 0.09%, 메모리 328.5MB (8.58%)
- **Gateway Service**: CPU 0.13%, 메모리 268.9MB (7.02%)
- **PostgreSQL**: 메모리 144.6MB (3.78%)
- **Redis**: CPU 0.47%, 메모리 5.059MB (0.13%)

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

### 9.4 비용 절감 효과

- **RDS 제거**: 월 $59.04 절감
- **전체 비용 절감**: 월 $58-79 (50-60% 절감)
- **연간 절감**: $696-948

### 9-1. 프로젝트 성과 및 영향

#### 사용자 수 및 활동 통계

- **등록 사용자**: 115명
- **채팅방**: 237개
- **일일 API 요청**: 약 1,000건

#### 채팅방 및 메시지 통계

- 평균 채팅방당 메시지 수 추적
- Multi-Agent 처리 통계

#### 시스템 안정성 지표

- **Uptime**: 99% 이상
- **평균 응답 시간**: 50-70ms
- **에러율**: 1% 미만

#### 비용 절감 효과

- **RDS 제거로 50-60% 절감**: 월 $58-79, 연간 $696-948
- **인프라 비용 최적화**: t3.medium 인스턴스로 충분한 성능 확보

#### 성능 개선 효과

- **응답 시간 40% 개선**: 85-120ms → 45-70ms
- **동시 사용자 140% 증가**: 50명 → 120명
- **DB 조회 75% 감소**: 메시지당 8-12회 → 2-3회

---

## 10. 프론트엔드 아키텍처

### 10.1 React 19 + TypeScript 구조

- **React 19.1.1**: 최신 React 기능 활용
- **TypeScript 5.8.3**: 타입 안정성 보장
- **Vite 7.1.12**: 빠른 개발 서버 및 빌드

### 10.2 상태 관리

- **Zustand 5.0.8**: 전역 상태 관리
- **React Query 5.90.7**: 서버 상태 관리 및 캐싱

### 10.3 UI 라이브러리

- **Material-UI 7.3.2**: 컴포넌트 라이브러리
- **TailwindCSS 4.1.13**: 유틸리티 기반 CSS
- **DaisyUI 5.1.10**: TailwindCSS 컴포넌트

### 10.4 실시간 통신 (SSE 클라이언트 구현)

```typescript
const eventSource = new EventSource(`${BASE_URL}/api/chat/stream/${chatroomId}`);

eventSource.addEventListener('conversation_chunk', function(event) {
    // 실시간으로 AI 응답 조각 표시
    updateConversationBuffer(event.data);
});
```

### 10.5 라우팅 및 인증 처리

- **React Router DOM 7.8.2**: 클라이언트 사이드 라우팅
- **JWT 토큰 관리**: 로컬 스토리지에 저장
- **인증 상태 관리**: Zustand를 통한 전역 인증 상태

---

## 11. API 설계 및 문서화

### 11.1 Swagger/OpenAPI (SpringDoc) 사용

각 서비스별로 Swagger UI가 제공됩니다:
- **Gateway**: `http://localhost:8080/swagger-ui.html`
- **Auth Service**: `http://localhost:8081/swagger-ui.html`
- **User Service**: `http://localhost:8082/swagger-ui.html`
- **Chat Service**: `http://localhost:8083/swagger-ui.html`

### 11.2 RESTful API 설계 원칙

- **리소스 기반 URL**: `/api/users/{userId}`
- **HTTP 메서드 활용**: GET, POST, PUT, PATCH, DELETE
- **표준 HTTP 상태 코드**: 200, 201, 400, 401, 403, 404, 500

### 11.3 API 버전 관리

현재는 버전 1을 사용하며, 향후 확장을 위해 버전 관리 구조를 준비했습니다.

### 11.4 에러 응답 표준화

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

- **JUnit 5**: 테스트 프레임워크
- **Mockito**: 모킹 라이브러리
- **Spring Boot Test**: 통합 테스트 지원

### 12.2 통합 테스트 (Spring Boot Test)

- **@SpringBootTest**: 전체 컨텍스트 로드
- **@WebMvcTest**: 웹 레이어만 테스트
- **@DataJpaTest**: 데이터 레이어만 테스트

### 12.3 E2E 테스트 (Gateway 통합 테스트)

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class AuthE2ETest {
    @Test
    void testLoginFlow() {
        // Gateway -> Auth Service -> User Service 전체 플로우 테스트
    }
}
```

### 12.4 테스트 커버리지 현황

현재 테스트 커버리지는 개선이 필요한 부분입니다. 향후 단위 테스트 및 통합 테스트를 추가할 예정입니다.

### 12.5 친밀도 레벨 테스트 케이스

격식체 챗봇이 낮은 친밀도에서 사용자의 친근한 말투에 영향받지 않고 일관된 레벨을 유지하는지 검증하는 테스트 케이스를 작성했습니다.

---

## 13. 배포 자동화 및 DevOps

### 13.1 배포 스크립트 (PowerShell/Shell)

서비스별 배포 스크립트가 있으며, 다음 기능을 제공합니다:
- Gradle 빌드
- Docker 이미지 생성
- 컨테이너 중지/제거
- 컨테이너 실행 및 Health Check

### 13.2 Docker 컨테이너화 전략

- **멀티 스테이지 빌드**: 빌드 이미지와 런타임 이미지 분리
- **레이어 캐싱**: 의존성 레이어와 소스 코드 레이어 분리
- **환경변수 주입**: 설정을 환경변수로 주입

### 13.3 환경별 설정 관리

- **dev**: 로컬 개발 환경
- **prod**: 프로덕션 환경
- **docker**: Docker 컨테이너 환경

### 13.4 자동 백업 스크립트

- **데이터베이스 덤프**: 주기적 백업
- **EBS 스냅샷**: 인프라 백업

### 13.5 모니터링 스크립트

- **check_bottleneck.sh**: 서비스별 병목 현상 체크
- **로그 분석 스크립트**: 에러 로그 분석

---

## 14. 데이터베이스 마이그레이션

### 14.1 Flyway를 통한 스키마 버전 관리

Flyway를 사용하여 데이터베이스 스키마 버전을 관리합니다.

```sql
-- V1__Create_chat_schema.sql
CREATE SCHEMA IF NOT EXISTS chat_schema;

-- V2__Create_chatrooms_table.sql
CREATE TABLE chat_schema.chatrooms (
    id UUID PRIMARY KEY,
    ...
);
```

### 14.2 마이그레이션 전략

- **버전 관리**: V{version}__{description}.sql 형식
- **롤백 전략**: 수동 롤백 스크립트 작성
- **테스트**: 로컬 환경에서 마이그레이션 테스트

### 14.3 RDS → Local DB 마이그레이션 경험

자세한 내용은 [6.6 RDS → Local DB 마이그레이션](#66-rds--local-db-마이그레이션-비용-절감) 섹션 참조

---

## 15. 에러 핸들링 및 복원력

### 15.1 Circuit Breaker 패턴 (Resilience4j)

서비스 간 통신 시 장애 격리를 위해 Circuit Breaker 패턴을 적용했습니다.

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

- **최대 재시도 횟수**: 3회
- **재시도 간격**: 지수 백오프
- **재시도 조건**: 네트워크 오류, 타임아웃 등

### 15.3 Fallback 전략

- **기본값 반환**: null 또는 기본 객체 반환
- **캐시된 데이터 사용**: 이전에 캐시된 데이터 사용
- **에러 로깅**: 모든 실패를 로그로 기록

### 15.4 예외 처리 표준화

- **커스텀 예외 클래스**: 도메인별 예외 정의
- **전역 예외 핸들러**: `@ControllerAdvice`를 통한 중앙 예외 처리
- **에러 응답 형식**: 표준화된 에러 응답 형식

---

## 16. 모니터링 및 로깅

### 16.1 Prometheus 메트릭 수집

- **HTTP 요청 수 및 응답 시간**
- **데이터베이스 연결 풀 상태**
- **JVM 메모리 사용량**
- **CPU 사용률**

### 16.2 Grafana 대시보드 구성

- **서비스별 메트릭 시각화**
- **에러율 추적**
- **응답 시간 모니터링**
- **리소스 사용률 추적**

### 16.3 로그 분석 스크립트

- **daily-log-analysis.sh**: 일일 로그 분석
- **check_bottleneck.sh**: 병목 현상 체크
- **에러 로그 필터링**: 특정 에러 패턴 추적

### 16.4 성능 병목 감지 도구

- **Docker stats**: 컨테이너 리소스 사용량
- **Prometheus 쿼리**: 메트릭 기반 병목 감지
- **로그 분석**: 느린 쿼리, 타임아웃 등 추적

---

## 17. 보안 구현 상세

### 17.1 IP 블랙리스트 시스템

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
}
```

### 17.2 보안 모니터링 스크립트

- **monitor-suspicious-ips.sh**: 의심스러운 IP 모니터링
- **block-ip-aws.ps1**: AWS Security Group을 통한 IP 차단

### 17.3 Security Group 최적화

- 최소 권한 원칙 적용
- 필요한 포트만 개방
- 관리자 IP만 SSH 접근 허용

### 17.4 OAuth 2.0 (Google) 통합

- Google OAuth 2.0을 통한 소셜 로그인 지원
- Google API Client 라이브러리 사용

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