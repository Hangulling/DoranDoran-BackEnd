# DoranDoran MSA 아키텍처 다이어그램

## 전체 시스템 아키텍처

```mermaid
graph TB
    %% External Layer
    subgraph "External Layer"
        Client[웹 클라이언트]
        Mobile[모바일 앱]
    end

    %% API Gateway Layer
    subgraph "API Gateway Layer"
        Gateway[API Gateway<br/>Spring Cloud Gateway<br/>Port: 8080]
    end

    %% Service Layer
    subgraph "Microservices Layer"
        Auth[Auth Service<br/>Spring Boot<br/>Port: 8081]
        User[User Service<br/>Spring Boot<br/>Port: 8082]
        Chat[Chat Service<br/>Spring Boot<br/>Port: 8083]
        Batch[Batch Service<br/>Spring Boot<br/>Port: 8085]
    end

    %% Data Layer
    subgraph "Data Layer"
        DB[(PostgreSQL<br/>Port: 5432<br/>Shared Database)]
        Redis[(Redis<br/>Port: 6379<br/>Cache & Session)]
    end

    %% External Services
    subgraph "External Services"
        OpenAI[OpenAI API<br/>GPT Models]
        Prometheus[Prometheus<br/>Port: 9090]
        Grafana[Grafana<br/>Port: 3000]
    end

    %% Connections
    Client --> Gateway
    Mobile --> Gateway
    
    Gateway --> Auth
    Gateway --> User
    Gateway --> Chat
    Gateway --> Batch

    Auth --> DB
    Auth --> Redis
    User --> DB
    User --> Redis
    Chat --> DB
    Chat --> Redis
    Batch --> DB

    Chat --> OpenAI

    Auth --> Prometheus
    User --> Prometheus
    Chat --> Prometheus
    Batch --> Prometheus
    Prometheus --> Grafana

    %% Service Communication
    Auth -.->|Feign Client| User
    Chat -.->|Feign Client| User
    Chat -.->|Feign Client| Auth
```

## 서비스별 상세 아키텍처

### 1. API Gateway (Spring Cloud Gateway)
```mermaid
graph LR
    subgraph "API Gateway"
        Gateway[Gateway Service]
        RateLimit[Rate Limiter<br/>Redis 기반]
        CORS[CORS Handler]
        Router[Route Handler]
    end

    Client[Client] --> Gateway
    Gateway --> RateLimit
    RateLimit --> CORS
    CORS --> Router
    Router --> Auth[Auth Service]
    Router --> User[User Service]
    Router --> Chat[Chat Service]
    Router --> Batch[Batch Service]
```

### 2. Auth Service
```mermaid
graph TB
    subgraph "Auth Service"
        AuthController[Auth Controller]
        AuthService[Auth Service]
        JWTService[JWT Service]
        UserIntegration[User Integration<br/>Feign Client]
        CircuitBreaker[Circuit Breaker<br/>Resilience4j]
    end

    subgraph "Auth Database"
        AuthTables[(auth_schema<br/>tables)]
    end

    AuthController --> AuthService
    AuthService --> JWTService
    AuthService --> UserIntegration
    UserIntegration --> CircuitBreaker
    AuthService --> AuthTables
```

### 3. User Service
```mermaid
graph TB
    subgraph "User Service"
        UserController[User Controller]
        UserService[User Service]
        UserRepository[User Repository]
        EventPublisher[Event Publisher<br/>Spring Events]
    end

    subgraph "User Database"
        UserTables[(user_schema<br/>tables)]
    end

    UserController --> UserService
    UserService --> UserRepository
    UserService --> EventPublisher
    UserRepository --> UserTables
```

### 4. Chat Service (Multi-Agent AI)
```mermaid
graph TB
    subgraph "Chat Service"
        ChatController[Chat Controller]
        ChatService[Chat Service]
        MultiAgent[Multi-Agent<br/>Orchestrator]
        
        subgraph "AI Agents"
            IntimacyAgent[Intimacy Agent<br/>친밀도 분석]
            VocabularyAgent[Vocabulary Agent<br/>어휘 추출]
            TranslationAgent[Translation Agent<br/>번역]
            ConversationAgent[Conversation Agent<br/>대화 생성]
        end
        
        subgraph "Real-time Communication"
            WebSocket[WebSocket Handler]
            SSE[SSE Manager]
        end
        
        OpenAIClient[OpenAI Client]
        UserIntegration[User Integration<br/>Feign Client]
    end

    subgraph "Chat Database"
        ChatTables[(chat_schema<br/>tables)]
    end

    subgraph "External AI"
        OpenAI[OpenAI API]
    end

    ChatController --> ChatService
    ChatService --> MultiAgent
    MultiAgent --> IntimacyAgent
    MultiAgent --> VocabularyAgent
    MultiAgent --> TranslationAgent
    MultiAgent --> ConversationAgent
    
    ChatController --> WebSocket
    ChatController --> SSE
    
    IntimacyAgent --> OpenAIClient
    VocabularyAgent --> OpenAIClient
    TranslationAgent --> OpenAIClient
    ConversationAgent --> OpenAIClient
    OpenAIClient --> OpenAI
    
    ChatService --> UserIntegration
    ChatService --> ChatTables
```

## 서비스 간 통신 패턴

### 1. 동기 통신 (HTTP/REST)
```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant Auth
    participant User
    participant Chat

    Client->>Gateway: POST /api/chat/messages
    Gateway->>Auth: JWT 검증
    Auth->>User: 사용자 정보 조회 (Feign)
    User-->>Auth: UserDto 반환
    Auth-->>Gateway: 인증 성공
    Gateway->>Chat: 메시지 전송
    Chat->>User: 사용자 정보 조회 (Feign)
    User-->>Chat: UserDto 반환
    Chat-->>Gateway: 메시지 저장 완료
    Gateway-->>Client: 응답
```

### 2. 이벤트 기반 통신
```mermaid
sequenceDiagram
    participant User
    participant EventBus
    participant Auth
    participant Chat

    User->>User: 사용자 상태 변경
    User->>EventBus: UserStatusChangedEvent 발행
    EventBus->>Auth: 이벤트 수신
    EventBus->>Chat: 이벤트 수신
    Auth->>Auth: 인증 정책 적용
    Chat->>Chat: 채팅방 접근 제한
```

### 3. 실시간 통신 (WebSocket + SSE)
```mermaid
sequenceDiagram
    participant Client
    participant Chat
    participant MultiAgent
    participant OpenAI

    Client->>Chat: WebSocket 연결
    Client->>Chat: 메시지 전송
    Chat->>MultiAgent: Multi-Agent 처리 시작
    MultiAgent->>OpenAI: 병렬 AI 호출
    OpenAI-->>MultiAgent: 스트림 응답
    MultiAgent->>Chat: SSE 이벤트 전송
    Chat->>Client: 실시간 응답 스트림
```

## 데이터 흐름

### 1. 사용자 등록 플로우
```mermaid
flowchart TD
    A[사용자 등록 요청] --> B[API Gateway]
    B --> C[User Service]
    C --> D[사용자 생성]
    D --> E[UserCreatedEvent 발행]
    E --> F[Auth Service 이벤트 수신]
    F --> G[인증 정보 초기화]
```

### 2. 채팅 메시지 처리 플로우
```mermaid
flowchart TD
    A[메시지 전송] --> B[API Gateway]
    B --> C[JWT 검증]
    C --> D[Chat Service]
    D --> E[Multi-Agent 처리]
    E --> F[IntimacyAgent]
    E --> G[VocabularyAgent]
    E --> H[ConversationAgent]
    F --> I[SSE 스트림]
    G --> J[TranslationAgent]
    H --> I
    J --> I
    I --> K[클라이언트 응답]
```

## 인프라 구성

### Docker Compose 서비스
```mermaid
graph TB
    subgraph "Docker Compose"
        subgraph "Database Layer"
            PostgreSQL[(PostgreSQL<br/>shared-db)]
            Redis[(Redis)]
        end
        
        subgraph "Application Layer"
            AuthContainer[Auth Service<br/>Container]
            UserContainer[User Service<br/>Container]
            ChatContainer[Chat Service<br/>Container]
            BatchContainer[Batch Service<br/>Container]
            GatewayContainer[API Gateway<br/>Container]
        end
        
        subgraph "Monitoring Layer"
            PrometheusContainer[Prometheus<br/>Container]
            GrafanaContainer[Grafana<br/>Container]
        end
    end

    AuthContainer --> PostgreSQL
    UserContainer --> PostgreSQL
    ChatContainer --> PostgreSQL
    BatchContainer --> PostgreSQL
    
    AuthContainer --> Redis
    UserContainer --> Redis
    ChatContainer --> Redis
    GatewayContainer --> Redis
    
    GatewayContainer --> AuthContainer
    GatewayContainer --> UserContainer
    GatewayContainer --> ChatContainer
    GatewayContainer --> BatchContainer
    
    PrometheusContainer --> AuthContainer
    PrometheusContainer --> UserContainer
    PrometheusContainer --> ChatContainer
    PrometheusContainer --> BatchContainer
    
    GrafanaContainer --> PrometheusContainer
```

## 보안 및 모니터링

### 1. 보안 계층
- **JWT 인증**: 모든 API 요청에 JWT 토큰 검증
- **Rate Limiting**: Redis 기반 요청 속도 제한
- **CORS**: 크로스 오리진 요청 제어
- **Circuit Breaker**: 서비스 장애 격리

### 2. 모니터링
- **Prometheus**: 메트릭 수집
- **Grafana**: 대시보드 및 알림
- **Actuator**: 각 서비스 헬스체크
- **Logging**: 구조화된 로그 수집
